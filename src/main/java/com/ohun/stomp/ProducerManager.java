package com.ohun.stomp;

import com.ohun.stomp.util.Function;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by xiaoxu.yxx on 2014/10/18.
 */
final class ProducerManager {
    private final StompClientManager stompClientManager;

    private final AtomicLong pos = new AtomicLong(0);

    ProducerManager(StompClientManager stompClientManager) {
        this.stompClientManager = stompClientManager;
    }

    StompClient acquireClient() {
        StompClient client = getNext();
        if (client.isConnected()) return client;
        StompClient stompClient = stompClientManager.foreach(new Function<StompClient, Boolean>() {
            @Override
            public Boolean apply(StompClient input) {
                return input.isConnected();
            }
        });
        return stompClient == null ? client : stompClient;
    }

    StompClient getNext() {
        List<StompClient> clients = stompClientManager.getClients();
        return clients.get(((int) (pos.incrementAndGet() % clients.size())));
    }

    void shutdown() {
    }

}
