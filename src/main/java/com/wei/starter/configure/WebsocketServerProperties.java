package com.wei.starter.configure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author lw
 * @Date 2022/1/19  下午4:55
 * @Version 1.0
 */
@Data
@ConfigurationProperties(prefix = "wei.websocket")
public class WebsocketServerProperties {

    private  String host = "0.0.0.0";

    private  int port = 8424;

    private int bossLoopGroupThreads = 16;

    private int workerLoopGroupThreads = 16;

    //是否升级为stomp协议
    private boolean StompProtocol = false;

}
