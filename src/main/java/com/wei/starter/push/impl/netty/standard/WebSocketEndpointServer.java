package com.wei.starter.push.impl.netty.standard;

import com.wei.starter.push.PushService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketEndpointServer extends EndpointServer{


    public WebSocketEndpointServer(PushService pushService) {
        super(pushService);
    }

    @Override
    public void doAfterHandshake(ChannelHandlerContext ctx, WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
        //从头中拿logUserId
        String loginUserId = "132";
        //握手成功后开启连接
        doOnOpen(ctx.channel(), loginUserId, "default");
    }

    @Override
    public void doOnEvent(ChannelHandlerContext ctx, Object evt) {

    }

    @Override
    public void doOnMessage(Channel channel, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            log.info(((TextWebSocketFrame) frame).text());
        }
        if(frame instanceof CloseWebSocketFrame){
            doOnClose(channel);
        }
    }
}
