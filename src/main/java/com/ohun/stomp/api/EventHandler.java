package com.ohun.stomp.api;

import com.ohun.stomp.protocol.Frame;

/**
 * Created by xiaoxu.yxx on 2014/10/9.
 */
public interface EventHandler {
    void onConnected(Frame frame);

    void onMessage(Frame frame);

    void onError(Frame frame);

    void onReceipt(Frame frame);

}
