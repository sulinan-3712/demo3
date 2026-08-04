package com.example.demo.test.modbusTCP;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.nio.charset.StandardCharsets;

/**
 * 功能描述: 服务端初始化，客户端与服务器端连接一旦创建，这个类中方法就会被回调，设置出站编码器和入站解码器
 */
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
//        ChannelPipeline pipeline = socketChannel.pipeline();
//        //接收消息格式,使用自定义解析数据格式
//        pipeline.addLast("decoder", new MyDecoder());
//        //发送消息格式，使用自定义解析数据格式
//        pipeline.addLast("encoder", new MyEncoder());
//
//        //自定义的空闲检测
//        pipeline.addLast(new NettyServerHandler());

        ChannelPipeline pipeline =
                socketChannel.pipeline();

        pipeline.addLast(
                new DelimiterBasedFrameDecoder(
                        2048,
                        Unpooled.wrappedBuffer(
                                "\r\n".getBytes(StandardCharsets.US_ASCII))
                ));

        pipeline.addLast(
                new StringDecoder(
                        StandardCharsets.US_ASCII));

        pipeline.addLast(
                new NettyServerHandler());
    }
}
