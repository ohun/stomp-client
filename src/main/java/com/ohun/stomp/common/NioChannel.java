package com.ohun.stomp.common;

import com.ohun.stomp.api.Channel;
import com.ohun.stomp.api.Receiver;
import com.ohun.stomp.protocol.FrameDecode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public final class NioChannel implements Channel, Runnable {
    private SocketChannel socketChannel;
    private Selector selector;
    private Receiver receiver;
    private StompConfig config;

    public NioChannel(Receiver receiver) {
        this.receiver = receiver;
    }


    @Override
    public void write(byte[] buffer) throws IOException {
        socketChannel.write(ByteBuffer.wrap(buffer));
    }

    @Override
    public boolean isClosed() {
        return socketChannel == null || !socketChannel.isOpen();
    }

    @Override
    public void open() throws Exception {
        this.initChannel(config.getHost(), config.getPort());
        this.start();
    }


    private void start() {
        Thread thread = new Thread(this);
        thread.setName("NioChannel-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void initChannel(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open();
        this.selector = Selector.open();
        this.socketChannel.configureBlocking(true);
        this.socketChannel.connect(new InetSocketAddress(host, port));
    }

    @Override
    public void run() {
        try {
            socketChannel.configureBlocking(false);
            socketChannel.register(this.selector, SelectionKey.OP_READ);
            Thread thread = Thread.currentThread();
            while (!thread.isInterrupted()) {
                int select = selector.select();
                if (select > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isReadable()) {
                            read(sk);
                        }
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e) {
            receiver.exception(e.getMessage(), e);
        } finally {
            close();
        }
    }


    protected void read(SelectionKey sk) throws IOException {
        byte[] data = null;
        try {
            SocketChannel channel = (SocketChannel) sk.channel();
            int length = 1024;
            ByteBuffer buffer = ByteBuffer.allocate(length);
            channel.read(buffer);

            if (buffer.position() < length) {
                data = buffer.array();
            } else {
                int l = length;
                do {
                    data = new byte[l];
                    System.arraycopy(buffer.array(), 0, data, l - length, l);
                    l = l + l;
                    buffer.clear();
                } while (channel.read(buffer) > 0);
            }
            receiver.receive(FrameDecode.decode(data));
        } catch (Exception e) {
            receiver.exception(e.getMessage(), e);
        }
    }


    @Override
    public void close() {
        try {
            receiver.disconnect(config);
        } catch (Exception e) {/* We ignore these. */ }
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {/* We ignore these. */}
    }

    @Override
    public void setHeartbeat(long readTimeout, long writeTimeout) {

    }
}
