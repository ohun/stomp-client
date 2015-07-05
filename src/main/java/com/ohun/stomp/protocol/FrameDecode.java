package com.ohun.stomp.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.ohun.stomp.protocol.Constants.*;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public final class FrameDecode {
    private static Logger logger = LoggerFactory.getLogger(FrameDecode.class);

    public static Frame decode(byte[] array) {
        if (array.length == 0) return null;
        Cursor cursor = new Cursor();
        try {
            Command command = getCommand(array, cursor);
            Map<String, String> heads = getHeads(array, cursor);
            byte[] body = getBody(array, cursor);
            return new Frame(command, heads, body);
        } catch (Exception e) {
            logger.error("MessageDecode decode msg Exception,msg=" + new String(array, UTF_8), e);
        }
        return null;
    }

    public static Command getCommand(byte[] array, Cursor cursor) {
        return Command.valueOf(getLine(array, cursor));
    }

    public static Map<String, String> getHeads(byte[] array, Cursor cursor) {
        Map<String, String> heads = new HashMap<String, String>();
        String line;
        while ((line = getLine(array, cursor)) != null) {
            int index = line.indexOf(COLON);
            if (index == -1) return heads;
            String name = line.substring(0, index);
            String value = line.substring(index + 1);
            heads.put(name, value);
        }
        return heads;
    }

    public static byte[] getBody(byte[] array, Cursor cursor) {
        if (cursor.index >= array.length) return null;
        if (array[cursor.index] == NULL) return null;
        byte[] body = new byte[array.length - cursor.index];

        System.arraycopy(array, cursor.index, body, 0, body.length);
        return body;
        //return ByteBuffer.wrap(array, cursor.index, array.length - cursor.index);
    }


    public static String getLine(byte[] array, Cursor cursor) {
        for (int begin = cursor.index, i = begin; i < array.length; i++) {
            byte b = array[i];
            if (b == CR_13) {//如果当前是回车符,则下一必须是换行符
                if (++i == array.length) return null;
                if (array[i] != LF_10) continue;
                cursor.index = i + 1;
                return new String(array, begin, i - 1 - begin, UTF_8);
            }
            if (b == LF_10) {
                cursor.index = i + 1;
                int L = i - begin;
                if (L == 0) return null;
                return new String(array, begin, L, UTF_8);
            }
        }
        return null;
    }

    private static class Cursor {
        public int index;
    }

}
