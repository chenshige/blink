// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adblock;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.UrlUtils;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.WebAddress;


import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class AdBlock {
    // Load library.
//    static {
//        System.loadLibrary("adblock");
//    }

    // resource type consistent with native blocking_rule_option.h
    private static final int   BROI_SCRIPT           = 0;
    private static final int   BROI_IMAGE            = 1;
    private static final int   BROI_STYLESHEET       = 2;
    private static final int   BROI_OBJECT           = 3;
    private static final int   BROI_XMLHTTPREQUEST   = 4;
    private static final int   BROI_OBJECTSUBREQUEST = 5;
    private static final int   BROI_SUBDOCUMENT      = 6;
    private static final int   BROI_DOCUMENT   = 7;
    private static final int   BROI_ELEMHIDE   = 8;
    private static final int   BROI_OTHER      = 9;
    private static final int   BROI_THIRDPARTY = 10;


    public static final String LOGTAG = AdBlock.class.getSimpleName();
    private static final long INIT_DELAY = 300L; // delay 300 milliseconds.
    private static final int MATCH_TIMEOUT = 5; // 5 milliseconds.


    private static AdBlock sInstance;

    // Pointer to native side instance.
    private long mNativeAdBlock = 0L;

    private Handler mHandler = new Handler();
    private static ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private AtomicInteger mAtomic;

    private boolean mAdBlockEnabled = false;
    private String mMainUrl = "";
    private WebResourceResponse mMatchResult = null;
    private boolean mInitCompleted = false;
    private boolean mMatchCompleted = true;
    private boolean mNewPage = true;
    private boolean mDestroy = false;

    public static AdBlock getInstance() {
        if (sInstance == null) {
            sInstance = new AdBlock();
        }
        return sInstance;
    }

    private AdBlock() {
        mAtomic = new AtomicInteger();
//        mNativeAdBlock = nativeCreateAdBlock();
        ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
        mMatchResult = new WebResourceResponse("text/plain", "utf-8", EMPTY);
        mAdBlockEnabled = BrowserSettings.getInstance().getAdBlockEnabled();
    }

    public void initAdBlock(final Context context) {
        // #1 Init AdBlockDataUpdator;
        // #2 Create native instance;
        // #3 Check whether update local data.
        if (mAtomic.getAndIncrement() != 0) return;
        mInitCompleted = false;
        mDestroy = false;
        AdBlockDataUpdator.getInstance().init(context);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                String dir = AdBlockDataUpdator.getInstance().getAdblockDirPath();
                List<String> files = FileUtils.getAllFiles(dir);
                for (int index = 0; index < files.size(); index++) {
                    if (FileUtils.fileIsExists(files.get(index))) {
//                        nativeInitAdBlock(files.get(index));
                    }
                }
                if (mDestroy) {
//                    nativeDestroyAdBlock(mNativeAdBlock);
                    mNativeAdBlock = 0L;
                } else {
                    mInitCompleted = true;
//                    nativeInitComplete();
                }
            }
        }, INIT_DELAY);
    }

    public void destroyAdBlock() {
        if (mAtomic.decrementAndGet() == 0) return;
        if (!mInitCompleted || mDestroy) {
            mDestroy = true;
            return;
        }
        mInitCompleted = false;
//        nativeDestroyAdBlock(mNativeAdBlock);
        mNativeAdBlock = 0L;
    }

    public void setAdBlockEnabled(boolean enabled) {
        mAdBlockEnabled = enabled;
    }

    public void onPageStarted(String url) {
        if (!mMainUrl.equals(url)) {
            mNewPage = true;
        }
        mMainUrl = url;
    }

    public void onPageFinished(WebView view, String url) {
        if (!mInitCompleted || !mAdBlockEnabled) {
            return;
        }
        injectCSS(view, url);
    }

    public WebResourceResponse shouldInterceptRequest(final Context context, final String url) {
        WebResourceResponse response = shouldInterceptRequest(url);
        if (response != null && !mAdBlockEnabled) {
            return null;
        }
        return response;
    }
    public boolean shouldDisableJs(String url) {
        List<String> hosts = AdBlockDataUpdator.getInstance().readDisableJsHosts();
        for (String host : hosts) {
            if (UrlUtils.checkUrlBelongToHost(url, host)) {
                return true;
            }
        }
        return false;
    }

    private WebResourceResponse shouldInterceptRequest(final String url) {
        // If the last match has not completed, only return null, that means AdBlock
        // will ignore some ads. I think that is OK.
        if (!mInitCompleted || !mMatchCompleted) return null;
        if (!mAdBlockEnabled) return null;

        mMatchCompleted = false;

        Future<Boolean> future = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                String host = "";
                try {
                    WebAddress webAddress = new WebAddress(url);
                    host = webAddress.getHost();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (host.isEmpty()) return false;
                return false;
//                return nativeBlockRequest(url, host, 1, getResourceType(url));
            }
        });

        // 5 ms is timeout for native match operation.
        boolean match = false;
        try {
            //while (!future.isDone());
            match = future.get(MATCH_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            match = false;
        }

        mMatchCompleted = true;

        return match ? mMatchResult : null;
    }

    private void injectCSS(WebView webView, String url) {
        String host = "";
        try {
            WebAddress webAddress = new WebAddress(url);
            host = webAddress.getHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (host.isEmpty()) return;

        String[] result = nativeMatchHost(host);
        if (result == null || result.length == 0) return;

        StringBuilder css = new StringBuilder("");
        for (int i = 0; i < result.length; i++) {
            css.append(result[i] + " { display:none !important; } ");
        }

        webView.loadUrl("javascript:(function() {" +
                "var parent = document.getElementsByTagName('head')[0];" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" + css.toString() + "';" +
                "parent.appendChild(style);" +
                "})();");
    }

    private int getResourceType(String url) {
        if (mMainUrl.equals(url)) return BROI_DOCUMENT;
        String extension = FileUtils.getFileExtensionFromUrl(url);
        if (TextUtils.isEmpty(extension)) return BROI_OTHER;
        if(extension.equalsIgnoreCase("jpg") ||
                extension.equalsIgnoreCase("webp") ||
                extension.equalsIgnoreCase("bmp") ||
                extension.equalsIgnoreCase("png") ||
                extension.equalsIgnoreCase("ico") ||
                extension.equalsIgnoreCase("jpeg") ||
                extension.equalsIgnoreCase("gif")) {
            return BROI_IMAGE;
        }

        if (extension.equalsIgnoreCase("css")) return BROI_STYLESHEET;
        if (extension.equalsIgnoreCase("js")) return BROI_SCRIPT;
        if (extension.equalsIgnoreCase("html")) return BROI_SUBDOCUMENT;
        return BROI_OTHER;
    }

    private static native long nativeCreateAdBlock();

    private static native void nativeInitAdBlock(String filePath);

    private static native void nativeDestroyAdBlock(long nativeAdBlock);

    private static native boolean nativeBlockRequest(String url, String host, int routeId, int resourceType);

    private static native String[] nativeMatchHost(String host);
    private static native void nativeInitComplete();

}
