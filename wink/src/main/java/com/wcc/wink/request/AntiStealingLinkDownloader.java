package com.wcc.wink.request;

import android.text.TextUtils;
import android.webkit.CookieManager;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.wcc.wink.Resource;
import com.wcc.wink.Wink;
import com.wcc.wink.WinkError;
import com.wcc.wink.loader.UrlFetcher;
import com.wcc.wink.util.Connections;
import com.wcc.wink.util.Objects;
import com.wcc.wink.util.Streams;
import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wcc.wink.WinkError.NetworkError;

/**
 * 防盗链下载器类
 * Created by wenbiao.xie on 2015/11/7.
 */
public class AntiStealingLinkDownloader extends Downloader implements URLParams.URLParamsCreator<DownloadInfo> {
    private static final String TAG = "AntiStealingLinkDownloader";
    private static final int STAGE_START = 0;
    private static final int STAGE_FETCH_URL = 1;
    private static final int STAGE_FETCH_LENGTH = 2;
    private static final int STAGE_DOWNLOAD = 3;

    private Downloader mStub;
    private AtomicBoolean mCancelled = new AtomicBoolean(false);
    private final UrlFetcher<Resource> mUrlFetcher;
    private OkHttpClient mClient;
    private Call mCall;
    private AtomicInteger mStage = new AtomicInteger(STAGE_START);
    private boolean supportSegmental;
    private final boolean needCheckLength;

    protected AntiStealingLinkDownloader(WinkRequest task, UrlFetcher<Resource> fetcher) {
        super(task);
        mUrlFetcher = fetcher;
        mClient = Connections.getOkHttpClient();
        supportSegmental = (task.getEntity().downloaderType == Downloader.DOWNLOADER_SEGMENTED);
        needCheckLength = Wink.get().getSetting().isNeedCheckLengthBeforeDownload();
    }

    public boolean isCanceled() {
        return mCancelled.get();
    }

    @Override
    public int download() {
        WLog.v(TAG, "download");
        notifyDownloadEvent(EVENT_PREPARING, PREPARE_ACTION_FETCHURL);
        int err = NetworkError.CANCEL;

        do {
            if (isCanceled()) {
                break;
            }

            err = doFetch();
            if (err != NetworkError.SUCCESS)
                break;

            if (isCanceled())
                err = NetworkError.CANCEL;

        } while (false);

        if (err != NetworkError.SUCCESS) {
            onDownloadComplete(err);
            return err;
        }

        Downloader downloader = null;
        // 选择合适下载器进行下载
        if (mEntity.downloaderType == DOWNLOADER_UNKNOWN) {
            // 如果之前已经采用分段式下载，或者文件大小超过1M时，将采用分段式下载
            // 否则使用单线程下载

            if (supportSegmental && mEntity.getTotalSizeInBytes() > Utils.MEGA_BYTES) {
                downloader = new SegmentedDownloader(mTask, this);
            }
        } else if (alreadySegmented(mEntity)) {
            downloader = new SegmentedDownloader(mTask, this);
        }

        if (downloader == null)
            downloader = new OkURLDownloader(mTask, this);

        downloader.setOnDownloadListener(mOnDownloadListener);
        mStub = downloader;

        if (isCanceled()) {
            onDownloadComplete(NetworkError.CANCEL);
            mStub = null;
            return NetworkError.CANCEL;
        }

        return downloader.download();
    }

    private boolean alreadySegmented(DownloadInfo entity) {
        return (entity.downloaderType == DOWNLOADER_SEGMENTED);
    }

    private int doFetch() {
        WLog.v(TAG, "doFetch");
        int retryTimes = 1;
        int err = NetworkError.SUCCESS;
        boolean forceGotUrl = false;

        if (TextUtils.isEmpty(mEntity.getUrl()) && mEntity.getDownloadedSizeInBytes() > 0) {
            forceGotUrl = true;
        }

        do {

            if (mCancelled.get()) {
                err = NetworkError.CANCEL;
                break;
            }

            WLog.i(TAG, "try %d time", retryTimes);
            int stage = mStage.get();
            switch (stage) {
                case STAGE_START:
                    mStage.set(STAGE_FETCH_URL);
                case STAGE_FETCH_URL: {
                    try {
                        err = mUrlFetcher.load(mEntity.getResource(), forceGotUrl);
                        if (err == NetworkError.SUCCESS) {
                            mEntity.setUrl(mEntity.getResource().getUrl());
                            if (!Objects.equals(mEntity.getTitle(), mEntity.getResource().getTitle())) {
                                mEntity.setTitle(mEntity.getResource().getTitle());
                            }
                        }

                    } catch (InterruptedException e) {
                        err = NetworkError.CANCEL;

                    } catch (Exception e) {
                        WLog.printStackTrace(e);
                        err = NetworkError.FAIL_IO_ERROR;
                    }

                    if (err != NetworkError.SUCCESS) {
                        break;
                    }

                    mStage.set(STAGE_FETCH_LENGTH);
                }

                case STAGE_FETCH_LENGTH: {
                    if (!needCheckLength && mEntity.getTotalSizeInBytes() > 0) {
                        break;
                    }

                    try {
                        long total = fetchTotalLength(mEntity);
                        WLog.i(TAG, "fetch the total length: %d", total);
                        if (total > 0) {
                            supportSegmental = true;
                        }

                        if (total > 0 && mEntity.getTotalSizeInBytes() == 0) {
                            mEntity.setTotalSizeInBytes(total);
                        } else if (needCheckLength && total > 0 && mEntity.getTotalSizeInBytes() > 0
                                && mEntity.getTotalSizeInBytes() != total) {

                            if (!TextUtils.isEmpty(mEntity.getLocalFilePath())) {
                                File file = new File(mEntity.getLocalFilePath());
                                boolean exists = file.exists();
                                if (exists) {
                                    file.delete();
                                    mEntity.reset();
                                    mEntity.setTotalSizeInBytes(total);
                                }
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        WLog.printStackTrace(e);
                        err = NetworkError.SOCKET_TIMEOUT;
                    } catch (InterruptedIOException e) {
                        WLog.printStackTrace(e);
                        err = NetworkError.CANCEL;
                    } catch (NetworkIOException e) {
                        WLog.printStackTrace(e);
                        err = e.code();
                    } catch (IOException e) {
                        WLog.printStackTrace(e);
                        if ("Canceled".equals(e.getMessage()))
                            err = NetworkError.CANCEL;
                        else
                            err = NetworkError.FAIL_IO_ERROR;
                    }
                }
            }

            if (err != NetworkError.CANCEL && isCanceled()) {
                err = NetworkError.CANCEL;
            }

            if (err == NetworkError.SUCCESS || err == NetworkError.CANCEL) {
                break;
            }

            if (err == NetworkError.AUTH_EXPIRED) {
                forceGotUrl = true;
                mStage.set(STAGE_FETCH_URL);
            }

            retryTimes++;
        } while (retryTimes <= 3);

        if (err == NetworkError.SUCCESS) {
            mStage.set(STAGE_DOWNLOAD);
        }
        return err;
    }

    private Request createLengthRequest(String url, String referer) {
        long pos = 1;
        long len = 1;

        //封装请求
        Request.Builder builder = new Request.Builder()
                //下载地址
                .header("Range", "bytes=" + pos + "-"
                        + (pos + len - 1))
                .url(url);

        final String userAgent = Wink.get().getSetting().getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) {
            builder.header("User-Agent", userAgent);
        }
        if (!TextUtils.isEmpty(referer)) {
            builder.header("Referer", referer);
        }

        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(url);
            if (!TextUtils.isEmpty(cookie)) {
                WLog.i(TAG, "cookie: %s", cookie);
                builder.addHeader("cookie", cookie);
            }
        } catch (Exception e) {
            WLog.printStackTrace(e);
        }

        WLog.i(TAG, "create okhttp length request for url %s", url);
        return builder.build();
    }

