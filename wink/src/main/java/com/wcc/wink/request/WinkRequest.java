package com.wcc.wink.request;

import android.content.Context;

import com.wcc.wink.DownloadMode;
import com.wcc.wink.util.Comparators;


/**
 * Created by wenbiao.xie on 2015/11/5.
 */
public class WinkRequest {

    /**
     * 任务的优先级，值越大，则优先级越高
     */
    public static final int PRIORITY_DEFAULT = 0;       // 默认优先级
    public static final int PRIORITY_ACTIVE_START = 100;// 主动开始
    public static final int PRIORITY_CANDIDATE = 50;    // 候选优先级

    private final static int SAVE_THRESHOLD = 2048;     // 2K
    public final WinkStat mStat;
    private DownloadState mState;
    private final DownloadInfo mEntity;

    private Downloader mDownloader;
    private int mPriority = PRIORITY_DEFAULT;
    private final Context mContext;
    private int mProgress;
    private long mLast = -1;
    private boolean mDeleteRequested = false;
    private boolean mFirstDownloadForEntity;
    private int mForceStopCause;

    public WinkRequest(Context context, DownloadInfo entity) {
        this.mContext = context;
        this.mEntity = entity;
        this.mStat = new WinkStat();
        this.mProgress = entity.getDownloadProgress();
        this.mState = stateFromEntity(entity);
    }

    private static DownloadState stateFromEntity(DownloadInfo entity) {
        int status = entity.getDownloadState();
        switch (status) {
            case ResourceStatus.DOWNLOAD_FAILED:
                return DownloadState.stopped;

            case ResourceStatus.INIT:
                return DownloadState.intialized;

            case ResourceStatus.DOWNLOADING:
            case ResourceStatus.PAUSE:
            case ResourceStatus.WAIT:
                return DownloadState.paused;

            default:
                return DownloadState.deleted;
        }
    }

    public boolean shouldSilent() {
        return DownloadMode.shouldSilent(mEntity.getDownloadMode());
    }

    public void setForceStopCause(int cause) {
        mForceStopCause = cause;
    }

    public int getForceStopCause() {
        return mForceStopCause;
    }

    public void setFirstDownloadForEntity(boolean first) {
        mFirstDownloadForEntity = first;
    }

    public boolean isFirstDownloadForEntity() {
        return mFirstDownloadForEntity;
    }

    public Context getContext() {
        return mContext;
    }

    public void attach(Downloader downloader) {
        this.mDownloader = downloader;
    }

    public void detach() {
        this.mDownloader = null;
    }

    public Downloader getDownloader() {
        return mDownloader;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        mPriority = priority;
    }

    public synchronized boolean requestDelete() {
        return mDeleteRequested;
    }

    public synchronized void deleteIt() {
        mDeleteRequested = true;
    }

    public DownloadState getState() {
        return mState;
    }

    public void setState(DownloadState state) {
        this.mState = state;
    }

    public DownloadInfo getEntity() {
        return mEntity;
    }

    public WinkStat getDownloadStat() {
        return mStat;
    }

    public void onRequestStart() {
        mStat.onRequest(mEntity.getDownloadedSizeInBytes());
        Tracer tracer = mEntity.getTracer();
        if (tracer != null) {
            tracer.tryTimes++;
        }

        // 统计下载开始
        if (mFirstDownloadForEntity) {
            if (tracer != null)
                tracer.setStartTime(mStat.startTime);

            // TODO: 2016/6/14 add stat first wink

        } else {
            // TODO: 2016/6/14 add stat cotinue wink
        }
    }

    public void onRequestEnd(int err) {
        mStat.onDownloadEnd(err);
        Tracer tracer = mEntity.getTracer();
        if (tracer == null) return;

        tracer.setEndTime(System.currentTimeMillis());
        tracer.setConnectNetworkUsedTime(mStat.responseDuration);
        tracer.setUsedTime(mStat.downloadDuration);
        tracer.setMaxSpeed(mStat.maxSpeed);
        tracer.setAvgSpeed(mStat.avgSpeed);
        tracer.setCurrentSpeed(mStat.currentSpeed);
    }

    public void calcSpeed() {
        mStat.calcSpeed();
        Tracer tracer = mEntity.getTracer();
        if (tracer != null) {
            tracer.setAvgSpeed(mStat.avgSpeed);
            tracer.setMaxSpeed(mStat.maxSpeed);
            tracer.setCurrentSpeed(mStat.currentSpeed);
        }
    }

    void receiveBytes(int length) {
        mStat.onReceiveBytes(length);
        if (mLast == -1) {
            mLast = mEntity.getDownloadedSizeInBytes();
        }
    }

    public boolean needSaveForChanged() {
        int s = (int) (mEntity.getDownloadedSizeInBytes() - mLast);
        if (s >= SAVE_THRESHOLD) {
            mLast = mEntity.getDownloadedSizeInBytes();
            return true;
        }
        return false;
    }

    public  int getProgress() {
        return mProgress;
    }

    public void setProgress(int p) {
        this.mProgress = p;
    }

    public int compareTo(WinkRequest another) {
        int value = Comparators.compare(mPriority, another.mPriority);
        if (value != 0)
            return  value;

        return Comparators.compare(mStat.startTime, another.mStat.startTime);
    }
}
