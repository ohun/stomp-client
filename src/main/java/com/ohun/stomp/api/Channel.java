package com.ohun.stomp.api;

import java.io.IOException;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public interface Channel {

    void open() throws Exception;

    void close();

    boolean isClosed();

    void write(byte[] buffer) throws IOException;

    void setHeartbeat(long readTimeout, long writeTimeout);
}
