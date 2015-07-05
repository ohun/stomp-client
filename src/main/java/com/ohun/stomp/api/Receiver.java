package com.ohun.stomp.api;


import com.ohun.stomp.common.StompConfig;
import com.ohun.stomp.protocol.Frame;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public interface Receiver {
    void connect(StompConfig config) throws Exception;

    void disconnect(StompConfig config) throws Exception;

    void reconnect() throws Exception;

    void receive(Frame message);

    void exception(String message, Throwable throwable);
}
