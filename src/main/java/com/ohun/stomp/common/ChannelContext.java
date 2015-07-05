package com.ohun.stomp.common;

import com.ohun.stomp.api.Channel;
import com.ohun.stomp.api.StompException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

import static com.ohun.stomp.common.ChannelContext.State.CONNECTED;
import static com.ohun.stomp.common.ChannelContext.State.CONNECTING;

/**
 * Created by xiaoxu.yxx on 2014/7/26.
 */
public final class ChannelContext {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Object stateLock = new Object();
    private final Channel channel;
    private final long timeout;

    public ChannelContext(long timeout, Channel channel) {
        this.channel = channel;
        this.timeout = timeout;
    }

    public void connect() throws StompException {
        if (!channel.isClosed()) channel.close();
        try {
            this.channel.open();
            this.waitForConnected();
        } catch (Exception e) {
            this.channel.close();
            this.setConnectionState(State.DISCONNECTED);
            throw new StompException("channel open happened exception, channel=" + channel, e);
        }
        logger.info("Stomp Client connect server success, channel=" + channel);
    }

    public boolean isConnected() {
        return this.state == CONNECTED && !this.channel.isClosed();
    }

    public boolean isClosed() {
        return this.channel.isClosed();
    }

    public void setConnectionState(State state) {
        synchronized (this.stateLock) {
            this.state = state;
            this.stateLock.notifyAll();
        }
    }

    public void waitForConnected() throws Exception {
        if (this.isConnected()) return;
        setConnectionState(CONNECTING);
        synchronized (this.stateLock) {
            this.stateLock.wait(timeout);
        }
        if (!this.isConnected()) {
            throw new TimeoutException("Connection timed out.");
        }
    }

    protected void waitForDisconnected() throws Exception {
        if (!this.isConnected()) return;
        setConnectionState(State.DISCONNECTING);
        synchronized (this.stateLock) {
            this.stateLock.wait(timeout);
        }
        if (this.isConnected()) {
            throw new TimeoutException("Disconnection timed out.");
        }
    }

    public void disconnect() {
        try {
            this.channel.close();
            this.waitForDisconnected();
        } catch (Exception e) {
            this.setConnectionState(State.DISCONNECTED);
            throw new StompException("channel disconnect happened exception, channel=" + channel);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public State getState() {
        return state;
    }

    public enum State {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING,
    }

    private State state = State.DISCONNECTED;
}
