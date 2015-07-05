package com.ohun.stomp.netty;

import com.ohun.stomp.api.StompException;
import com.ohun.stomp.protocol.Command;
import com.ohun.stomp.protocol.Frame;
import com.ohun.stomp.protocol.Heads;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ohun.stomp.protocol.Constants.*;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;

/**
 * Created by xiaoxu.yxx on 2014/11/06.
 */
public final class StompFrameDecoder2 extends ByteToMessageDecoder {

    private Reader last = start();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (decodeFrame(in, out)) ;
    }


    private boolean decodeFrame(ByteBuf in, List<Object> out) {
        while (in.isReadable()) {
            Result<?> result = last.read(in);
            switch (result.state) {
                case BROKEN:
                    last = (Reader) result.value;
                    return false;
                case DONE:
                    out.add(result.value);
                    last = start();
                    return true;
                case NEXT:
                    last = (Reader) result.value;
            }
        }
        return false;
    }

    private Reader start() {
        return new CommandReader(0);
    }


    private class CommandReader extends LineReader {

        CommandReader(int offset) {
            super(offset);
        }

        @Override
        protected Reader line(ByteBuf line, int offset) {
            return line.isReadable() ?
                    //读取到非空行,说明读取到命令部分,接着要读取header部分
                    new HeaderReader(offset, toCmd(line)) :
                    //读取到空行,说明是心跳字节,忽略心跳字节,继续读取命令部分
                    this.offset(offset);
        }

        private Command toCmd(ByteBuf command) {
            return Command.valueOf(fastToString(command));
        }
    }

    private class HeaderReader extends LineReader {

        HeaderReader(int offset, Command command) {
            super(offset);
            this.command = command;
            this.headers = new HashMap<String, String>();
        }

        @Override
        protected Reader line(ByteBuf line, int offset) {
            return line.isReadable() ?
                    //读取到非空行,说明还有下一行,接着读取下一行,直到读完所有header
                    this.append(line).offset(offset) :
                    //读取到空行,说明header已经读完,接着读取body部分
                    new ContentReader(offset, command, headers);
        }

        private HeaderReader append(ByteBuf header) {
            String line = fastToString(header);
            int index = line.indexOf(COLON);
            if (index > 0) {
                String name = line.substring(0, index);
                String value = line.substring(index + 1);
                if (headers.containsKey(name)) return this;
                headers.put(name, value);
            }
            return this;
        }

        final Command command;
        final Map<String, String> headers;
    }


    private class ContentReader extends AbstractReader {

        ContentReader(int offset, Command command, Map<String, String> headers) {
            super(NULL_0, offset);
            this.offset = offset;
            this.command = command;
            this.headers = headers;
        }

        @Override
        public Result<?> read(ByteBuf in) {
            int L = contentLength(headers);//没带content-length以结束符为准
            return L == -1 ? readContentUntilNull(in) : readFixedContent(L, in);
        }

        @Override
        protected Done next(ByteBuf in, int length) {
            if (length == 0) return new Done(newFrame(EMPTY_BUFFER));//length == 0表示body部分为空

            ByteBuf content = in.slice(in.readerIndex() + offset, length);
            in.skipBytes(offset + length + 1); //skip NULL

            return new Done(newFrame(content));
        }

        private Result<?> readContentUntilNull(ByteBuf in) {
            return super.read(in);
        }

        private Result<?> readFixedContent(int length, ByteBuf in) {
            int start = in.readerIndex() + offset;
            int end = in.writerIndex();

            //如果可读取长度小于body部分,说明body不完整
            if (end - start < length + 1) {
                return new Broken(this);
            }

            //如果body的最后一个字符不是NULL,说明content-length或body不合法
            if (in.getByte(start + length) != NULL_0) {
                throw new StompException("Non-NULL end of frame."
                        + in.readSlice(offset + length + 1).toString(UTF_8));
            }

            ByteBuf content = in.slice(start, length);
            in.skipBytes(offset + length + 1);//skip NULL

            return new Done(newFrame(content));
        }

        private Frame newFrame(ByteBuf content) {
            return new Frame(command, headers, toBytes(content));
        }

        final int offset;
        final Command command;
        final Map<String, String> headers;
    }

    private abstract class LineReader extends AbstractReader {
        protected LineReader(int offset) {
            super(LF_10, offset);
        }

        @Override
        protected Next next(ByteBuf in, int length) {
            int start = in.readerIndex() + offset;
            int readerIndex = offset + length + 1;
            if (length == 0) return new Next(line(EMPTY_BUFFER, readerIndex));
            if (in.getByte(start + length - 1) == CR_13) length -= 1;
            return new Next(line(in.slice(start, length), readerIndex));
        }

        protected abstract Reader line(ByteBuf line, int offset);
    }

    private abstract class AbstractReader implements Reader {
        protected AbstractReader(byte broken, int offset) {
            this.broken = broken;
            this.offset = offset;
        }

        @Override
        public Result<?> read(ByteBuf in) {
            int start = in.readerIndex() + offset;
            int end = in.writerIndex();
            for (int i = start; i < end; i++) {
                if (in.getByte(i) == broken) {//找到broken,处理下一部分
                    return next(in, i - start);//length = i - start
                }
            }
            return new Broken(this);//没找到,frame不完整
        }

        protected abstract Result<?> next(ByteBuf in, int length);

        protected final byte broken;
        protected int offset;

        protected Reader offset(int offset) {
            this.offset = offset;
            return this;
        }
    }


    private interface Reader {
        Result<?> read(ByteBuf in);
    }

    private abstract class Result<T> {
        protected Result(T value, State state) {
            this.value = value;
            this.state = state;
        }

        final T value;
        final State state;
    }

    private enum State {
        BROKEN, NEXT, DONE;
    }

    private final class Broken extends Result<Reader> {
        Broken(Reader reader) {
            super(reader, State.BROKEN);
        }
    }

    private final class Next extends Result<Reader> {
        Next(Reader reader) {
            super(reader, State.NEXT);
        }
    }

    private final class Done extends Result<Frame> {
        Done(Frame frame) {
            super(frame, State.DONE);
        }
    }


    private int contentLength(Map<String, String> headers) {
        String value = headers.get(Heads.CONTENT_LENGTH);
        if (value == null) return -1;
        return Integer.valueOf(value);
    }

    private byte[] toBytes(ByteBuf in) {
        if (in.hasArray()) {
            return in.array();
        } else {
            byte[] b = new byte[in.readableBytes()];
            in.readBytes(b);
            return b;
        }
    }

    /**
     * 此方法只适合ASCII码
     *
     * @param in
     * @return
     */
    private String fastToString(ByteBuf in) {
        int max = in.writerIndex(), i = in.readerIndex();
        if (max - i == 0) return EMPTY_LINE;
        StringBuilder sb = new StringBuilder(max - i);
        for (; i < max; i++) {
            sb.append((char) in.getByte(i));
        }
        return sb.toString();
    }
}