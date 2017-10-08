package com.wcc.wink.request;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieManager;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.wcc.wink.Resource;
import com.wcc.wink.Wink;
import com.wcc.wink.WinkError;
import com.wcc.wink.util.BufferedRandomIO;
import com.wcc.wink.util.Connections;
import com.wcc.wink.util.FileUtils;
import com.wcc.wink.util.Streams;
import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wcc.wink.WinkError.NetworkError;

/**
 * Created by wenbiao.xie on 2015/11/11.
 */
public class OkURLDownloader extends Downloader implements URLParams.URLParamsCreator<DownloadInfo> {

    private final static String TAG = "OkURLDownloader";
    private final static int BUFF_LEN = 4096;

    private URLParams<DownloadInfo> mParams = null;
    private volatile boolean mIsRunning = false;
    private final Object mLock = new Object();
    private Thread mDownloadThread;
    private OkHttpClient mClient;
    private Call mCall;
    private ContentHandler mContentHandler;
    private AtomicBoolean mCancelled = new AtomicBoolean(false);
    private final Object mResultLock = new Object();
    private boolean mResultGot = false;
    private volatile int mResultError;

    protected OkURLDownloader(WinkRequest task) {
        this(task, null);
    }

    protected OkURLDownloader(WinkRequest task, URLParams.URLParamsCreator<DownloadInfo> creator) {
        super(task);
        mClient = Connections.getOkHttpClient();
        mContentHandler = new ResumableContentHandler(task.getContext());
        if (creator != null)
            this.mParams = creator.createUrlParams(task.getEntity());
        else {
            this.mParams = createUrlParams(task.getEntity());
        }

        mEntity.downloaderType = DOWNLOADER_WHOLE_SINGLE;
    }

    public boolean isCanceled() {
        return mCancelled.get();
    }

    public URLParams<DownloadInfo> createUrlParams(DownloadInfo info) {
        if (TextUtils.isEmpty(info.getUrl()))
            throw new IllegalStateException("fuck you, url empty!!");

        URLParams<DownloadInfo> params = new URLParams<>();

        params.url = info.getUrl();
        String localFilePath = info.getLocalFilePath();
        if (TextUtils.isEmpty(localFilePath) || !Utils.isExist(localFilePath)) {
            params.target = info.getLoader().getTargetFile(info.getResource(), info);
        } else if (localFilePath.endsWith(Utils.TEMP_SUFFIX)) {
            params.target = new File(localFilePath.substring(0, localFilePath.length() -
                    Utils.TEMP_SUFFIX.length()));
        } else {
            params.target = new File(localFilePath);
        }

        params.entity = info;
        return params;
    }

