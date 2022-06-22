package com.wei.starter.core.push.impl.netty.standard;

import com.wei.starter.core.push.PushService;
import com.wei.starter.core.push.common.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class EndpointServer {

    protected PushService pushService;

    public EndpointServer(PushService pushService){
        this.pushService = pushService;
    }

    public abstract void doAfterHandshake(ChannelHandlerContext ctx, WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete);


    public abstract void doOnEvent(ChannelHandlerContext ctx, Object evt);

    public abstract void doOnMessage(Channel channel, WebSocketFrame webSocketFrame);

    public void doOnOpen(Channel channel, String loginUserId, String destination){
        //加入队列
        ChannelManager.addChannel(loginUserId,channel);
        //给连接设置userId
        channel.attr(ChannelManager.USER_ID).set(loginUserId);
        //登入服务
        pushService.loginUser(Constants.WS_CLUSTER_USER_ID + loginUserId, Constants.WS_SESSION_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        //补发离线消息
        pushService.pushSupplementMsg(loginUserId, destination);

        //连接关闭时的兜底操作
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                pushService.logoutUser(Constants.WS_CLUSTER_USER_ID + loginUserId);
                ChannelManager.removeChannel(loginUserId);
            }
        });
    }

    public  void doOnClose(Channel ctx){
        ctx.close();
    }

    public void doOnError(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        log.error(ctx.channel().remoteAddress().toString(), cause);
    }
}
