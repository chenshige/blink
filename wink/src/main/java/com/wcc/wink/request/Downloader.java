package com.wcc.wink.request;

public abstract class Downloader {

    public final static int EVENT_PREPARING = 0;
    public final static int EVENT_PREPARED = 1;
    public final static int EVENT_START = 2;
    public final static int EVENT_PROGRESS = 3;
    public final static int EVENT_COMPLETE = 4;
    public final static int EVENT_ERROR = 5;

    public final static int PREPARE_ACTION_FETCHURL = 1;

    public static final int DOWNLOADER_UNKNOWN = 0;
    public static final int DOWNLOADER_WHOLE_SINGLE = 1;
    public static final int DOWNLOADER_SEGMENTED = 2;

    public interface OnDownloadListener {
        void onDownload(WinkRequest task, int event, int param);
    }

    protected WinkRequest mTask;
    protected DownloadInfo mEntity = null;
    protected OnDownloadListener mOnDownloadListener = null;

    protected Downloader(WinkRequest task) {
        this.mTask = task;
        this.mEntity = task.getEntity();
    }


    public void setOnDownloadListener(OnDownloadListener listener) {
        this.mOnDownloadListener = listener;
    }

    protected void notifyDownloadEvent(int event, int param) {
        final OnDownloadListener listener = mOnDownloadListener;
        if (listener != null) {
            listener.onDownload(mTask, event, param);
        }
    }

    protected void onPrepare() {
        mTask.getDownloadStat().onDownloadStart();
        notifyDownloadEvent(EVENT_PREPARED, 0);
    }

    protected void onStart() {
        notifyDownloadEvent(EVENT_START, 0);
    }

    protected void onDownloadComplete(int cause) {
        notifyDownloadEvent(EVENT_COMPLETE, cause);
    }

    public boolean isCanceled() {
        return false;
    }

    public abstract int download();

    public abstract void cancel(boolean fordelete);
}