    private Request createRequest(URLParams<DownloadInfo> params) {
        Map<String, String> headers = null;
        long pos = params.entity.getDownloadedSizeInBytes();
        long len = params.entity.getTotalSizeInBytes();
        String referer = "";
        if (params.entity.getResource() instanceof SimpleURLResource) {
            referer = ((SimpleURLResource) params.entity.getResource()).getReferer();
        }
        // 进行断点续传的设置
        if (len > 0) {
            headers = new HashMap<>();
            headers.put("RANGE", "bytes=" + pos + "-"
                    + (pos + len - 1));

        } else if (pos > 0) {
            headers = new HashMap<>();
            headers.put("RANGE", "bytes=" + pos
                    + "-");
        }

        if (!TextUtils.isEmpty(referer) && headers != null) {
            headers.put("Referer", referer);
        }

        //封装请求
        Request.Builder builder = new Request.Builder()
                //下载地址
//                .header("User-Agent", ACContext.getUA())
                .url(params.url);

        if (headers != null) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        final String userAgent = Wink.get().getSetting().getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) {
            builder.header("User-Agent", userAgent);
        }

        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(params.url);
            if (!TextUtils.isEmpty(cookie)) {
                WLog.i(TAG, "cookie: %s", cookie);
                builder.addHeader("cookie", cookie);
            }
        } catch (Exception e) {
        }

        WLog.i(TAG, "create okhttp request for url %s", params.url);

        return builder.build();
    }

    static void deleteFile(File target) {
        File file = target;
        boolean exists = file.exists();
        if (!exists) {
            file = Utils.getTempFile(file);
            exists = file.exists();
        }

        if (exists) {
            file.delete();
        }
    }

    static boolean checkEntity(DownloadInfo entity, File target) throws IOException {
        // 检查是否已经下载完成
        if (entity.getDownloadedSizeInBytes() == entity.getTotalSizeInBytes()
                && entity.getTotalSizeInBytes() > 0) {
            File file = target;
            boolean exists = file.exists();
            boolean cached = false;
            // 如果目标文件不存在，检测缓存文件是否存在
            if (!exists) {
                file = Utils.getTempFile(file);
                exists = file.exists();
                cached = true;
            }

            if (!exists || file.length() != entity.getTotalSizeInBytes()) {
                if (exists)
                    file.delete();

                entity.reset();
                return true;
            } else if (cached) {
                file.renameTo(target);
            }

            throw new NetworkIOException("already completed!", NetworkError.SUCCESS);
        }
        // 如果文件未下完，且下载长度大于总长度
        else if (entity.getDownloadedSizeInBytes() > entity.getTotalSizeInBytes()) {
            File file = target;
            boolean exists = file.exists();
            if (!exists) {
                file = Utils.getTempFile(target);
                exists = file.exists();
            }

            if (exists && file.length() != entity.getDownloadedSizeInBytes()) {
                file.delete();
                entity.reset();
                return true;
            } else if (!exists) {
                entity.reset();
                return true;
            }
        }
        // 如果检查缓存文件的存在性与有效性
        else {
            File file = Utils.getTempFile(target);
            if (file.exists()) {
                // 修正进度，以实际大小为准
                if (entity.getDownloadedSizeInBytes() != file.length()) {
                    entity.setDownloadedSizeInBytes(file.length());
                    return true;
                }

            } else if (entity.getDownloadedSizeInBytes() != 0) {
                entity.reset();
                return true;
            }
        }

        return false;
    }

    @Override
    public int download() {
        mDownloadThread = Thread.currentThread();
        mIsRunning = true;

        int err;
        final DownloadInfo entity = mEntity;
        final URLParams<DownloadInfo> params = mParams;
        WLog.i(TAG, "download start...");
        do {
            if (isCanceled()) {
                err = WinkError.NetworkError.CANCEL;
                break;
            }

            // 检查是否已经下载完成
            try {
                checkEntity(entity, params.target);
            } catch (NetworkIOException e) {
                WLog.printStackTrace(e);
                err = NetworkError.SUCCESS;
                break;
            } catch (IOException e) {
                WLog.printStackTrace(e);
                err = NetworkError.FAIL_IO_ERROR;
                break;
            }

            if (isCanceled()) {
                err = WinkError.NetworkError.CANCEL;
                break;
            }

            onPrepare();
            try {
                WLog.i(TAG, "downloading...");
                Request request = createRequest(params);
                mCall = mClient.newCall(request);

                if (isCanceled()) {
                    err = NetworkError.CANCEL;
                    break;
                }

                onStart();
                mCall.enqueue(new URLCallback(this, entity.getDownloadedSizeInBytes()));
                err = waitResult();

                if (err == NetworkError.AUTH_EXPIRED) {
                    mEntity.setUrl(null);
                } else if (err == NetworkError.RANGE_INVALID) {
                    mEntity.reset();
                    deleteFile(params.target);
                }

            } catch (Exception e) {
                WLog.printStackTrace(e);
                err = NetworkError.FAIL_UNKNOWN;
            }

        } while (false);

        if (err != NetworkError.SUCCESS &&
                err != NetworkError.CANCEL && isCanceled()) {
            err = NetworkError.CANCEL;
        }

        onDownloadComplete(err);
        mDownloadThread = null;
        synchronized (mLock) {
            mIsRunning = false;
            mLock.notify();
        }

        WLog.i(TAG, "download completed, error = %d", err);
        return err;
    }

    @Override
    public void cancel(boolean fordelete) {
        WLog.v(TAG, "cancel");
        if (isCanceled())
            return;

        mCancelled.set(true);
        final Call call = mCall;
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }

        final Thread thread = mDownloadThread;
        if (thread != null) {
            thread.interrupt();
        }

        if (fordelete) {
            waitDownloadStop();
            File cacheFile = Utils.getTempFile(mParams.target);
            if (cacheFile.exists()) {
                cacheFile.delete();
            } else if (mParams.target.exists()) {
                mParams.target.delete();
            }
        }
    }

    private void waitDownloadStop() {
        synchronized (mLock) {
            while (mIsRunning) {
                try {
                    mLock.wait(20);
                } catch (InterruptedException e) {
                    WLog.printStackTrace(e);
                    break;
                }
            }
        }
    }

    private void notifyResult(int err) {
        synchronized (mResultLock) {
            mResultError = err;
            mResultGot = true;
            mResultLock.notify();
        }
    }

    private int waitResult() {
        synchronized (mResultLock) {
            while (!mResultGot) {
                try {
                    mResultLock.wait(20);
                } catch (InterruptedException e) {
                    return NetworkError.CANCEL;
                }
            }
        }

        return mResultError;
    }

    private void onStartResponse(int err, Bundle params) {
        final WinkRequest task = mTask;
        if (err == NetworkError.SUCCESS) {
            task.getDownloadStat().onDownloadResponse();
        } else {
            task.getDownloadStat().onErrorHappened(err, params);
        }
    }

    private class ResumableContentHandler implements ContentHandler {

        private final Context mContext;
        File mCacheFile;
        //        FileOutputStream mFileOutputStream;
//        FileOutputStream mFileOutputStream;
//        BufferedOutputStream mBufferedOutputStream;

        long mCurrentLength = 0;
        long mTotalLength = 0;
        RandomAccessFile mAccessFile;

        int oldProgress = -1;

        public ResumableContentHandler(Context context) {
            this.mContext = context;
        }

        protected void onAdvance(int progress) {
            if (oldProgress == progress)
                return;

            WLog.i(TAG, "download bytes, progress: %d%%, current: %d, total: %d",
                    progress, mCurrentLength, mTotalLength);
            oldProgress = progress;
            notifyDownloadEvent(EVENT_PROGRESS, progress);
        }

        @Override
        public void complete(int cause, int curpos) {
            WLog.v(TAG, "complete cause = %d, length = %d", cause, curpos);
            if (mAccessFile != null) {
                Streams.safeClose(mAccessFile);
                mAccessFile = null;
            }

            if (cause == NetworkError.SUCCESS) {
                if (mEntity.getTotalSizeInBytes() < mEntity.getDownloadedSizeInBytes()) {
                    mEntity.setTotalSizeInBytes(mEntity.getDownloadedSizeInBytes());
                }
                try {
                    Uri apkpath = Uri.withAppendedPath(Uri.fromFile(new File(Wink.get().getSetting()
                            .getSimpleResourceStorageDirectory().getAbsolutePath())), mEntity.getTitle());
                    if (FileUtils.fileIsExists(apkpath.getPath())) {
                        FileUtils.checkFile(mContext, mParams.entity.getTitle(), 1, mCacheFile, mEntity);
                    } else {
                        boolean ret = mCacheFile.renameTo(new File(apkpath.getPath()));
                        mEntity.setLocalFilePath(apkpath.getPath());
                        Resource res = mEntity.getResource();
                        if (res instanceof SimpleURLResource) {
                            String mime = ((SimpleURLResource) res).getMimeType();
                            if ("application/vnd.android.package-archive".equalsIgnoreCase(mime)) {
                                FileUtils.apkInfo(mContext, mEntity);
                            }
                        }
                        mEntity.setTitle(mEntity.getTitle());

                        WLog.i("", "cache file renameTo %s, result:%b, title:%s", mEntity.getLocalFilePath(), ret,
                                mEntity.getTitle());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean handle(byte[] datas, int offset, int len) {
            if (isCanceled()) {
                return false;
            }

            if (mAccessFile != null && len > 0) {
                try {
                    mCurrentLength += len;
                    /*if (mCurrentLength > mTotalLength) {
                        mEntity.reset();
                        mCacheFile.delete();
                        return false;
                    }*/

                    mAccessFile.write(datas, offset, len);
                    mEntity.setDownloadedSizeInBytes(mCurrentLength);
                    mTask.receiveBytes(len);

                    if (mTotalLength <= 0) mTotalLength = mCurrentLength;

                    int progress = (int) (mCurrentLength * 100L / mTotalLength);
                    if (progress > 100) {
                        progress = 100;
                        mEntity.setTotalSizeInBytes(mCurrentLength);
                    }
                    mEntity.setDownloadProgress(progress);
                    onAdvance(progress);
                } catch (IOException e) {
                    WLog.printStackTrace(e);
                    Streams.safeClose(mAccessFile);
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean prepare(long curpos, long total) {
            WLog.v(TAG, "prepare total = %d, curpos = %d", total, curpos);
            if (total <= 0) {
//                if (mEntity.getTotalSizeInBytes() <= 0)
//                    return false;
//
//                total = mEntity.getTotalSizeInBytes();
            }

            final String path = mEntity.getLocalFilePath();
            if (!TextUtils.isEmpty(path)) {
                mCacheFile = new File(path);
            } else {
                mCacheFile = Utils.getTempFile(mParams.target);
                mEntity.setLocalFilePath(mCacheFile.getAbsolutePath());
            }

            if (!mCacheFile.exists()) {
                try {
                    Utils.create(mCacheFile);
                } catch (Exception e) {
                    WLog.printStackTrace(e);
                    return false;
                }
            } else if (mCacheFile.length() != curpos) {
                mCacheFile.delete();
                try {
                    Utils.create(mCacheFile);
                } catch (Exception e) {
                    WLog.printStackTrace(e);
                    return false;
                }
            }

            FileOutputStream fos = null;
            try {

                mAccessFile = new BufferedRandomIO(mCacheFile, "rw", false);
                mCurrentLength = curpos;
                mTotalLength = total;

                if (mTotalLength <= 0) {
                    mTotalLength = mCurrentLength;
                }

                mEntity.setTotalSizeInBytes(mTotalLength);
                mEntity.setDownloadedSizeInBytes(mCurrentLength);

                if (mCurrentLength > 0)
                    mAccessFile.seek(mCurrentLength);

                int progress = mTotalLength <= 0 ? 0 : (int) (mCurrentLength * 100L / mTotalLength);
                mEntity.setDownloadProgress(progress);
                mTask.setProgress(progress);

                return true;

            } catch (IOException e) {
                WLog.printStackTrace(e);
                Streams.safeClose(mAccessFile);
                return false;
            }
        }
    }

    private static class URLCallback implements Callback {

        private long mStartLength;
        private WeakReference<OkURLDownloader> mParent;

        public URLCallback(OkURLDownloader downloader, long len) {
            mParent = new WeakReference<>(downloader);
            mStartLength = len;
        }

        private void onDownloadResponse(int err, Bundle params) {
            OkURLDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "wink is null 1");
                return;
            }

            downloader.onStartResponse(err, params);
        }

        private boolean isCancelled() {
            OkURLDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "wink is null 2");
                return true;
            }

            return downloader.isCanceled();
        }

        private void onDownloadResult(int err) {
            OkURLDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "wink is null 3");
                return;
            }

            downloader.notifyResult(err);
        }

        @Override
        public void onFailure(Request request, IOException e) {
            WLog.v(TAG, "onFailure");

            int err = NetworkError.FAIL_CONNECT_TIMEOUT;
            if ("Canceled".equalsIgnoreCase(e.getMessage()) || isCancelled()) {
                err = NetworkError.CANCEL;
            }

            onDownloadResponse(err, null);
            onDownloadResult(err);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            int ret = NetworkError.SUCCESS;
            int code = response.code();

            ResponseBody body = response.body();
            WLog.i(TAG, "onResponse code = %d", code);

            if (!response.isSuccessful()) {
                Bundle errParams = new Bundle();
                errParams.putInt("http_status", code);
                ret = WinkError.toNetworkErrorWithHttpStatus(code);

                if (code == 416) {
                    Request request = response.request();
                    errParams.putString("http_request_range", request.header("RANGE"));

                    String range = response.header("Content-Range", null);
                    if (!TextUtils.isEmpty(range)) {
                        errParams.putString("http_response_range", range);
                    }
                }

                onDownloadResponse(ret, errParams);

            } else {
                onDownloadResponse(NetworkError.SUCCESS, null);
            }

            if (ret != NetworkError.SUCCESS) {
                Streams.safeClose(body);
                onDownloadResult(ret);
                return;
            }

//            final String[] exceptionContentType = new String[]{"text", "html"};
//
//
//            MediaType mediaType = body.contentType();
//            if (mediaType != null && !TextUtils.isEmpty(mediaType.type())) {
//                String type = mediaType.type().toLowerCase();
//                for (String s : exceptionContentType) {
//                    if (s.equals(type)) {
//                        WLog.w(TAG, "no available network!");
//                        Streams.safeClose(body);
//                        onDownloadResult(NetworkError.NO_AVAILABLE_NETWORK);
//                        return;
//                    }
//                }
//            }

            if (isCancelled()) {
                Streams.safeClose(body);
                onDownloadResult(NetworkError.CANCEL);
                return;
            }

            final OkURLDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "wink is null before io read/write");
                Streams.safeClose(body);
                return;
            }

            ContentHandler contentHandler = downloader.mContentHandler;


            long total = body.contentLength();
            if (total > 0) {
                total += mStartLength;
            }

            Charset charset = Charset.defaultCharset();
            MediaType contentType = body.contentType();
            if (contentType != null) {
                try {
                    charset = contentType.charset(charset);
                } catch (UnsupportedCharsetException e) {
                    //Couldn't decode the response body; charset is likely malformed.
                }
            }

            if (!contentHandler.prepare(mStartLength, total)) {
//                throw new NetworkIOException("content-length not invalid!", NetworkError.FAIL_IO_ERROR);
                WLog.w(TAG, "content-length not invalid!");
                Streams.safeClose(body);
                onDownloadResult(NetworkError.FAIL_IO_ERROR);
                return;
            }

            long current = mStartLength;

            do {
                if (downloader.isCanceled()) {
                    ret = NetworkError.CANCEL;
                    break;
                }

                //将返回结果转化为流，并写入文件
                int len;
                byte[] buf = new byte[BUFF_LEN];
                InputStream inputStream = body.byteStream();

                try {
                    while (!downloader.isCanceled() && (len = inputStream.read(buf)) != -1) {
                        if (len == 0)
                            continue;

                        if (!contentHandler.handle(buf, 0, len)) {
                            ret = NetworkError.FAIL_IO_ERROR;
                            break;
                        }

                        current += len;
                    }

                } catch (SocketTimeoutException e) {
                    WLog.printStackTrace(e);
                    ret = NetworkError.SOCKET_TIMEOUT;
                } catch (InterruptedIOException e) {
                    WLog.printStackTrace(e);
                    ret = NetworkError.CANCEL;
                } catch (Exception e) {
                    WLog.printStackTrace(e);
                    ret = NetworkError.FAIL_IO_ERROR;
                } finally {
                    Streams.safeClose(inputStream);
                }

            } while (false);

            WLog.d(TAG, "current length: %d", current);
            ret = !downloader.isCanceled() ? ret : NetworkError.CANCEL;
            contentHandler.complete(ret, (int) current);
            Streams.safeClose(body);
            onDownloadResult(ret);
        }
    }


}

