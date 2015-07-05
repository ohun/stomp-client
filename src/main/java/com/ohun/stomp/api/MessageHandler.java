package com.ohun.stomp.api;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public interface MessageHandler {
    void onMessage(Message message);
}