    private int codeToNetworkError(int code) {
        return WinkError.toNetworkErrorWithHttpStatus(code);
    }

    private long fetchTotalLength(DownloadInfo info) throws IOException {

        String referer = "";
        String mimeType = null;
        if (mEntity.getResource() instanceof SimpleURLResource) {
            referer = ((SimpleURLResource) mEntity.getResource()).getReferer();
            mimeType = ((SimpleURLResource) mEntity.getResource()).getMimeType();
        }

        Request request = createLengthRequest(info.getUrl(), referer);
        final Call call = mClient.newCall(request);

        if (isCanceled()) {
            throw new NetworkIOException("Canceled", WinkError.NetworkError.CANCEL);
        }

        mCall = call;
        Response response = null;
        try {
            response = call.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCall = null;
        try {
            WLog.i(TAG, "fetchTotalLength result: %d", response.code());
            if (!response.isSuccessful()) {
                throw new NetworkIOException("fetchTotalLength failed!", codeToNetworkError(response.code()));
            }

            String mime = response.header("Content-Type", null);
            WLog.i(TAG, "mime: %s", mime);
            if (TextUtils.isEmpty(mimeType) || mimeType.equals("application/octet-stream")) {
                if (mEntity.getResource() instanceof SimpleURLResource && !TextUtils.isEmpty(mime)) {
                    ((SimpleURLResource) mEntity.getResource()).setMimeType(mime);
                }
            } else {
                if (mEntity.getResource() instanceof SimpleURLResource) {
                    ((SimpleURLResource) mEntity.getResource()).setMimeType(mimeType);
                }
            }

            String range = response.header("Content-Range", null);
            if (TextUtils.isEmpty(range)) {
                WLog.w(TAG, "not support ranges");
                return -1;
            }
            WLog.d(TAG, "Content-Range: %s", range);
            int index = range.lastIndexOf('/');
            if (index >= 0) {
                return Long.parseLong(range.substring(index + 1));
            }

            return -1;
        } finally {
            Streams.safeClose(response.body());
        }
    }

    @Override
    public void cancel(boolean forDelete) {
        WLog.v(TAG, "cancel for delete: %b", forDelete);
        if (isCanceled())
            return;

        mCancelled.set(true);
        int stage = mStage.get();
        switch (stage) {
            case STAGE_FETCH_URL:
                mUrlFetcher.cancel();
                break;
            case STAGE_FETCH_LENGTH:
                if (mCall != null && !mCall.isCanceled())
                    mCall.cancel();
                break;
            case STAGE_DOWNLOAD:
                if (mStub != null) {
                    mStub.cancel(forDelete);
                }
                break;
        }
    }

    @Override
    public URLParams<DownloadInfo> createUrlParams(DownloadInfo info) {

        if (TextUtils.isEmpty(info.getUrl()))
            throw new IllegalStateException("fuck you, url empty!!");

        URLParams<DownloadInfo> params = new URLParams<>();
        params.url = info.getUrl();
        String localFilePath = info.getLocalFilePath();
        if (TextUtils.isEmpty(info.getLocalFilePath()) || !Utils.isExist(info.getLocalFilePath())) {
            params.target = info.getLoader().getTargetFile(info.getResource(), info);
        } else if (localFilePath.endsWith(Utils.TEMP_SUFFIX)) {
            params.target = new File(localFilePath.substring(0, localFilePath.length() - Utils.TEMP_SUFFIX.length()));
        } else {
            params.target = new File(localFilePath);
        }

        params.entity = info;
        return params;
    }
}
