package com.ohun.stomp.netty;

import com.ohun.stomp.api.StompException;
import com.ohun.stomp.protocol.Command;
import com.ohun.stomp.protocol.Frame;
import com.ohun.stomp.protocol.Heads;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ohun.stomp.protocol.Constants.*;

/**
 * Created by xiaoxu.yxx on 2014/7/24.
 */
public final class StompFrameDecoder extends ByteToMessageDecoder {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decodeHeartbeat(in);//处理心跳字节
        decodeFrames(in, out);//处理frame
    }

    private void decodeHeartbeat(ByteBuf buffer) {
        while (buffer.isReadable()) {
            if (buffer.readByte() != LF_10) {
                buffer.readerIndex(buffer.readerIndex() - 1);
                break;
            }
        }
    }

    private void decodeFrames(ByteBuf buffer, List<Object> out) throws Exception {
        try {
            while (buffer.isReadable()) {
                //1.记录当前读取位置位置.如果读取到非完整的frame,要恢复到该位置,便于下次读取
                buffer.markReaderIndex();
                out.add(decodeFrame(buffer));
            }
        } catch (StompException e) {
            //2.读取到不完整的frame,恢复到最近一次正常读取的位置,便于下次读取
            buffer.resetReaderIndex();
        }
    }

    private Frame decodeFrame(ByteBuf buffer) throws Exception {
        //1.读取命令
        final Command command = decodeCommand(buffer);
        //2.读取head部分
        final Map<String, String> header = decodeHeader(buffer);
        //3.读取body部分
        final byte[] body = decodeBody(buffer, header);
        //4.组装成一完整的帧
        return new Frame(command, header, body);
    }

    private Command decodeCommand(ByteBuf buffer) {
        String line = readLine(buffer, MAX_CMD_LEN);
        if (line == null || line.isEmpty()) {
            throw new StompException("invalid command");
        }
        try {
            return Command.valueOf(line);
        } catch (Exception e) {
            logger.error("decode stomp command error,line=" + line);
        }

        throw new StompException("invalid command");
    }

    private Map<String, String> decodeHeader(ByteBuf buffer) throws IOException {
        Map<String, String> header = new HashMap<String, String>();
        while (buffer.isReadable()) {
            String line = readLine(buffer, MAX_HEAD_LEN);
            if (line == null) throw new StompException("invalid header");
            if (line.isEmpty()) break;
            processHeaderLine(header, line);
        }
        return header;
    }

    private void processHeaderLine(Map<String, String> headers, String line) {
        int index = line.indexOf(COLON);
        if (index > 0) {
            String name = line.substring(0, index);
            String value = line.substring(index + 1);
            if (headers.containsKey(name)) return;
            headers.put(name, value);
        }
    }

    private byte[] decodeBody(ByteBuf buffer, Map<String, String> header) {
        //1.从head里取出body的长度
        int contentLength = getContentLength(header);
        if (contentLength > 0) {
            //2.根据内容长度读取body
            return read_binary_body(buffer, contentLength);
        } else {
            //3.根据frame结束字符'\u0000'读取body
            return read_text_body(buffer);
        }
    }

    private int getContentLength(Map<String, String> header) {
        String length = header.get(Heads.CONTENT_LENGTH);
        if (length == null || length.isEmpty()) return 0;
        try {
            return Integer.valueOf(length.trim());
        } catch (Exception e) {
        }
        return -1;
    }

    private byte[] read_text_body(ByteBuf buffer) {
        //1.取出结束符'\u0000'之前的内容的长度
        int contentLength = buffer.bytesBefore(NULL_0);
        if (contentLength == -1) {//没找到结束符body不完整
            throw new StompException("invalid text body");
        }
        //2.没有内容,是正常情况
        if (contentLength == 0) {
            //读取结束符'\u0000',正常返回
            buffer.readByte();
            return EMPTY_BYTES;
        }
        //3.读取完整的body部分的字节
        byte[] body = new byte[contentLength];
        buffer.readBytes(body);
        buffer.skipBytes(1);//跳过最后的结束符
        return body;
    }

    private byte[] read_binary_body(ByteBuf buffer, int content_length) {
        //1.如果可读字节数小于内容长度+1,说明body不完整
        int N = buffer.readableBytes();//可读字节总数
        int L = content_length + 1;//+1代表要加上结一个束符'\u0000'
        if (N < L) {
            throw new StompException("invalid binary body");
        }
        //2.判断最后一个字符是不是frame结束符\u0000'
        byte NUL = buffer.getByte(buffer.readerIndex() + content_length);
        if (NUL != NULL_0) {//没找到结束符,说明body不完整
            throw new StompException("invalid binary body");
        }
        //3.读取完整的body部分的字节
        byte[] body = new byte[content_length];
        buffer.readBytes(body);
        buffer.skipBytes(1);//跳过最后的结束符
        return body;
    }


    private String readLine(ByteBuf buffer, int maxLineLength) {
        if (!buffer.isReadable()) return null;
        //1.判断最大可读的字节长度
        int L = Math.min(maxLineLength, buffer.readableBytes());
        //2.根据换行符('\n')读取一行数据
        StringBuilder sb = new StringBuilder(Math.min(L, 64));
        for (int i = 0; i < L; i++) {
            byte nextByte = buffer.readByte();
            if (nextByte == LF_10) {//2.1遇到换行符('\n')表示读到完整一行
                return sb.length() == 0 ? EMPTY_LINE : sb.toString();
            } else if (nextByte == CR_13) {//2.2遇到回车符('\r')要看下个字符是否是换行符('\n')
                nextByte = buffer.readByte();
                if (nextByte == LF_10) {//2.3遇到换行符('\n')表示读到完整一行
                    return sb.length() == 0 ? EMPTY_LINE : sb.toString();
                }
                i++;//2.4多读了一次,所以要多计数一次
            } else {
                sb.append((char) nextByte);
            }
        }
        return null;
    }
}
