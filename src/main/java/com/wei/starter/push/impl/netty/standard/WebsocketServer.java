package com.wei.starter.push.impl.netty.standard;

import com.wei.starter.push.PushService;
import com.wei.starter.push.configure.WebsocketServerProperties;
import com.wei.starter.push.impl.netty.NettyPushServiceImpl;
import com.wei.starter.push.impl.netty.standard.stomp.StompEndpointServer;
import com.wei.starter.push.impl.netty.standard.stomp.StompVersion;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * @Author lw
 * @Date 2022/1/19  下午4:53
 * @Version 1.0
 */
@Slf4j
public class WebsocketServer implements SmartInitializingSingleton {


    private PushService pushService;

    public WebsocketServer(PushService pushService){
        this.pushService = pushService;
    }

    private static WebsocketServerProperties properties;

    public void init(WebsocketServerProperties WebsocketServerProperties) {
        properties = WebsocketServerProperties;
    }

    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(properties.getBossLoopGroupThreads());
        EventLoopGroup worker = new NioEventLoopGroup(properties.getWorkerLoopGroupThreads());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                //日志级别
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        //心跳监测
                        pipeline.addLast(new IdleStateHandler(180, 0, 0));
                        pipeline.addLast(new HeartBeatHandler());
                        //解析websocket协议
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                        pipeline.addLast(new WebSocketServerProtocolHandler("/websocket/hop-internet-hospital",
                                StompVersion.SUB_PROTOCOLS));
                        EndpointServer endpointServer = new WebSocketEndpointServer(pushService);
                        if(properties.isStompProtocol()){
                            endpointServer = new StompEndpointServer(pushService);
                        }
                        //添加处理器
                        pipeline.addLast(new ServerHandler(endpointServer));
                    }
                });
        bootstrap.bind(properties.getPort()).addListener(future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            log.info("websocket启动成功：" + properties.getHost() + ":" + properties.getPort());
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }

    @Override
    public void afterSingletonsInstantiated() {
        start();
    }
}
