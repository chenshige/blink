package com.wcc.wink.request;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by wenbiao.xie on 2016/6/21.
 */
public class WinkRuntime {

    private final Map<String, WinkRequest> mWaitingTasks;
    private final Map<String, WinkRequest> mRunningTasks;

    private final Map<String, WinkRequest> mFinishedTasks;
    private final Map<String, WinkRequest> mUnFinishedTasks;
    private final Context mContext;
    private final RuntimePolicy mPolicy;
    private final ExecutorService mExecutorService;
    private Handler mHandler = null;
    private boolean mAllowSchedule = true;
    private int mSilentRunningTaskCount = 0;

    WinkRuntime(Context context, RuntimePolicy runtimePolicy, ExecutorService service,
                Looper looper) {
        this.mContext = context.getApplicationContext();
        this.mPolicy = runtimePolicy;
        this.mWaitingTasks = new HashMap<>(10);
        this.mRunningTasks = new HashMap<>(Math.max(mPolicy.maxRunningPoolSize(), 3));
        this.mUnFinishedTasks = new HashMap<>(10);
        this.mFinishedTasks = new HashMap<>(10);

        if (service == null) {
            int threads = runtimePolicy.maxRunningPoolSize() +
                    runtimePolicy.slientRunningPoolSize();

            mExecutorService = Executors.newFixedThreadPool(threads, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "download_task");
                    return t;
                }
            });

        } else {
            mExecutorService = service;
        }

        mHandler = new Handler(looper);
    }

}
