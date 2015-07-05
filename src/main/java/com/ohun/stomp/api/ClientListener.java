package com.ohun.stomp.api;

import com.ohun.stomp.StompClient;

/**
 * Created by xiaoxu.yxx on 2014/7/25.
 */
public interface ClientListener {

    void onConnected(StompClient client);

    void onDisconnected(StompClient client);

    void onException(StompClient client, Throwable throwable);
}
