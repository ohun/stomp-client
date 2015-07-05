package com.ohun.stomp.protocol;

import java.util.Map;

import static com.ohun.stomp.protocol.Constants.*;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 * frame=
 * command EOL
 * ( header EOL )
 * EOL
 * OCTET
 * NULL
 * ( EOL )
 *
 * @author ohun@live.cn
 * @version 1.0
 * @see FrameDecode
 */
public final class FrameEncode {

    public static byte[] encode(Command c, Map<String, String> h, byte[] b) {
        int length = c.name.length();
        if (h != null && h.size() > 0) length += 64 * h.size();
        StringBuilder message = new StringBuilder(length);
        //1.处理命令部分:command EOL
        message.append(c.name).append(LF);

        //2.追加head部分:name:value EOL
        //EOL
        if (h != null && h.size() > 0) {
            for (Map.Entry<String, String> e : h.entrySet()) {
                message.append(e.getKey())
                        .append(COLON)
                        .append(e.getValue())
                        .append(LF);
            }
        }
        message.append(LF);

        byte[] head_part = message.toString().getBytes(Constants.UTF_8);
        byte[] frame_data;
        //3.处理body部分
        if (b != null) {//如果body存在,则追加body,frame = head + body
            frame_data = new byte[head_part.length + b.length + 1];
            System.arraycopy(head_part, 0, frame_data, 0, head_part.length);
            System.arraycopy(b, 0, frame_data, head_part.length, b.length);
        } else {//body 不存在,head部分即为整个frame
            frame_data = new byte[head_part.length + 1];
            System.arraycopy(head_part, 0, frame_data, 0, head_part.length);
        }
        //4.添加frame结束符EOL
        frame_data[frame_data.length - 1] = NULL_0;
        return frame_data;
    }
}
