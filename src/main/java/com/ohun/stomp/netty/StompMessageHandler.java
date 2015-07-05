package com.ohun.stomp.netty;

import com.ohun.stomp.protocol.Constants;
import com.ohun.stomp.protocol.Frame;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xiaoxu.yxx on 2014/7/24.
 */
@ChannelHandler.Sharable
public final class StompMessageHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NettyChannel channel;

    public StompMessageHandler(NettyChannel channel) {
        this.channel = channel;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            logger.warn("One user event Triggered. evt=" + evt);
            super.userEventTriggered(ctx, evt);
            return;
        }
        IdleStateEvent stateEvent = (IdleStateEvent) evt;
        switch (stateEvent.state()) {
            case READER_IDLE:
                channel.receiver.reconnect();
                logger.warn("heartbeat read timeout,chanel closed!");
                break;
            case WRITER_IDLE:
                ctx.writeAndFlush(Unpooled.buffer(1).writeByte(Constants.LF_10));
                logger.warn("heartbeat write timeout,do write an EOL.");
                break;
            case ALL_IDLE:
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("netty channel inactive, config=" + channel.config);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        channel.receiver.exception(cause.getMessage(), cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channel.receiver.receive((Frame) msg);
    }

}