// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NetworkServices implements Runnable {
    private static final String LOGTAG = NetworkServices.class.getSimpleName();

    private ConcurrentLinkedQueue<UpdateHandler> mTaskQueue = new ConcurrentLinkedQueue<UpdateHandler>();

    private final ScheduledExecutorService THREAD_POOL_EXECUTOR = Executors.newScheduledThreadPool(3);
    private AtomicBoolean mRunTask;

    private static NetworkServices sInstance = null;

    private NetworkServices() {
        mRunTask = new AtomicBoolean(true);
    }

    public static NetworkServices getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkServices();
        }

        return sInstance;
    }

    public void updateServices(UpdateHandler handler) {
        if (null == handler) return;
        mTaskQueue.add(handler);
        if (mRunTask.get()) {
            mRunTask.set(false);
            THREAD_POOL_EXECUTOR.execute(this);
        }
    }

    @Override
    public void run() {
        while (!mTaskQueue.isEmpty()) {
            UpdateHandler handler = mTaskQueue.poll();
            // Handler will be scheduled with a requested delay in milliseconds.
            THREAD_POOL_EXECUTOR.schedule(
                    new UpdatorRunnable(handler),
                    handler.delay(),
                    TimeUnit.MILLISECONDS);
        }
        mRunTask.set(true);
    }

    private class UpdatorRunnable implements Runnable {
        private UpdateHandler mHandler;

        public UpdatorRunnable(UpdateHandler handler) {
            mHandler = handler;
        }

        @Override
        public void run() {
            mHandler.initForUpdate();
            if (mHandler.checkUpdate()) {
                try {
                    mHandler.doUpdateBefore();
                    mHandler.doUpdateSuccess(mHandler.doUpdateNow());
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.doUpdateFail();
                }
            } else {
                mHandler.checkUpdateFail();
            }
        }
    }

}
