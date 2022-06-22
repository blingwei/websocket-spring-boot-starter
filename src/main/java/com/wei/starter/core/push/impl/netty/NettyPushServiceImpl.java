package com.wei.starter.core.push.impl.netty;

import com.wei.starter.core.push.bo.Message;
import com.wei.starter.core.push.impl.DefaultPushService;
import com.wei.starter.core.push.impl.netty.standard.ChannelManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * @Author lw
 * @Date 2022/1/24  下午2:40
 * @Version 1.0
 */
@Slf4j
public class NettyPushServiceImpl extends DefaultPushService {


    @Override
    public boolean checkUserConnectedToThisServer(String userId) {
        return ChannelManager.getChannel(userId).isPresent();
    }

    @Override
    public boolean sendMsgToUserFromThisServer(String userId, Message message) {
        Optional<Channel> optionalChannel = ChannelManager.getChannel(userId);
        if (!optionalChannel.isPresent() || message == null || StringUtils.isBlank(message.getSeq())) {
            return false;
        }
        try {
            Channel channel = optionalChannel.get();
            channel.writeAndFlush(new TextWebSocketFrame(message.getContent()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("消息发送失败" + message.toString());
            log.error(e.getMessage());
        }
        return false;
    }

}

