package com.ohun.stomp;

import com.ohun.stomp.common.ReceiptFuture;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaoxu.yxx on 2014/10/2.
 */
public final class MessageProducer {
    private final ProducerManager manager;
    private final String destination;
    private final AtomicInteger txIdCounter = new AtomicInteger();

    MessageProducer(ProducerManager manager, String destination) {
        this.manager = manager;
        this.destination = destination;
    }

    public void send(String content) {
        manager.acquireClient().send(destination, content);
    }

    public void send(byte[] content, Map<String, String> heads) {
        manager.acquireClient().send(destination, content, heads);
    }

    public void send(byte[] content) {
        manager.acquireClient().send(destination, content, null);
    }

    public ReceiptFuture sendW(byte[] content, Map<String, String> heads) {
        return manager.acquireClient().sendW(destination, content, heads);
    }

    void send(StompClient client, String content) {
        client.send(destination, content);
    }

    void send(StompClient client, byte[] content) {
        client.send(destination, content, null);
    }


    public StompTransaction begin() {
        String txId = nextTxId();
        StompClient client = manager.acquireClient();
        client.begin(txId);
        return new StompTransaction(txId, this, client);
    }

    private String nextTxId() {
        return "tx-" + this.txIdCounter.getAndIncrement();
    }

}
