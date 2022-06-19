package com.wei.starter.push.impl.netty.standard.stomp;

import com.wei.starter.push.PushService;
import com.wei.starter.push.bo.Message;
import com.wei.starter.push.impl.netty.standard.ChannelManager;
import com.wei.starter.push.impl.netty.standard.EndpointServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;

public class StompEndpointServer extends EndpointServer {


    public StompEndpointServer(PushService pushService) {
        super(pushService);
    }

    @Override
    public void doAfterHandshake(ChannelHandlerContext ctx, WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
        //升级为stomp协议
        StompVersion stompVersion = StompVersion.findBySubProtocol(handshakeComplete.selectedSubprotocol());
        ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).set(stompVersion);
        ctx.pipeline()
                .addLast(new WebSocketFrameAggregator(65536))
                .addLast(new StompSubframeDecoder()).addLast(new StompWebSocketFrameEncoder())
                .addLast(new StompSubframeAggregator(65536))
                .addLast(new StompChatHandler(this));
    }

    @Override
    public void doOnEvent(ChannelHandlerContext ctx, Object evt) {

    }

    @Override
    public void doOnMessage(Channel channel, WebSocketFrame webSocketFrame) {
        //text或者binary的帧不接收
        if (!(webSocketFrame instanceof TextWebSocketFrame || webSocketFrame instanceof BinaryWebSocketFrame)) {
            channel.close();
        }
    }

    //ack时删除离线消息，根据dest 和 msgId
    public void doOnAck(Channel channel, String dest, String msgId){
        Message message = new Message();
        message.setSeq(msgId);
        pushService.delMsgForSupplement(channel.attr(ChannelManager.USER_ID).get(), dest, message);
    }
}
