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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wenbiao.xie on 2016/6/23.
 */
public class SegmentedDownloader extends Downloader implements URLParams.URLParamsCreator<DownloadInfo> {
    private final static String TAG = "SegmentedDownloader";
    private final static int BUFF_LEN = 4096;
    private final static int MIN_SEGMENTS = 2;
    private final static int MAX_SEGMENTS = 4;
    private final static int MIN_SEGMENT_SIZE = 2 * Utils.MEGA_BYTES;

    private URLParams<DownloadInfo> mParams = null;
    private volatile boolean isRunning = false;
    private final Object runningLock = new Object();
    private Thread mDownloadThread;
    private OkHttpClient mClient;
    private AtomicBoolean mCancelled = new AtomicBoolean(false);
    private CountDownLatch mResultLatch;
    private Call[] mCalls;
    private int[] mResults;
    private Context mContext;

    protected SegmentedDownloader(WinkRequest task) {
        this(task, null);
    }

    protected SegmentedDownloader(WinkRequest task, URLParams.URLParamsCreator<DownloadInfo> creator) {
        super(task);
        mContext = task.getContext();
        mClient = Connections.getOkHttpClient();
        if (creator != null)
            this.mParams = creator.createUrlParams(task.getEntity());
        else {
            this.mParams = createUrlParams(task.getEntity());
        }

        mEntity.downloaderType = DOWNLOADER_SEGMENTED;
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

    private Request createRangeRequest(String url, DownloadInfo.DownloadRange range, String referer) {
        long pos = range.currentPosition();
        long end = range.end() - 1;

        //封装请求
        Request.Builder builder = new Request.Builder()
                //下载地址
                .header("Range", "bytes=" + pos + "-"
                        + end)
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

        WLog.i(TAG, "create okhttp range request for url %s", url);
        return builder.build();
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

                long total = entity.getTotalSizeInBytes();
                entity.reset();
                entity.setTotalSizeInBytes(total);
                return true;
            } else if (cached) {
                file.renameTo(target);
            }

            throw new NetworkIOException("already completed!", WinkError.NetworkError.SUCCESS);
        }
        // 如果文件未下完，且下载长度大于总长度
        else if (entity.getDownloadedSizeInBytes() > entity.getTotalSizeInBytes()) {
            WLog.w(TAG, "downloaed(%d) > total(%d) happened", entity.getDownloadedSizeInBytes(),
                    entity.getTotalSizeInBytes());

            File file = target;
            boolean exists = file.exists();
            if (!exists) {
                file = Utils.getTempFile(target);
                exists = file.exists();
            }

            if (exists && file.length() != entity.getTotalSizeInBytes()) {
                file.delete();
                long total = entity.getTotalSizeInBytes();
                entity.reset();
                entity.setTotalSizeInBytes(total);
                return true;
            } else if (!exists) {
                long total = entity.getTotalSizeInBytes();
                entity.reset();
                entity.setTotalSizeInBytes(total);
                return true;
            }
        }
        // 如果检查缓存文件的存在性与有效性
        else {
            File file = Utils.getTempFile(target);
            if (file.exists()) {
                // 修正进度，以实际大小为准
                if (entity.getTotalSizeInBytes() != file.length()) {
                    file.delete();
                    long total = entity.getTotalSizeInBytes();
                    entity.reset();
                    entity.setTotalSizeInBytes(total);
                    return true;
                }

            } else if (entity.getDownloadedSizeInBytes() != 0) {
                long total = entity.getTotalSizeInBytes();
                entity.reset();
                entity.setTotalSizeInBytes(total);
                return true;
            }
        }

