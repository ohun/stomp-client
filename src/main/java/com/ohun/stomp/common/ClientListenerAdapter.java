package com.ohun.stomp.common;

import com.ohun.stomp.StompClient;
import com.ohun.stomp.api.ClientListener;

/**
 * Created by xiaoxu.yxx on 2014/10/9.
 */
public class ClientListenerAdapter implements ClientListener {
    public static final ClientListener INSTANCE = new ClientListenerAdapter();

    @Override
    public void onConnected(StompClient client) {

    }

    @Override
    public void onDisconnected(StompClient client) {

    }

    @Override
    public void onException(StompClient client, Throwable throwable) {
        try {
            client.reconnect();
        } catch (Throwable e) {
        }
    }
}
