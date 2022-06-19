package com.wei.starter.push.impl.netty.standard;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;


public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final EndpointServer endpointServer;

    public ServerHandler(EndpointServer endpointServer) {
        this.endpointServer = endpointServer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        endpointServer.doOnError(ctx, cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        endpointServer.doOnMessage(ctx.channel(), (WebSocketFrame) msg);
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //成功握手后的操作
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            endpointServer.doAfterHandshake(ctx, (WebSocketServerProtocolHandler.HandshakeComplete) evt);
        } else {
            endpointServer.doOnEvent(ctx, evt);
            super.userEventTriggered(ctx, evt);
        }
    }

}
