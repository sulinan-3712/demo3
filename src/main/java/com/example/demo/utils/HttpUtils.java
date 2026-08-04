package com.example.demo.utils;

import com.alibaba.fastjson2.JSON;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author youzi
 * @date 2023-08-11
 * @desc Http请求执行工具
 */
@Slf4j
public class HttpUtils {

    /**
     * 内容格式 - JSON
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * 内容格式 - 表单
     */
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

    /**
     * 执行 Get 请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @return 请求结果
     */
    public static String doGet(String url, Map<String, String> headerMap, Map<String, String> paramMap) {
        return doRequest(RequestParam.builder()
                .method(HttpGet.METHOD_NAME)
                .contentType(ContentType.APPLICATION_FORM_URLENCODED)
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .build());
    }

    /**
     * 执行 Post 请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @param bodyMap 请求体参数
     * @return 请求结果
     */
    public static String doPost(String url, Map<String, String> headerMap, Map<String, String> paramMap, Map<String, String> bodyMap) {
        return doRequest(RequestParam.builder()
                .method(HttpPost.METHOD_NAME)
                .contentType(ContentType.APPLICATION_JSON)
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .bodyMap(bodyMap)
                .build());
    }

    /**
     * 执行 Post 表单请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @param bodyMap 请求体参数
     * @return 请求结果
     */
    public static String doPostForm(String url, Map<String, String> headerMap, Map<String, String> paramMap, Map<String, String> bodyMap) {
        return doRequest(RequestParam.builder()
                .method(HttpPost.METHOD_NAME)
                .contentType(ContentType.create(APPLICATION_FORM_URLENCODED, Consts.UTF_8))
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .bodyMap(bodyMap)
                .build());
    }

    /**
     * 执行 Post 文件流请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @param inputStream 文件输入流
     * @param fullName 文件名称
     * @return 请求结果
     */
    public static String doPostMultipart(String url, Map<String, String> headerMap, Map<String, String> paramMap,
                                         InputStream inputStream, String fullName) {
        return doRequest(RequestParam.builder()
                .method(HttpPost.METHOD_NAME)
                .contentType(ContentType.MULTIPART_FORM_DATA)
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .inputStream(inputStream)
                .fullName(fullName)
                .build());
    }

    /**
     * 执行 Put 请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @param bodyMap 请求体参数
     * @return 请求结果
     */
    public static String doPut(String url, Map<String, String> headerMap, Map<String, String> paramMap, Map<String, String> bodyMap) {
        return doRequest(RequestParam.builder()
                .method(HttpPut.METHOD_NAME)
                .contentType(ContentType.APPLICATION_JSON)
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .bodyMap(bodyMap)
                .build());
    }

    /**
     * 执行 Delete 请求
     *
     * @param url 请求地址
     * @param headerMap 请求头参数
     * @param paramMap 请求参数
     * @return 请求结果
     */
    public static String doDelete(String url, Map<String, String> headerMap, Map<String, String> paramMap) {
        return doRequest(RequestParam.builder()
                .method(HttpDelete.METHOD_NAME)
                .contentType(ContentType.APPLICATION_FORM_URLENCODED)
                .url(url)
                .headerMap(headerMap)
                .paramMap(paramMap)
                .build());
    }

