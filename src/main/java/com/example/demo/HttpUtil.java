package com.example.demo;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HttpUtil
 *
 * @author lufeixia
 * @version 1.0
 * @date 2019/11/29 13:57
 * @since 1.8
 */
public class HttpUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

    // 默认链接超时时间
    private static final int DEFAULT_CONNECT_TIMEOUT = 120000;
    // 默认网络超时时间
    private static final int DEFAULT_SOCKET_TIMEOUT = 60000;
    // 默认链接请求超时时间
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 60000;

    /**
     * get 请求
     *
     * @param url url地址
     * @param param 参数
     * @param headers 请求头
     * @param connectTimeout 链接超时时间
     * @param socketTimeout 网络超时时间
     * @param connectionRequestTimeout 链接请求超时时间
     * @param ignoreSsl 是否忽略证书
     * @return get
     * @throws IOException exception
     */
    public static String get(
            String url,
            JSONObject param,
            boolean paramsEncode,
            Map<String, String> headers,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout,
            boolean ignoreSsl)
            throws IOException {
        // 获取链接
        CloseableHttpClient client = null;
        if (ignoreSsl) {
            client = HttpClients.createDefault();
        } else {
            client = createSSLClientDefault();
        }
        // 参数封装
        String params;
        if (paramsEncode) {
            params = httpGetByEncode(param);
        } else {
            params = map2String(param);
        }
        String uri = url + (url.contains("?") ? "&" : "?") + params;
        HttpGet httpGet = new HttpGet(uri);
        // 设置超时时间
        httpGet.setConfig(
                getRequestConfig(connectTimeout, socketTimeout, connectionRequestTimeout));
        // 设置请求头
        setHeader(httpGet, headers);
        String respContent = null;
        // 执行链接
        CloseableHttpResponse resp = null;
        try {
            resp = client.execute(httpGet);
            LOGGER.info("get请求返回接口结果：{}" + resp);
            if (resp.getStatusLine().getStatusCode() == 200) {
                HttpEntity he = resp.getEntity();
                respContent = EntityUtils.toString(he, "UTF-8");
            }
        } catch (IOException e) {
            LOGGER.error("GET请求执行方法报错" + e);
        } finally {
            if (resp != null) {
                resp.close();
            }
            client.close();
        }
        return respContent;
    }

    public static String httpGet(
            String url,
            JSONObject param,
            Map<String, String> headers,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout)
            throws IOException {
        return get(
                url,
                param,
                false,
                headers,
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout,
                true);
    }

    public static String httpGet(String url, JSONObject param, Map<String, String> headers)
            throws IOException {
        return get(
                url,
                param,
                false,
                headers,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_SOCKET_TIMEOUT,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                true);
    }

    public static String httpGet(String url, Map<String, String> headers) throws IOException {
        return get(
                url,
                null,
                false,
                headers,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_SOCKET_TIMEOUT,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                true);
    }

    public static String httpGetForEncode(
            String url,
            JSONObject param,
            Map<String, String> headers,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout)
            throws IOException {
        return get(
                url,
                param,
                true,
                headers,
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout,
                true);
    }

    /**
     * post 请求
     *
     * @param url url地址
     * @param param 参数
     * @param headers 请求头
     * @param connectTimeout 链接超时时间
     * @param socketTimeout 网络超时时间
     * @param connectionRequestTimeout 链接请求超时时间
     * @param ignoreSsl 是否忽略证书
     * @return
     * @throws IOException
     */
    public static String post(
            String url,
            JSONObject param,
            Map<String, String> headers,
            ContentType contentType,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout,
            boolean ignoreSsl)
            throws IOException {
        // 获取链接
        CloseableHttpClient client = null;
        if (ignoreSsl) {
            client = HttpClients.createDefault();
        } else {
            client = createSSLClientDefault();
        }
        // 参数封装
        HttpPost httpPost = new HttpPost(url);
        if (param != null) {
            httpPost.setEntity(new StringEntity(JSONObject.toJSONString(param), contentType));
        }
        // 设置超时时间
        httpPost.setConfig(
                getRequestConfig(connectTimeout, socketTimeout, connectionRequestTimeout));
        // 设置请求头
        setHeader(httpPost, headers);
        String respContent = null;
        CloseableHttpResponse resp = null;
        try {
            // LOGGER.info("post请求url和参数：{}：{}", url, param);
            resp = client.execute(httpPost);
            // LOGGER.info("post请求返回接口结果：{}：", resp);
            if (resp.getStatusLine().getStatusCode() == 200) {
                HttpEntity he = resp.getEntity();
                respContent = EntityUtils.toString(he, "UTF-8");
            }
        } catch (IOException e) {
            LOGGER.error("POST请求执行方法报错" + e);
        } finally {
            if (resp != null) {
                resp.close();
            }
            client.close();
        }
        return respContent;
    }

    public static String httpPost(
            String url,
            JSONObject param,
            Map<String, String> headers,
            ContentType contentType,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout)
            throws IOException {
        return post(
                url,
                param,
                headers,
                contentType,
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout,
                true);
    }

    public static String httpsPost(
            String url,
            JSONObject param,
            Map<String, String> headers,
            ContentType contentType,
            int connectTimeout,
            int socketTimeout,
            int connectionRequestTimeout)
            throws IOException {
        return post(
                url,
                param,
                headers,
                contentType,
                connectTimeout,
                socketTimeout,
                connectionRequestTimeout,
                false);
    }

    public static String httpsPost(
            String url, JSONObject param, Map<String, String> headers, ContentType contentType)
            throws IOException {
        return post(
                url,
                param,
                headers,
                contentType,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_SOCKET_TIMEOUT,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                false);
    }

    /**
     * 设置链接超时时间
     *
     * @param connectTimeout 链接超时时间
     * @param socketTimeout 网络超时时间
     * @param connectionRequestTimeout 链接请求超时时间
     * @return
     */
    private static RequestConfig getRequestConfig(
            int connectTimeout, int socketTimeout, int connectionRequestTimeout) {
        return RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
    }

    /**
     * get方法生成参数
     *
     * @param param
     * @return
     */
    private static String map2String(JSONObject param) {
        if (param != null && param.size() > 0) {
            StringBuilder params = new StringBuilder();
            for (Map.Entry<String, Object> entry : param.entrySet()) {
                params.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            return params.toString();
        } else {
            return "";
        }
    }

    private static String httpGetByEncode(Map<String, Object> map) {
        List<BasicNameValuePair> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                BasicNameValuePair nameValuePair =
                        new BasicNameValuePair(entry.getKey(), entry.getValue().toString());
                params.add(nameValuePair);
            }
        }
        String queryString = URLEncodedUtils.format(params, "UTF-8");

        if (StringUtils.isBlank(queryString)) {
            return "";
        } else {
            return queryString;
        }
    }

    /**
     * 设置请求头 标志浏览器
     *
     * @param http AbstractExecutionAwareRequest
     */
    private static void setHeader(AbstractExecutionAwareRequest http, Map<String, String> headers) {
        http.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        http.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        http.setHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/53.0.2785.104 Safari/537.36 Core/1.53.3357.400 QQBrowser/9.6.11858.400");
        http.setHeader("Accept-Encoding", "gzip, deflate, sdch");
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                http.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 证书
     *
     * @return
     */
    private static CloseableHttpClient createSSLClientDefault() {
        try {
            SSLContext sslcontext = createSSLContext();
            SSLConnectionSocketFactory sslsf =
                    new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);

            return HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return HttpClients.createDefault();
    }

    private static SSLContext createSSLContext() {
        SSLContext sslcontext = null;
        try {
            TrustManager[] trustManagers = new TrustManager[1];
            trustManagers[0] = new TrustAnyTrustManager();
            sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(new KeyManager[0], trustManagers, new SecureRandom());
            SSLContext.setDefault(sslcontext);
            sslcontext.init(null, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error("Create SSL Error", e);
        }
        return Objects.requireNonNull(sslcontext);
    }

    /**
     * 自定义私有类
     *
     * @author haow
     */
    static class TrustAnyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
