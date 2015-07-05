package com.ohun.stomp;

import com.ohun.stomp.api.Message;
import com.ohun.stomp.api.MessageHandler;
import com.ohun.stomp.util.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xiaoxu.yxx on 2014/7/25.
 */
final class ConsumerManager {
    private final Map<String, Map<String, MessageConsumer>> subscriptions = new ConcurrentHashMap<String, Map<String, MessageConsumer>>(2);

    private final StompClientManager stompClientManager;

    ConsumerManager(StompClientManager stompClientManager) {
        this.stompClientManager = stompClientManager;
    }

    void add(MessageConsumer subscription) {
        Map<String, MessageConsumer> map = subscriptions.get(subscription.getDestination());
        if (map == null) {
            map = new ConcurrentHashMap<String, MessageConsumer>(1);
            subscriptions.put(subscription.getDestination(), map);
        }
        map.put(subscription.getId(), subscription);
    }


    void remove(MessageConsumer subscription) {
        Map<String, MessageConsumer> map = subscriptions.get(subscription.getDestination());
        if (map == null) return;
        map.remove(subscription.getId());
    }

    MessageConsumer get(String destination, String id) {
        Map<String, MessageConsumer> map = subscriptions.get(destination);
        if (map == null) return null;
        return map.get(id);
    }

    List<MessageConsumer> all() {
        List<MessageConsumer> list = new ArrayList<MessageConsumer>();
        for (Map<String, MessageConsumer> map : subscriptions.values()) {
            list.addAll(map.values());
        }
        return list;
    }


    void subscribe(final MessageConsumer consumer) {
        stompClientManager.foreach(new Function<StompClient, Boolean>() {
            @Override
            public Boolean apply(StompClient input) {
                if (input.isConnected()) {
                    input.subscribe(consumer.getDestination(), consumer.getId(), consumer.getHeads());
                }
                return Boolean.TRUE;
            }
        });
        this.add(consumer);
    }

    void unsubscribe(final MessageConsumer consumer) {
        stompClientManager.foreach(new Function<StompClient, Boolean>() {
            @Override
            public Boolean apply(StompClient input) {
                if (input.isConnected()) {
                    input.unsubscribe(consumer.getId());
                }
                return Boolean.TRUE;
            }
        });
        this.remove(consumer);
    }

    void resubscribe(StompClient client) {
        if (!client.isConnected()) return;
        for (MessageConsumer consumer : all()) {
            client.subscribe(consumer.getDestination(), consumer.getId(), consumer.getHeads());
        }
    }

    void shutdown() {
        defaultExecutor.shutdownNow();
    }

    final ExecutorService defaultExecutor = Executors.newSingleThreadExecutor();


    final MessageHandler defaultHandler = new MessageHandler() {
        @Override
        public void onMessage(Message message) {

        }
    };


}
