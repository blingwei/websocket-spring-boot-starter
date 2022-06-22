package com.wei.starter.core.push.impl.netty.standard.stomp;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.netty.handler.codec.stomp.StompHeaders.*;

/**
 * 继承SimpleChannelInboundHandler类读到数据后会把资源释放
 */
@Sharable
public class StompChatHandler extends SimpleChannelInboundHandler<StompFrame> {

    public static final AttributeKey<Set<StompSubscription>> DESTINATIONS = AttributeKey.valueOf("dest");
    private final ConcurrentMap<String, Set<StompSubscription>> chatDestinations =
            new ConcurrentHashMap<String, Set<StompSubscription>>();
    private final StompEndpointServer stompEndpointServer;

    public StompChatHandler(StompEndpointServer stompEndpointServer) {
        this.stompEndpointServer = stompEndpointServer;
    }

    private static void onDisconnect(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String receiptId = inboundFrame.headers().getAsString(RECEIPT);
        if (receiptId == null) {
            ctx.close();
            return;
        }
        StompFrame receiptFrame = new DefaultStompFrame(StompCommand.RECEIPT);
        receiptFrame.headers().set(RECEIPT_ID, receiptId);
        ctx.writeAndFlush(receiptFrame).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送完error帧后要关闭连接
     *
     * @param message
     * @param description
     * @param ctx
     */
    private static void sendErrorFrame(String message, String description, ChannelHandlerContext ctx) {
        StompFrame errorFrame = new DefaultStompFrame(StompCommand.ERROR);
        errorFrame.headers().set(MESSAGE, message);

        if (description != null) {
            errorFrame.content().writeCharSequence(description, CharsetUtil.UTF_8);
        }

        ctx.writeAndFlush(errorFrame).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StompFrame inboundFrame) throws Exception {
        DecoderResult decoderResult = inboundFrame.decoderResult();
        if (decoderResult.isFailure()) {
            sendErrorFrame("rejected frame", decoderResult.toString(), ctx);
            return;
        }

        switch (inboundFrame.command()) {
            case STOMP:
            case CONNECT:
                onConnect(ctx, inboundFrame);
                break;
            case SUBSCRIBE:
                onSubscribe(ctx, inboundFrame);
                break;
            case SEND:
                onSend(ctx, inboundFrame);
                break;
            case ACK:
                onAck(ctx, inboundFrame);
                break;
            case UNSUBSCRIBE:
                onUnsubscribe(ctx, inboundFrame);
                break;
            case DISCONNECT:
                onDisconnect(ctx, inboundFrame);
                break;
            default:
                sendErrorFrame("unsupported command",
                        "Received unsupported command " + inboundFrame.command(), ctx);
        }
    }

    /**
     * 确认这个消息已经被客户端消费了
     *
     * @param ctx
     * @param inboundFrame
     */
    private void onAck(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String msgId = inboundFrame.headers().getAsString(MESSAGE_ID);
        String subscriptionId = inboundFrame.headers().getAsString(SUBSCRIPTION);
        if (msgId == null || subscriptionId == null) {
            sendErrorFrame("missed header", "Required  'id' || 'subscriptionId' header missed", ctx);
            return;
        }
        String dest = "";
        for (Entry<String, Set<StompSubscription>> entry : chatDestinations.entrySet()) {
            for (StompSubscription subscription : entry.getValue()) {
                if (subscription.id().equals(subscriptionId) && subscription.channel().equals(ctx.channel())) {
                    dest = subscription.destination();
                }
            }
        }
        stompEndpointServer.doOnAck(ctx.channel(), dest, msgId);
    }

    private void onSubscribe(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String destination = inboundFrame.headers().getAsString(DESTINATION);
        String subscriptionId = inboundFrame.headers().getAsString(ID);
        String loginUserId = inboundFrame.headers().getAsString(LOGIN);

        if (destination == null || subscriptionId == null) {
            sendErrorFrame("missed header", "Required 'destination' or 'id' header missed", ctx);
            return;
        }
        Set<StompSubscription> subscriptions = chatDestinations.get(destination);
        if (subscriptions == null) {
            subscriptions = new HashSet<StompSubscription>();
            Set<StompSubscription> previousSubscriptions = chatDestinations.putIfAbsent(destination, subscriptions);
            if (previousSubscriptions != null) {
                subscriptions = previousSubscriptions;
            }
        }

        final StompSubscription subscription = new StompSubscription(subscriptionId, destination, ctx.channel());
        if (subscriptions.contains(subscription)) {
            sendErrorFrame("duplicate subscription",
                    "Received duplicate subscription id=" + subscriptionId, ctx);
            return;
        }

        subscriptions.add(subscription);
        Set<StompSubscription> destList = Optional.ofNullable(ctx.channel().attr(DESTINATIONS).get())
                .orElse(new HashSet<>());
        destList.add(subscription);
        ctx.channel().attr(DESTINATIONS).set(destList);
        stompEndpointServer.doOnOpen(ctx.channel(), loginUserId, destination);
        ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> chatDestinations.get(subscription.destination()).remove(subscription));
        String receiptId = inboundFrame.headers().getAsString(RECEIPT);
        if (receiptId != null) {
            StompFrame receiptFrame = new DefaultStompFrame(StompCommand.RECEIPT);
            receiptFrame.headers().set(RECEIPT_ID, receiptId);
            ctx.writeAndFlush(receiptFrame);
        }
    }

    /**
     * 客户端发送消息到服务端
     *
     * @param ctx
     * @param inboundFrame
     */
    private void onSend(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String destination = inboundFrame.headers().getAsString(DESTINATION);
        if (destination == null) {
            sendErrorFrame("missed header", "required 'destination' header missed", ctx);
            return;
        }
        System.out.println("收到消息" + inboundFrame.content().toString());
    }

    private void onUnsubscribe(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String subscriptionId = inboundFrame.headers().getAsString(SUBSCRIPTION);
        for (Entry<String, Set<StompSubscription>> entry : chatDestinations.entrySet()) {
            Iterator<StompSubscription> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                StompSubscription subscription = iterator.next();
                if (subscription.id().equals(subscriptionId) && subscription.channel().equals(ctx.channel())) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    /**
     * 接收到一个CONNECT帧需要回复一个CONNECTED帧
     *
     * @param ctx
     * @param inboundFrame
     */
    private void onConnect(ChannelHandlerContext ctx, StompFrame inboundFrame) {
        String acceptVersions = inboundFrame.headers().getAsString(ACCEPT_VERSION);
        String loginUserId = inboundFrame.headers().getAsString(LOGIN);
        StompVersion handshakeAcceptVersion = ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).get();
        if (acceptVersions == null || !acceptVersions.contains(handshakeAcceptVersion.version())) {
            sendErrorFrame("invalid version",
                    "Received invalid version, expected " + handshakeAcceptVersion.version(), ctx);
            return;
        }
        //回复一个CONNECTED帧
        StompFrame connectedFrame = new DefaultStompFrame(StompCommand.CONNECTED);
        connectedFrame.headers()
                .set(VERSION, handshakeAcceptVersion.version())
                .set(SERVER, "Netty-Server")
                .set(HEART_BEAT, "0,10000");
        ctx.writeAndFlush(connectedFrame);
    }
}
