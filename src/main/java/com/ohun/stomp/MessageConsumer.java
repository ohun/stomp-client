package com.ohun.stomp;

import com.ohun.stomp.api.Message;
import com.ohun.stomp.api.MessageHandler;
import com.ohun.stomp.protocol.AckMode;
import com.ohun.stomp.protocol.Heads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by xiaoxu.yxx on 2014/7/25.
 */
public final class MessageConsumer {
    private final Map<String, String> heads = new HashMap<String, String>(4);
    private final ConsumerManager manager;
    private final String destination;
    private String id;
    private Executor executor;
    private MessageHandler handler;

    MessageConsumer(ConsumerManager manager, String destination) {
        this.manager = manager;
        this.destination = destination;
    }

    public MessageConsumer id(String id) {
        this.id = id;
        return this;
    }

    public MessageConsumer ackMode(AckMode ackMode) {
        heads.put(Heads.ACK, ackMode.name);
        return this;
    }


    public MessageConsumer header(String name, String value) {
        heads.put(name, value);
        return this;
    }


    public MessageConsumer handler(MessageHandler handler) {
        this.handler = handler;
        return this;
    }

    public MessageConsumer executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public void subscribe() {
        if (this.handler == null) this.handler = manager.defaultHandler;
        if (this.executor == null) this.executor = manager.defaultExecutor;
        manager.subscribe(this);
    }

    public void unsubscribe() {
        manager.unsubscribe(this);
    }

    boolean messageReceived(final Message message) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                handler.onMessage(message);
            }
        });
        return true;
    }


    public String getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public Map<String, String> getHeads() {
        return heads;
    }
}
