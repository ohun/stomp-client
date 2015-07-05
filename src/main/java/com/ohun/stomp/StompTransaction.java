package com.ohun.stomp;

/**
 * Created by xiaoxu.yxx on 2014/10/2.
 */
public final class StompTransaction {
    private final MessageProducer producer;
    private final StompClient client;
    private final String id;

    StompTransaction(String id, MessageProducer producer, StompClient client) {
        this.producer = producer;
        this.client = client;
        this.id = id;
    }

    public void send(String content) {
        producer.send(client, content);
    }

    public void send(byte[] content) {
        producer.send(client, content);
    }

    public void abort() {
        client.abort(this.id);
    }

    public void commit() {
        client.commit(this.id);
    }

    public String getId() {
        return id;
    }
}
