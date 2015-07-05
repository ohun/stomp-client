package com.ohun.stomp.netty;


import com.ohun.stomp.api.Channel;
import com.ohun.stomp.api.Receiver;
import com.ohun.stomp.common.StompConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelOption.*;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public final class NettyChannel implements Channel, Runnable {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String IDLE_HANDLER_NAME = "heartbeatHandler";

    final Receiver receiver;
    final StompConfig config;
    private io.netty.channel.Channel channel;
    private EventLoopGroup workerGroup;


    public NettyChannel(Receiver receiver, StompConfig config) {
        this.receiver = receiver;
        this.config = config;
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        channel.writeAndFlush(Unpooled.copiedBuffer(buffer));
    }

    @Override
    public boolean isClosed() {
        return workerGroup == null || workerGroup.isShutdown();
    }

    @Override
    public void open() throws Exception {
        this.initChannel();
        this.start();
        this.receiver.connect(config);
        logger.info("NettyChannel opened config=" + config);
    }

    private void start() {
        Thread thread = new Thread(this);
        thread.setName("NettyChannel-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void initChannel() throws Exception {
        this.workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout() - 100)
                    .option(SO_KEEPALIVE, true)
                    .option(TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addFirst(IDLE_HANDLER_NAME, new IdleStateHandler(0, 0, 0));
                            pipeline.addLast(
                                    new StompFrameDecoder(),
                                    new StompMessageHandler(NettyChannel.this)
                            );
                        }
                    });
            this.channel = b.connect(config.getHost(), config.getPort()).sync().channel();
        } catch (Exception e) {
            logger.error("NettyChannel initChannel exception,config=" + config);
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (this.isClosed()) return;
        try {
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
            }
        } catch (Exception e) {
            logger.error("NettyChannel close channel ex, config=" + config, e);
        }
        logger.warn("NettyChannel closed,channel={},config={}", channel, config);
    }

    @Override
    public void setHeartbeat(long readTimeout, long writeTimeout) {
        int r = (int) readTimeout + 3000;
        int w = (int) writeTimeout - 3000;
        channel.pipeline().replace(IDLE_HANDLER_NAME,
                IDLE_HANDLER_NAME, new IdleStateHandler(r, w, 0, TimeUnit.MILLISECONDS));
        logger.warn("NettyChannel setHeartbeat readTimeout={}, writeTimeout={}", r, w);
    }

    @Override
    public void run() {
        try {
            this.channel.closeFuture().sync();
        } catch (Exception e) {
            logger.error("NettyChannel run ex, config=" + config, e);
        } finally {
            this.workerGroup.shutdownGracefully();
            this.workerGroup = null;
            try {
                receiver.disconnect(config);
            } catch (Exception e) {
            }
            logger.warn("NettyChannel shutdownGracefully, config=" + config);
        }
    }

    @Override
    public String toString() {
        return "NettyChannel{" +
                "channel=" + channel +
                ", config=" + config +
                '}';
    }
}