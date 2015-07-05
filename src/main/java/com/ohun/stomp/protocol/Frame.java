package com.ohun.stomp.protocol;

import java.util.Map;

import static com.ohun.stomp.protocol.Constants.EMPTY_BYTES;
import static java.util.Collections.EMPTY_MAP;

/**
 * Created by xiaoxu.yxx on 2014/7/18.
 */
public final class Frame {
    public final Command command;
    public final Map<String, String> heads;
    public final byte[] body;

    public Frame(Command command) {
        this(command, EMPTY_MAP, EMPTY_BYTES);
    }

    public Frame(Command command, byte[] body) {
        this(command, EMPTY_MAP, body);
    }

    public Frame(Command command, Map<String, String> heads, byte[] body) {
        this.command = command;
        this.heads = heads;
        this.body = body;
    }
}
