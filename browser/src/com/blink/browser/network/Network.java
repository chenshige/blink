// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.blink.browser.util.NetworkUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class Network {
    private static final String LOGTAG = Network.class.getSimpleName();

    /**
     * User agent for mobile.
     */
    private static final String DEFAULT_USERAGENT = "Mozilla/5.0 (Linux; U;" +
            " Android 2.2; zh-cn;) AppleWebKit/533.1 (KHTML, " +
            "like Gecko) Version/4.0 Mobile Safari/533.1";

    private static final String ENCODING_GZIP = "gzip";

    public static String downloadXml(Context context, String url) throws IOException {
        return downloadXml(context, new URL(url), true, null, null, null);
    }

    public static String downloadXml(Context context, URL url, boolean noEncryptUrl, String userAgent,
            String encoding, String cookie) throws IOException {
        InputStream responseStream = null;
        BufferedReader reader = null;
        StringBuilder sbReponse = null;
        try {
            responseStream = downloadXmlAsStream(context, url, noEncryptUrl, userAgent, cookie, null, null);
            sbReponse = new StringBuilder(1024);
            reader = new BufferedReader(new InputStreamReader(responseStream, encoding));
            String line;
            while (null != (line = reader.readLine())) {
                sbReponse.append(line);
                sbReponse.append("\r\n");
            }
        } finally {
            if (null != responseStream) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sbReponse == null ? null : sbReponse.toString();
    }

    public static InputStream downloadXmlAsStream(Context context, String url) throws IOException {
        return downloadXmlAsStream(context, new URL(url), true, null, null, null, null);
    }

    public static InputStream downloadXmlAsStream(Context context, String url, boolean noEncryptUrl,
            String userAgent, String cookie) throws IOException {
        return downloadXmlAsStream(context, new URL(url), noEncryptUrl, userAgent, cookie, null, null);
    }

    /**
     * 包装 HTTP request/response 的辅助函数
     *
     * @param context 应用程序上下文
     * @param url HTTP地址
     * @param noEncryptUrl 是否加密
     * @param userAgent
     * @param cookie
     * @param requestHdrs 用于传入除userAgent和cookie之外的其他header info
     * @param responseHdrs 返回的HTTP response headers;
     * @return
     * @throws IOException
     */
    private static InputStream downloadXmlAsStream(
            /* in */ Context context,
            /* in */ URL url, boolean noEncryptUrl, String userAgent, String cookie, Map<String, String> requestHdrs,
            /* out */ HttpHeaderInfo responseHdrs) throws IOException {
        if (null == context) throw new IllegalArgumentException("context");
        if (null == url) throw new IllegalArgumentException("url");

        URL newUrl = url;
        //if (!noEncryptUrl) {
        //    newUrl = new URL(encryptURL(url.toString()));
        //}

        InputStream responseStream = null;
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection conn = getProxiableURLConnection(context, newUrl);
        conn.setConnectTimeout(getSocketTimeout(context, newUrl.toString()));
        conn.setReadTimeout(getSocketTimeout(context, newUrl.toString()));

        if (!TextUtils.isEmpty(userAgent)) {
            conn.setRequestProperty("User-agent", userAgent);
        }
        if (cookie != null) {
            conn.setRequestProperty("Cookie", cookie);
        }
        if (null != requestHdrs) {
            for (String key: requestHdrs.keySet()) {
                conn.setRequestProperty(key, requestHdrs.get(key));
            }
        }

        if (responseHdrs != null &&
                (url.getProtocol().equals("http") || url.getProtocol().equals("https"))) {
            responseHdrs.mResponseCode = conn.getResponseCode();
            responseHdrs.mContentType = conn.getContentType();
            if (responseHdrs.mAllHeaders == null) {
                responseHdrs.mAllHeaders = new ArrayMap<>();
            }
            for (int i = 0; ; i++) {
                String name = conn.getHeaderFieldKey(i);
                String value = conn.getHeaderField(i);

                if (name == null && value == null) break;
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(value)) continue;

                responseHdrs.mAllHeaders.put(name.toLowerCase(), value);
            }
        }

        responseStream = conn.getInputStream();
        return responseStream;
    }

    public static class HttpHeaderInfo {
        public int mResponseCode;
        public String mContentType;
        public String mUserAgent;
        public String mRealUrl;
        public Map<String, String> mAllHeaders;
    }

    /**
     * 向服务端提交HttpGet请求
     * 设置为5秒钟连接超时，发送数据不超时；
     *
     * @param context : Context
     * @param url: HTTP get的URL地址
     * @param nameValuePairs: HTTP jquery 参数
     * @noGZip gzip: whether response Accept-Encoding: gzip
     * @return: 如果get response代码不是2xx，表示发生了错误，返回null。否则返回服务器返回的数据（如果服务器没有返回任何数据，返回""）；
     * @throws IOException: 调用过程中可能抛出到exception
     */
    public static String doHttpGet(Context context, String url, List<NameValuePair> nameValuePairs, boolean gzip) throws IOException {
        BufferedReader reader = null;
        InputStream responseStream = null;
        StringBuilder sbReponse = null;
        HttpURLConnection conn = null;
        try {
            String params = null;
            String newUrl = url;
            if (nameValuePairs != null && nameValuePairs.size() > 0) {
                params = URLEncodedUtils.format(nameValuePairs, "UTF-8");
                newUrl = url + "?" + params;
            }
            Map<String, String> headers = new ArrayMap<>();
            HttpURLConnection.setFollowRedirects(true);
            conn = getProxiableURLConnection(context, new URL(newUrl));
            conn.setConnectTimeout(getSocketTimeout(context, null));
            conn.setReadTimeout(getSocketTimeout(context, null));
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "text/xml");
            // conn.setRequestProperty("User-agent", DEFAULT_USERAGENT);
            if (gzip) {
                conn.setRequestProperty("Accept-Encoding", ENCODING_GZIP);
            }

            for (int i = 0; ; i++) {
                String name = conn.getHeaderFieldKey(i);
                String value = conn.getHeaderField(i);
                if (name == null && value == null) {
                    break;
                }
                headers.put(name, value);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                responseStream = conn.getInputStream();
            } else {
                responseStream = conn.getErrorStream();
            }
            if (gzip || headers.containsKey("Content-Encoding")
                    && headers.get("Content-Encoding").toLowerCase().equals(ENCODING_GZIP)) {
                responseStream = new GZIPInputStream(responseStream);
            }

            sbReponse = new StringBuilder(1024);
            reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
            String line;
            while (null != (line = reader.readLine())) {
                android.util.Log.e("fjs", " " + line);
                sbReponse.append(line);
                sbReponse.append("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (responseStream != null) {
                    responseStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return sbReponse == null ? null : sbReponse.toString();
    }

    private static int getSocketTimeout(Context context, String url) {
        if (TextUtils.isEmpty(url) || (url.indexOf("wap") == -1)) {
            return 8000; // ms
        }
        return 15000;  // ms
    }

    /**
     * @param strUrl 要加密的URL string
     * @return 获取加密后的URL string
     */
    private static String encryptURL(String strUrl) {
        if(!TextUtils.isEmpty(strUrl)) {
            // TODO:
        }
        return strUrl;
    }

    /**
     * 实现自适应的 HttpURLConnection
     *
     * @param url
     * @return
     * @throws IOException
     * @throws MalformedURLException
     * @throws Exception
     */
    public static HttpURLConnection getProxiableURLConnection(Context context, URL url) throws IOException {
        if (url.getProtocol().toLowerCase().equals("https")) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1", "AndroidOpenSSL");
                sc.init(null, new X509TrustManager[] { new X509TrustManager () {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[] { };
                        }
                    }
                }, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
            HttpsURLConnection http = (HttpsURLConnection) url.openConnection();
            http.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            }); // 不进行主机名确认
            return http;
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }

    public interface PostDownloadHandler {
        void OnPostDownload(boolean success);
    }

    /**
     * 实际的网络流交由具体的业务处理
     */
    public interface OutsourceHandler {
        boolean outsource(InputStream input);
    }

    /**
     * 开始下载远程文件到指定输出流
     * @param url 远程文件地址
     * @param output 输出流
     * @param handler 下载成功或者失败的处理
     */
    public static void downloadFile(Context context, String url, OutputStream output,
            PostDownloadHandler handler) {
        DownloadTask task = new DownloadTask(context, url, output, handler);
        task.execute();
    }

    /**
     * 下载远程文件到指定输出流
     * @param url 远程文件地址
     * @param output stream
     * @return  success or failed
     */
    public static boolean downloadFile(Context context, String url, OutputStream output) {
        InputStream input = null;
        try {
            HttpURLConnection conn = getProxiableURLConnection(context, new URL(url));
            conn.setConnectTimeout(getSocketTimeout(context, url));
            conn.setReadTimeout(getSocketTimeout(context, url));
            input = conn.getInputStream();

            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 只做网络请求，拿到流数据之后，交由具体的业务处理
     * 针对图像部分
     * @param context
     * @param url
     * @param handler
     */
    public static boolean downloadFile(Context context, String url,OutsourceHandler handler){
        InputStream input = null;
        try {
            HttpURLConnection conn = getProxiableURLConnection(context, new URL(url));
            conn.setConnectTimeout(getSocketTimeout(context, url));
            conn.setReadTimeout(getSocketTimeout(context, url));
            input = conn.getInputStream();
            return handler.outsource(input);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false ;
    }

    public static Bitmap DownloadImage(Context context, String url) {
        InputStream input = null;
        try {
            HttpURLConnection conn = getProxiableURLConnection(context, new URL(url));
            conn.setConnectTimeout(getSocketTimeout(context, url));
            conn.setReadTimeout(getSocketTimeout(context, url));
            input = conn.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static class DownloadTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext;
        private String mUrl;
        private OutputStream mOutput;
        private PostDownloadHandler mHandler;

        public DownloadTask(Context context, String url, OutputStream output, PostDownloadHandler handler) {
            mContext = context;
            mUrl = url;
            mOutput = output;
            mHandler = handler;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = Network.downloadFile(mContext, mUrl, mOutput);
            if (mOutput != null) {
                try {
                    mOutput.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mHandler != null) {
                mHandler.OnPostDownload(result);
            }
        }
    }

    private HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    private void trustAllHosts() {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1");
            sc.init(null, mTrustManager, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(DO_NOT_VERIFY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    X509TrustManager myX509TrustManager = new X509TrustManager () {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] { };
        }
    };
    X509TrustManager[] mTrustManager = new X509TrustManager[] { myX509TrustManager };

    private HostnameVerifier hnv = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            return hv.verify(hostname, session);
        }
    };
}
