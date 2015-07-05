package com.ohun.stomp.api;

import com.ohun.stomp.StompClient;
import com.ohun.stomp.protocol.Heads;

import java.util.Collections;
import java.util.Map;

import static com.ohun.stomp.protocol.Constants.UTF_8;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public final class Message {
    public final StompClient client;
    public final Map<String, String> headers;
    public final byte[] body;

    public Message(StompClient client, byte[] body) {
        this(client, Collections.EMPTY_MAP, body);
    }

    public Message(StompClient client, Map<String, String> headers, byte[] body) {
        this.client = client;
        this.headers = headers;
        this.body = body;
    }

    public String head(String name) {
        return headers.get(name);
    }

    public String messageId() {
        return head(Heads.MESSAGE_ID);
    }

    public int contentLength() {
        String cl = head(Heads.CONTENT_LENGTH);
        return cl == null ? body.length : Integer.valueOf(cl);
    }

    public String contentType() {
        return head(Heads.CONTENT_TYPE);
    }

    public String getTextBody() {
        return body == null ? null : new String(body, UTF_8);
    }

    public void ack() {
        this.client.ack(this);
    }

    public void nack() {
        this.client.nack(this);
    }

    @Override
    public String toString() {
        return "Message:" +
                "\nheaders=" + headers +
                "\nbody=" + getTextBody();
    }
}
