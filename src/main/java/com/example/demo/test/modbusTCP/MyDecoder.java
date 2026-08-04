package com.example.demo.test.modbusTCP;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 功能描述: 自定义接收消息格式
 */
@Slf4j
public class MyDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        log.info("📡 接收到来自 {} 的数据", channelHandlerContext.channel().remoteAddress());
        String data =
                byteBuf.toString(StandardCharsets.US_ASCII);

        // 查找换行
        int endIndex = data.indexOf("\r\n");

        if (endIndex < 0) {
            return;
        }

        String sentence = data.substring(0, endIndex);

        // 消费掉已经处理的数据
        byteBuf.skipBytes(endIndex + 2);

        if (sentence.startsWith("$GNGGA")
                || sentence.startsWith("$GPGGA")) {

            list.add(sentence);
        }
    }

    public String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public static String toHexString1(byte[] b) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    public static String toHexString1(byte b) {
        String s = Integer.toHexString(b & 0xFF);
        if (s.length() == 1) {
            return "0" + s;
        } else {
            return s;
        }
    }

}