    /**
     * 执行请求
     *
     * @param request 请求参数
     * @return 请求结果
     */
    public static String doRequest(RequestParam request) {
        if (request == null) {
            log.error("Request param is null");
            return null;
        }
        if (request.getUrl() == null || "".equals(request.getUrl().trim())) {
            log.error("Request url is blank");
            return null;
        }
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            // 是否为https
            boolean isHttps = request.getUrl().toLowerCase().startsWith("https://");
            // 获取CloseableHttpClient
            client = getClient(isHttps, request.getLocalAddress());
            // 构建请求
            HttpUriRequest httpRequest = buildRequest(request);
            // 执行请求
            response = client.execute(httpRequest);
            // 请求结果
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } catch (java.net.BindException e) {
            // 如果是绑定本地地址失败,尝试不使用本地地址重试
            if (request.getLocalAddress() != null) {
                log.warn("绑定虚拟地址 {} 失败,将使用系统默认路由重试: {}",
                    request.getLocalAddress().getHostAddress(), e.getMessage());
                HttpClientUtils.closeQuietly(response);
                HttpClientUtils.closeQuietly(client);
                try {
                    // 重新创建客户端,不绑定本地地址
                    boolean isHttps = request.getUrl().toLowerCase().startsWith("https://");
                    client = getClient(isHttps, null);
                    HttpUriRequest httpRequest = buildRequest(request);
                    response = client.execute(httpRequest);
                    return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
                } catch (Exception retryEx) {
                    log.error("重试请求也失败", retryEx);
                }
            } else {
                log.error("Request error!", e);
            }
        } catch (Exception e) {
            log.error("Request error!", e);
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(client);
        }
        return null;
    }

    /**
     * 获取CloseableHttpClient
     *
     * @param isHttps 是否为 https 协议
     * @param localAddress 本地绑定的IP地址(可为null,表示使用系统默认路由)
     * @return CloseableHttpClient对象
     * @throws NoSuchAlgorithmException 无此算法异常
     * @throws KeyStoreException 密钥存储异常
     * @throws KeyManagementException 密钥管理异常
     */
    private static CloseableHttpClient getClient(boolean isHttps, InetAddress localAddress) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                // 数据交互间隔超时
                .setSocketTimeout(5000)
                // 连接超时
                .setConnectTimeout(5000)
                // 请求连接超时
                .setConnectionRequestTimeout(5000);
        
        // 如果指定了本地IP地址,则绑定到该地址
        if (localAddress != null) {
            requestConfigBuilder.setLocalAddress(localAddress);
            log.info("HTTP客户端已配置绑定本地地址: {}", localAddress.getHostAddress());
        }
        
        RequestConfig requestConfig = requestConfigBuilder.build();
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(requestConfig);
        
        if (isHttps) {
            // 绕过 SSL 的检验
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
            httpClientBuilder.setSSLSocketFactory(scsf);
        }
        return httpClientBuilder.build();
    }

    /**
     * 构建请求
     *
     * @param request 请求参数
     * @return HttpUriRequest对象
     * @throws URISyntaxException URI语法异常
     */
    private static HttpUriRequest buildRequest(RequestParam request) throws URISyntaxException {
        // 请求构建器
        RequestBuilder builder = RequestBuilder.create(request.getMethod());

        // 设置请求头
        if (request.getHeaderMap() != null && !request.getHeaderMap().isEmpty()) {
            request.getHeaderMap().forEach(builder::addHeader);
        }

        // 设置请求参数
        URIBuilder uriBuilder = new URIBuilder(request.getUrl());
        if (request.getParamMap() != null && !request.getParamMap().isEmpty()) {
            request.getParamMap().forEach(uriBuilder::addParameter);
        }
        builder.setUri(uriBuilder.build());

        // 设置请求体
        if (request.getBodyMap() != null && !request.getBodyMap().isEmpty()) {
            EntityBuilder entityBuilder = EntityBuilder.create().setContentType(request.getContentType());
            // 内容格式
            String contentType = request.getContentType().getMimeType();
            // 表单
            if (APPLICATION_FORM_URLENCODED.equals(contentType)) {
                List<NameValuePair> parameters = new ArrayList<>(request.getBodyMap().size());
                request.getBodyMap().forEach((key, value) -> parameters.add(new BasicNameValuePair(key, value)));
                entityBuilder.setParameters(parameters);
            }
            // JSON
            if (APPLICATION_JSON.equals(contentType)) {
                entityBuilder.setText(JSON.toJSONString(request.getBodyMap()));
            }
            builder.setEntity(entityBuilder.build());
        } else if (request.getInputStream() != null && request.getFullName() != null) {
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", request.getInputStream(), request.getContentType(), request.getFullName())
                    // 浏览器兼容模式，防止文件中文名乱码
                    .setCharset(StandardCharsets.UTF_8)
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .build();
            builder.setEntity(entity);
        }
        return builder.build();
    }

    /**
     * 请求参数
     */
    @Getter
    @Builder
    public static class RequestParam {

        /**
         * 请求方式
         */
        private String method;

        /**
         * 内容格式
         */
        private ContentType contentType;

        /**
         * 请求地址
         */
        private String url;

        /**
         * 请求头参数Map
         */
        private Map<String, String> headerMap;

        /**
         * 请求参数Map
         */
        private Map<String, String> paramMap;

        /**
         * 请求体Map
         */
        private Map<String, String> bodyMap;

        /**
         * 文件输入流
         */
        private InputStream inputStream;

        /**
         * 文件名称
         */
        private String fullName;

        /**
         * 本地绑定的IP地址（用于Keepalived等高可用场景，确保对方看到的是虚拟IP）
         * 如果为null，则使用系统默认路由选择的IP
         */
        private InetAddress localAddress;

    }

}
