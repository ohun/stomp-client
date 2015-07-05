package com.ohun.stomp;

import com.ohun.stomp.api.*;
import com.ohun.stomp.common.ChannelContext;
import com.ohun.stomp.common.ClientListenerAdapter;
import com.ohun.stomp.common.StompConfig;
import com.ohun.stomp.netty.NettyChannel;
import com.ohun.stomp.protocol.*;

import java.util.HashMap;
import java.util.Map;

import static com.ohun.stomp.common.ChannelContext.State.CONNECTED;
import static com.ohun.stomp.protocol.Constants.UTF_8;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 *
 * @author ohun@live.cn
 * @version 1.0
 */
public final class StompClient extends Stomp implements Receiver, EventHandler {

    private ClientListener clientListener = ClientListenerAdapter.INSTANCE;

    private Version version = Version.VERSION_1_2;

    private final StompClientManager clientManager;

    private final ChannelContext context;

    private final Channel channel;

    private final StompConfig config;

    public StompClient(StompConfig config, StompClientManager clientManager) {
        this.config = config;
        this.clientManager = clientManager;
        this.clientListener = clientManager;
        this.channel = new NettyChannel(this, config);
        this.context = new ChannelContext(config.getConnectTimeout(), channel);
    }


    @Override
    protected void transmit(Command c, Map<String, String> h, byte[] b) {
        try {
            channel.write(FrameEncode.encode(c, h, b));
        } catch (Exception e) {
            exception(e.getMessage(), e);
        }
    }

    void connect() {
        context.connect();
    }

    void disconnect() {
        context.disconnect();
    }

    @Override
    public void connect(StompConfig config) throws Exception {
        Map<String, String> header = new HashMap<String, String>(4);
        header.put(Heads.ACCEPT_VERSION, Version.supported());
        header.put(Heads.HEART_BEAT, config.getHeartbeatX() + "," + config.getHeartbeatY());
        if (config.getLogin() != null) header.put(Heads.LOGIN, config.getLogin());
        if (config.getPass() != null) header.put(Heads.PASSCODE, config.getPass());
        super.connect(header);
    }

    @Override
    public void disconnect(StompConfig config) throws Exception {
        super.disconnect(null);
        clientListener.onDisconnected(this);
    }

    @Override
    public void reconnect() throws Exception {
        this.disconnect();
        this.connect();
        this.resubscribe();
        logger.warn("stomp client reconnect success, config=" + config);
    }

    @Override
    public void exception(String message, Throwable throwable) {
        logger.error("stomp client receive an exception, message=" + message, throwable);
        clientListener.onException(this, throwable);
    }

    @Override
    public void onMessage(Frame frame) {
        String destination = frame.heads.get(Heads.DESTINATION);
        String id = frame.heads.get(Heads.SUBSCRIPTION);
        MessageConsumer consumer = clientManager.consumerManager.get(destination, id);
        if (consumer != null) {
            consumer.messageReceived(new Message(this, frame.heads, frame.body));
        }
    }

    @Override
    public void onConnected(Frame frame) {
        logger.info("Stomp client connected heads={}", frame.heads);
        setVersion(frame.heads);
        setHeartbeat(frame.heads);
        context.setConnectionState(CONNECTED);
        clientListener.onConnected(this);
    }

    @Override
    public void onError(Frame frame) {
        logger.error("Stomp client received an error,heads={},message={}",
                frame.heads, new String(frame.body, UTF_8));
        clientListener.onDisconnected(this);
    }

    @Override
    public void onReceipt(Frame frame) {
        String receiptId = frame.heads.get(Heads.RECEIPT_ID);
        if (receiptId != null) {
            receiptManager.onReceipt(frame.heads.get(Heads.RECEIPT_ID));
        } else {
            logger.error("Stomp client received receipt cmd but no receipt-id in heads");
        }
    }

    @Override
    public void receive(Frame frame) {
        switch (frame.command) {
            case MESSAGE:
                onMessage(frame);
                break;
            case ERROR:
                onError(frame);//no break, error cmd may be contains the receipt-id head
            case RECEIPT:
                onReceipt(frame);
                break;
            case CONNECTED:
                onConnected(frame);
                break;
        }
    }

    public void ack(Message message) {
        super.ack(getAckHeaders(message));
    }

    public void nack(Message message) {
        super.nack(getAckHeaders(message));
    }

    private void setHeartbeat(Map<String, String> heads) {
        logger.warn("set heartbeat from stomp server, heads=" + heads);
        if (heads == null) return;
        String heartbeat = heads.get(Heads.HEART_BEAT);
        if (heartbeat == null) return;
        String[] rw = heartbeat.split(",");
        if (rw.length != 2) return;
        long readTimeout = Long.valueOf(rw[0]);
        long writeTimeout = Long.valueOf(rw[1]);
        if (config.getHeartbeatY() == 0) readTimeout = 0;
        if (config.getHeartbeatX() == 0) writeTimeout = 0;
        if (readTimeout == 0 && writeTimeout == 0) return;
        channel.setHeartbeat(readTimeout, writeTimeout);
    }

    public void resubscribe() throws Exception {
        clientManager.consumerManager.resubscribe(this);
    }

    public boolean isConnected() {
        return context.isConnected();
    }

    public Version getVersion() {
        return version;
    }

    protected void setVersion(final Map<String, String> heads) {
        if (heads == null) return;
        String vs = heads.get(Heads.VERSION);
        this.version = Version.toVersion(vs);
    }

    public void setClientListener(ClientListener clientListener) {
        this.clientListener = clientListener;
    }

    public StompConfig getConfig() {
        return config;
    }

    private Map<String, String> getAckHeaders(Message message) {
        Map<String, String> map = new HashMap<String, String>(4);
        switch (this.version) {
            case VERSION_1_2:
                map.put(Heads.ID, message.headers.get(Heads.ACK));
                break;
            case VERSION_1_0:
            case VERSION_1_1:
                map.put(Heads.MESSAGE_ID, message.headers.get(Heads.MESSAGE_ID));
                map.put(Heads.SUBSCRIPTION, message.headers.get(Heads.SUBSCRIPTION));

        }
        return map;
    }
}
