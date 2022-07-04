package com.wei.starter.configure;

import com.wei.starter.core.push.PushService;
import com.wei.starter.core.push.common.RedisSubConfig;
import com.wei.starter.core.push.impl.netty.NettyPushServiceImpl;
import com.wei.starter.core.push.impl.netty.StompPushServiceImpl;
import com.wei.starter.core.push.impl.netty.standard.WebsocketServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.Resource;

@Configuration
@PropertySource("classpath:/websocket.properties")
@Import(RedisSubConfig.class)
@EnableConfigurationProperties(value = WebsocketServerProperties.class)
public class WebSocketConfiguration {

    @Resource
    private WebsocketServerProperties websocketServerProperties;

    @Bean
    @ConditionalOnProperty(prefix = "wei.websocket", name = "StompProtocol", havingValue = "false")
    public PushService pushService(){
        return new NettyPushServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(prefix = "wei.websocket", name = "StompProtocol", havingValue = "true")
    public PushService StompPushService(){
        return new StompPushServiceImpl();
    }

    @Bean
    @ConditionalOnProperty(prefix = "wei.websocket", name = "enable", havingValue = "true", matchIfMissing = true)
    public WebsocketServer websocketServer(PushService pushService){
        WebsocketServer websocketServer = new WebsocketServer(pushService);
        websocketServer.init(websocketServerProperties);
        websocketServer.start();
        return websocketServer;
    }

}