        return false;
    }

    private int fitSegments(long length) {
        int s = (int) (length / MIN_SEGMENT_SIZE);
        s = Math.max(s, MIN_SEGMENTS);
        return Math.min(s, MAX_SEGMENTS);
    }

    private DownloadInfo.DownloadRange[] checkAndCreateRanges(DownloadInfo entity) {
        DownloadInfo.DownloadRange[] ranges = entity.ranges;
        if (ranges == null || ranges.length == 0) {
            long length = entity.getTotalSizeInBytes();
            int size = fitSegments(length);
            int each = (int) (length / size);
            ranges = new DownloadInfo.DownloadRange[size];
            long start = 0;
            int index = 0;

            WLog.i(TAG, "checkAndCreateRanges total=%d, rangs=%d, each=%d", length, size, each);
            do {

                DownloadInfo.DownloadRange ra = new DownloadInfo.DownloadRange();
                ra.start = start;
                ra.length = each;
                ranges[index++] = ra;
                start = ra.end();

            } while (start + each <= length);

            WLog.i(TAG, "checkAndCreateRanges left check, start = %d....", start);
            if (start < length) {
                ranges[size - 1].length += length - start;
            }

            entity.ranges = ranges;
            return ranges;
        }

        int size = 0;
        for (int i = 0; i < ranges.length; i++)
            if (!ranges[i].finished())
                size++;

        if (size == 0)
            return null;

        if (size == ranges.length)
            return ranges;

        DownloadInfo.DownloadRange[] results = new DownloadInfo.DownloadRange[size];
        for (int i = ranges.length - 1; i >= 0 && size > 0; i--)
            if (!ranges[i].finished())
                results[--size] = ranges[i];

        return results;
    }

    @Override
    public int download() {
        mDownloadThread = Thread.currentThread();
        isRunning = true;

        int err;
        final DownloadInfo entity = mEntity;
        final URLParams<DownloadInfo> params = mParams;
        WLog.i(TAG, "download start...");
        do {
            if (isCanceled()) {
                err = WinkError.NetworkError.CANCEL;
                break;
            }
            WLog.i(TAG, "check entity and files");

            // 检查是否已经下载完成
            try {
                checkEntity(entity, params.target);
            } catch (NetworkIOException e) {
                WLog.printStackTrace(e);
                err = WinkError.NetworkError.SUCCESS;
                break;
            } catch (IOException e) {
                WLog.printStackTrace(e);
                err = WinkError.NetworkError.FAIL_IO_ERROR;
                break;
            }

            if (isCanceled()) {
                err = WinkError.NetworkError.CANCEL;
                break;
            }

            WLog.i(TAG, "create ranges...");
            // 创建分区信息
            DownloadInfo.DownloadRange[] ranges = checkAndCreateRanges(entity);
            if (ranges == null) {
                err = WinkError.NetworkError.SUCCESS;
                break;
            }

            File file = Utils.getTempFile(params.target);
            final String path = mEntity.getLocalFilePath();
            if (TextUtils.isEmpty(path)) {
                mEntity.setLocalFilePath(file.getAbsolutePath());
            }

            WLog.i(TAG, "create empty file with length %d", mEntity.getTotalSizeInBytes());
            try {
                // 创建大空文件
                Utils.createEmptyFile(file, mEntity.getTotalSizeInBytes());
            } catch (IOException e) {
                WLog.printStackTrace(e);
                err = WinkError.NetworkError.FAIL_IO_ERROR;
                break;
            }

            if (isCanceled()) {
                err = WinkError.NetworkError.CANCEL;
                break;
            }

            // 已经准备好了，可以创建分段下载线程
            onPrepare();

            WLog.i(TAG, "downloading...create %d range download requests", ranges.length);
            try {
                mResultLatch = new CountDownLatch(ranges.length);
                mResults = new int[ranges.length];

                Call[] calls = new Call[ranges.length];
                for (int i = 0; i < ranges.length; i++) {
                    String referer = "";
                    if (params.entity.getResource() instanceof SimpleURLResource) {
                        referer = ((SimpleURLResource) params.entity.getResource()).getReferer();
                    }
                    Request request = createRangeRequest(params.url, ranges[i], referer);
                    calls[i] = mClient.newCall(request);
                }

                if (isCanceled()) {
                    err = WinkError.NetworkError.CANCEL;
                    break;
                }

                mCalls = calls;
                for (int i = 0; i < ranges.length; i++) {
                    calls[i].enqueue(new RangeURLCallback(this, file, i, ranges[i]));
                }

                onStart();
                err = waitResult();

                if (err == WinkError.SUCCESS) {
                    Uri apkpath = Uri.withAppendedPath(Uri.fromFile(new File(Wink.get().getSetting()
                            .getSimpleResourceStorageDirectory().getAbsolutePath())), mEntity.getTitle());
                    if (FileUtils.fileIsExists(apkpath.getPath())) {
                        FileUtils.checkFile(mContext, mParams.entity.getTitle(), 1, file, mEntity);
                    } else {
                        boolean ret = file.renameTo(new File(apkpath.getPath()));
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
                } else if (err == WinkError.NetworkError.AUTH_EXPIRED) {
                    mEntity.setUrl(null);
                } else if (err == WinkError.NetworkError.RANGE_INVALID) {
                    mEntity.reset();
                }

            } catch (InterruptedException e) {
                WLog.printStackTrace(e);
                err = WinkError.NetworkError.CANCEL;
            } catch (Exception e) {
                WLog.printStackTrace(e);
                err = WinkError.NetworkError.FAIL_UNKNOWN;
            }

        } while (false);

        if (err != WinkError.NetworkError.SUCCESS &&
                err != WinkError.NetworkError.CANCEL && isCanceled()) {
            err = WinkError.NetworkError.CANCEL;
        }

        onDownloadComplete(err);
        mDownloadThread = null;
        mCalls = null;
        synchronized (runningLock) {
            isRunning = false;
            runningLock.notify();
        }

        WLog.i(TAG, "download completed, error = %d", err);
        return err;
    }

    @Override
    public void cancel(boolean fordelete) {
        WLog.v(TAG, "cancel");
        if (isCanceled())
            return;

        mCancelled.set(false);
        // 取消所有网络操作
        final Call[] calls = mCalls;
        if (calls != null) {
            for (Call call : calls) {
                if (call != null && !call.isCanceled()) {
                    call.cancel();
                }
            }
        }

        final Thread thread = mDownloadThread;
        if (thread != null) {
            thread.interrupt();
        }

        if (fordelete) {
            await();

            File cacheFile = Utils.getTempFile(mParams.target);
            if (cacheFile.exists()) {
                cacheFile.delete();
            } else if (mParams.target.exists()) {
                mParams.target.delete();
            }
        }
    }

    private void await() {
        synchronized (runningLock) {
            while (isRunning) {
                try {
                    runningLock.wait(20);
                } catch (InterruptedException e) {
                    WLog.printStackTrace(e);
                    break;
                }
            }
        }
    }

    private void notifyResult(int index, int err) {
        mResults[index] = err;
        mResultLatch.countDown();
    }

    private int waitResult() throws InterruptedException {
        mResultLatch.await();
        for (int i = 0; i < mResults.length; i++) {
            if (mResults[i] != WinkError.NetworkError.SUCCESS)
                return mResults[i];
        }

        return WinkError.NetworkError.SUCCESS;
    }

    private synchronized void onStartResponse(int err, Bundle params) {
        final WinkRequest task = mTask;
        if (err == WinkError.NetworkError.SUCCESS) {
            task.getDownloadStat().onDownloadResponse();
        } else {
            task.getDownloadStat().onErrorHappened(err, params);
        }
    }

    int oldProgress = -1;

    synchronized void onAdvance(DownloadInfo.DownloadRange range, int downloaded) {
        range.downloaded += downloaded;
        mEntity.incrDownloadedBytes(downloaded);
        mTask.receiveBytes(downloaded);
        if (oldProgress == mEntity.getDownloadProgress())
            return;

        oldProgress = mEntity.getDownloadProgress();
        WLog.i(TAG, "download bytes, progress: %d%%, current: %d, total: %d",
                oldProgress, mEntity.getDownloadedSizeInBytes(), mEntity.getTotalSizeInBytes());
        if (WLog.isDebug()) {
            WLog.i(TAG, "====>download ranges:");
            DownloadInfo.DownloadRange[] ranges = mEntity.getRanges();
            for (int i = 0; i < ranges.length; i++) {
                WLog.i(TAG, "========>range %d: %s", i, ranges[i]);
            }
        }

        notifyDownloadEvent(EVENT_PROGRESS, oldProgress);
    }

    static class RangeContentHandler implements ContentHandler {

        final RandomAccessFile mAccessFile;
        final DownloadInfo.DownloadRange mRange;
        private final WeakReference<SegmentedDownloader> mParent;

        public RangeContentHandler(SegmentedDownloader downloader, File cache,
                                   DownloadInfo.DownloadRange range) throws FileNotFoundException {
            this.mRange = range;
            mParent = new WeakReference<>(downloader);
//            mAccessFile = new RandomAccessFile(cache, "rw");
            mAccessFile = new BufferedRandomIO(cache, "rw", 1024 * 8, false);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                mAccessFile.close();
            } finally {
                super.finalize();
            }
        }

        private void onAdvance(int len) {
            SegmentedDownloader downloader = mParent.get();
            if (downloader == null)
                return;

            downloader.onAdvance(mRange, len);
        }

        @Override
        public void complete(int cause, int curpos) {
            WLog.v(TAG, "complete cause = %d, length = %d", cause, curpos);
            Streams.safeClose(mAccessFile);
        }

        @Override
        public boolean handle(byte[] datas, int offset, int len) {

            if (len > 0) {
                try {
                    mAccessFile.write(datas, offset, len);
                    onAdvance(len);
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
            try {
                if (curpos > 0)
                    mAccessFile.seek(curpos);

                return true;
            } catch (IOException e) {
                WLog.printStackTrace(e);
                Streams.safeClose(mAccessFile);
                return false;
            }
        }
    }

    private static class RangeURLCallback implements Callback {

        private WeakReference<SegmentedDownloader> mParent;
        private int mIndex;
        private final ContentHandler mContentHandler;
        private long mStartPosition;

        public RangeURLCallback(SegmentedDownloader downloader, File cache, int index,
                                DownloadInfo.DownloadRange range) throws IOException {
            mParent = new WeakReference<>(downloader);
            mStartPosition = range.currentPosition();
            mIndex = index;
            mContentHandler = new RangeContentHandler(downloader, cache, range);
        }

        private void onDownloadResponse(int err, Bundle params) {
            SegmentedDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "onDownloadResponse wink is null");
                return;
            }

            downloader.onStartResponse(err, params);
        }

        private boolean isCancelled() {
            SegmentedDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "isCancelled wink is null");
                return true;
            }

            return downloader.isCanceled();
        }

        private void onDownloadResult(int err) {
            SegmentedDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "onDownloadResult wink is null");
                return;
            }

            downloader.notifyResult(mIndex, err);
        }

        @Override
        public void onFailure(Request request, IOException e) {
            WLog.v(TAG, "onFailure");
            WLog.printStackTrace(e);

            int err = WinkError.NetworkError.FAIL_CONNECT_TIMEOUT;
            if ("Canceled".equalsIgnoreCase(e.getMessage()) || isCancelled()) {
                err = WinkError.NetworkError.CANCEL;
            } else if (e instanceof NetworkIOException) {
                err = ((NetworkIOException) e).code();
            }

            onDownloadResponse(err, null);
            onDownloadResult(err);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            int ret = WinkError.NetworkError.SUCCESS;
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
                onDownloadResponse(WinkError.NetworkError.SUCCESS, null);
            }

            if (ret != WinkError.NetworkError.SUCCESS) {
                Streams.safeClose(body);
                onDownloadResult(ret);
                return;
            }

            final String[] exceptionContentType = new String[]{"text", "html"};


            MediaType mediaType = body.contentType();
            if (mediaType != null && !TextUtils.isEmpty(mediaType.type())) {
                String type = mediaType.type().toLowerCase();
                for (String s : exceptionContentType) {
                    if (s.equals(type)) {
                        WLog.w(TAG, "no available network!");
                        Streams.safeClose(body);
                        onDownloadResult(WinkError.NetworkError.NO_AVAILABLE_NETWORK);
                        return;
                    }
                }
            }

            if (isCancelled()) {
                Streams.safeClose(body);
                onDownloadResult(WinkError.NetworkError.CANCEL);
                return;
            }

            final SegmentedDownloader downloader = mParent.get();
            if (downloader == null) {
                WLog.w(TAG, "wink is null before io read/write");
                Streams.safeClose(body);
                return;
            }

            ContentHandler contentHandler = mContentHandler;
            long total = body.contentLength();
            if (!contentHandler.prepare(mStartPosition, total)) {
                    /*throw new NetworkIOException("content-length not invalid!",
                            WinkError.NetworkError.FAIL_IO_ERROR);*/
                WLog.w(TAG, "content-length not invalid!");
                Streams.safeClose(body);
                onDownloadResult(WinkError.NetworkError.FAIL_IO_ERROR);
                return;
            }


            long current = 0;

            do {
                if (downloader.isCanceled()) {
                    ret = WinkError.NetworkError.CANCEL;
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
                            ret = WinkError.NetworkError.FAIL_IO_ERROR;
                            break;
                        }

                        current += len;
                    }

                } catch (SocketTimeoutException e) {
                    WLog.printStackTrace(e);
                    ret = WinkError.NetworkError.SOCKET_TIMEOUT;
                } catch (InterruptedIOException e) {
                    WLog.printStackTrace(e);
                    ret = WinkError.NetworkError.CANCEL;
                } catch (Exception e) {
                    WLog.printStackTrace(e);
                    ret = WinkError.NetworkError.FAIL_IO_ERROR;
                } finally {
                    Streams.safeClose(inputStream);
                }

            } while (false);

            WLog.d(TAG, "current length: %d", current);
            ret = !downloader.isCanceled() ? ret : WinkError.NetworkError.CANCEL;
            contentHandler.complete(ret, (int) current);
            Streams.safeClose(body);
            onDownloadResult(ret);
        }
    }
}
