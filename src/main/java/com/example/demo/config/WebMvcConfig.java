package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${file.upload.local-path:frontend/src/assets/iconBase}")
    private String localPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /iconBase/** 映射到外部文件夹
        registry.addResourceHandler("/iconBase/**")
                .addResourceLocations("file:D:/code/back/erms_platform_v6_three/frontend/src/assets/iconBase/");
    }
}
