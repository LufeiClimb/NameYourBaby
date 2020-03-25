package com.namebaby.utils;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HttpUtil
 *
 * @author LufeiClimb
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
            // LOGGER.error("POST请求执行方法报错" + e);
        } finally {
            if (resp != null) {
                resp.close();
            }
            client.close();
        }
        return respContent;
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

    public static String httpForm(String httpUrl, JSONObject param) {
        String formResult = null;
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        BufferedReader br = null;
        try {
            URL url = new URL(httpUrl);
            // 通过远程url连接对象打开连接
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接请求方式
            connection.setRequestMethod("POST");
            // 设置连接主机服务器超时时间：15000毫秒
            connection.setConnectTimeout(15000);
            // 设置读取主机服务器返回数据超时时间：60000毫秒
            connection.setReadTimeout(60000);

            // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
            connection.setDoOutput(true);
            // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // 设置鉴权信息：Authorization: Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0
            // connection.setRequestProperty("Authorization", "Bearer
            // da3efcbf-0845-4fe3-8aba-ee040be542c0");
            // 通过连接对象获取一个输出流
            os = connection.getOutputStream();
            // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的(form表单形式的参数实质也是key,value值的拼接，类似于get请求参数的拼接)
            os.write(createLinkString(param).getBytes());
            // 通过连接对象获取一个输入流，向远程读取
            if (connection.getResponseCode() == 200) {

                is = connection.getInputStream();
                // 对输入流对象进行包装:charset根据工作项目组的要求来设置
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                StringBuffer sbf = new StringBuffer();
                String temp = null;
                // 循环遍历一行一行读取数据
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                formResult = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 断开与远程地址url的连接
            connection.disconnect();
        }
        return formResult;
    }

    public static String createLinkString(JSONObject params) {

        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);

        StringBuilder prestr = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.getString(key);
            if (i == keys.size() - 1) { // 拼接时，不包括最后一个&字符
                prestr.append(key).append("=").append(value);
            } else {
                prestr.append(key).append("=").append(value).append("&");
            }
        }

        return prestr.toString();
    }

}
