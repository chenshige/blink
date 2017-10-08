package com.wcc.wink.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** 连接处理类，主要提供OkHttpClient对象
 * Created by wenbiao.xie on 2016/6/8.
 */
public final class Connections {

    private final static int CONN_TIMEOUT = 30000;
    private final static int READ_TIMEOUT = 30000;

    private static Singleton<OkHttpClient> SINGLE_CLIENT = new Singleton<OkHttpClient>() {
        @Override
        protected OkHttpClient create() {
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(CONN_TIMEOUT, TimeUnit.MILLISECONDS);
            client.setReadTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
            supportHttps(client);
            return client;
        }
    };


    private static void supportHttps(OkHttpClient client) {
        try {
            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            SSLContext e = SSLContext.getInstance("TLS");
            e.init(null, new TrustManager[]{tm}, null);
            client.setSslSocketFactory(e.getSocketFactory());

        } catch (Exception e) {
            throw new RuntimeException("supportHttps failed", e);
        }
    }

    private static OkUrlFactory sFactory;
    public static HttpURLConnection openConnection(String url) throws MalformedURLException {
        if (sFactory == null) {
            sFactory = new OkUrlFactory(getOkHttpClient());
        }
        return sFactory.open(new URL(url));
    }

    public static OkHttpClient getOkHttpClient() {
        return SINGLE_CLIENT.get();
    }


}
