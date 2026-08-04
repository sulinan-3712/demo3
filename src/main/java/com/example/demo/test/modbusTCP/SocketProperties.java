package com.example.demo.test.modbusTCP;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 功能描述: 配置类
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "socket")
public class SocketProperties {
    private Integer port;
    private String host;

}

