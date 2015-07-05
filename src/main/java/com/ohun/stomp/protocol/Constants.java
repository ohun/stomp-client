package com.ohun.stomp.protocol;

import java.nio.charset.Charset;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public interface Constants {
    char EOF = '\u0000';
    char NULL = EOF;
    char LF = '\n';
    byte CR = '\r';
    char COLON = ':';
    byte NULL_0 = (byte) EOF;
    byte CR_13 = (byte) CR;
    byte LF_10 = (byte) LF;
    byte COLON_58 = (byte) COLON;

    int MAX_CMD_LEN = 20;
    int MAX_HEAD_LEN = 10240;

    String EMPTY_LINE = "";
    byte[] EMPTY_BYTES = new byte[0];

    Charset UTF_8 = Charset.forName("UTF-8");
}
