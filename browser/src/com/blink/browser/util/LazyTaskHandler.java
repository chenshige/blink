package com.blink.browser.util;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;

import java.util.LinkedList;
import java.util.List;

public class LazyTaskHandler {

    private static List<Runnable> sLazyTasks = new LinkedList<>();

    public static void addLazyTask(Runnable task) {
        sLazyTasks.add(task);
    }

    public static void executeLazyTask() {
        final Handler handler = new Handler(Looper.myLooper());
        for (final Runnable task : sLazyTasks) {
            Looper.myQueue().addIdleHandler(new IdleHandler() {
                @Override
                public boolean queueIdle() {
                    handler.post(task);
                    return false;
                }
            });
        }
        sLazyTasks.clear();
    }
}
