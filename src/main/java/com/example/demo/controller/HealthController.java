package com.example.demo.controller;

import com.example.demo.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    private final static String virtualIp = "192.168.1.60";

    @GetMapping("/check")
    public String check(HttpServletRequest request) {
        String ip = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String protocol = request.getProtocol();
        String host = request.getRemoteHost();
        int port = request.getRemotePort();
        String userAgent = request.getHeader("User-Agent");
        String accept = request.getHeader("Accept");
        String referer = request.getHeader("Referer");
        
        System.out.println("=== 请求详细信息 ===");
        System.out.println("客户端IP: " + ip);
        System.out.println("请求方法: " + method);
        System.out.println("请求URI: " + uri);
        System.out.println("查询参数: " + queryString);
        System.out.println("协议: " + protocol);
        System.out.println("远程主机: " + host);
        System.out.println("远程端口: " + port);
        System.out.println("User-Agent: " + userAgent);
        System.out.println("Accept: " + accept);
        System.out.println("Referer: " + referer);
        System.out.println("==================");
        
        return "ok-2";
    }

    @GetMapping("/test")
    public String test() {
        // 构建请求参数，如果需要绑定虚拟IP则设置localAddress
        HttpUtils.RequestParam.RequestParamBuilder builder = HttpUtils.RequestParam.builder()
                .method(HttpGet.METHOD_NAME)
                .contentType(ContentType.APPLICATION_JSON)
                .url("http://192.168.1.119:10088/location/health/check");

        // 如果配置了虚拟IP，则绑定到该IP
        if (virtualIp != null && !virtualIp.trim().isEmpty()) {
            try {
                InetAddress localAddr = InetAddress.getByName(virtualIp.trim());
                builder.localAddress(localAddr);
                log.info("位置查询请求将绑定到虚拟IP: {}", virtualIp);
            } catch (Exception e) {
                log.error("解析虚拟IP失败: {}, 将使用默认路由", virtualIp, e);
            }
        }

        return HttpUtils.doRequest(builder.build());
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }
}
