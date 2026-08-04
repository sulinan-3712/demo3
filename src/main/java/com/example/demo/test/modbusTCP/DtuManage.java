package com.example.demo.test.modbusTCP;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能描述: 定时发送Dtu报文
 */
@Slf4j
@Component
public class DtuManage {


    public void sendMsg(){
        ConcurrentHashMap<ChannelId, Channel> channelMap = ChannelMap.getChannelMap();
        if(CollectionUtils.isEmpty(channelMap)){
            log.info("当前没有连接的设备");
            return;
        }
        ConcurrentHashMap.KeySetView<ChannelId, Channel> channelIds = channelMap.keySet();
        byte[] msgBytes = {0x25, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC2, (byte) 0xEF};
        for(ChannelId channelId : channelIds){
            Channel channel = ChannelMap.getChannelByName(channelId);
            // 判断是否活跃
            if(channel==null || !channel.isActive()){
                ChannelMap.getChannelMap().remove(channelId);
                log.info("客户端:{},连接中断", channelId);
                return ;
            }
            // 指令发送
            ByteBuf buffer = Unpooled.buffer();
            log.info("开始发送报文:{}", msgBytes);
            buffer.writeBytes(msgBytes);
            channel.writeAndFlush(buffer).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("客户端:{},回写成功:{}", channelId, msgBytes);
                } else {
                    log.info("客户端:{},回写失败:{}", channelId, msgBytes);
                }
            });
        }
    }

    /**
     * 功能描述: 定时删除不活跃的连接
     * @return void
     */
    public void deleteInactiveConnections(){
        ConcurrentHashMap<ChannelId, Channel> channelMap = ChannelMap.getChannelMap();
        if(!CollectionUtils.isEmpty(channelMap)){
            for (Map.Entry<ChannelId, Channel> next : channelMap.entrySet()) {
                ChannelId channelId = next.getKey();
                Channel channel = next.getValue();
                if (!channel.isActive()) {
                    channelMap.remove(channelId);
                    log.info("客户端:{},连接已经中断", channelId);
                }
            }
        }
    }
}


