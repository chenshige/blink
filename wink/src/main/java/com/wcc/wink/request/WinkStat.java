package com.wcc.wink.request;

import android.os.Bundle;

import com.wcc.wink.WinkError;
import com.wcc.wink.util.WLog;

/**
 * Created by wenbiao.xie on 2015/11/6.
 */
public class WinkStat {
    // 下载触发时间
    public long startTime;
    // 下载起始长度
    public long startLength;
    // 下载开始时间
    public long startDownloadTime;
    // 下载请求等待间隔
    public int waitingDuration;
    // 下载网络请求响应间隔
    public int responseDuration;
    // 下载过程持续时间
    public int downloadDuration;
    // 本次下载的字节数
    public long downloadBytes;
    // 最近接收数据时间
    public long lastReceiveTime;
    // 平均速度，采用整数计数，*100保留两位小数
    public long avgSpeed;
    // 当前速度，采用整数计数，*100保留两位小数
    public long currentSpeed;
    // 最高速度，采用整数计数，*100保留两位小数
    public long maxSpeed;
    // 异常错误码
    public int error = Integer.MAX_VALUE;
    public Bundle paramsForError;

    void onRequest(long length) {
        startTime = System.currentTimeMillis();
        startLength = length;
    }

    void onDownloadStart() {
        startDownloadTime = System.currentTimeMillis();
        long waiting = startDownloadTime - startTime;
        waitingDuration = (int) (waiting / 1000L);
    }

    boolean hasResponded() {
        return (error != Integer.MAX_VALUE);
    }

    void onDownloadResponse() {
        if (hasResponded()) {
            return;
        }
        error = WinkError.NetworkError.SUCCESS;
        lastReceiveTime = System.currentTimeMillis();
        lastCalcSpeedTime = lastReceiveTime;
        long end = lastReceiveTime - startDownloadTime;
        responseDuration = (int) (end / 1000L);
    }

    void onErrorHappened(int err, Bundle params) {
        if (hasResponded()) {
            error = err;
            paramsForError = params;
            return;
        }

        error = err;
        paramsForError = params;
        lastReceiveTime = System.currentTimeMillis();
        lastCalcSpeedTime = lastReceiveTime;
        long end = lastReceiveTime - startDownloadTime;
        responseDuration = (int) (end / 1000L);
    }

    synchronized void onDownloadEnd(int err) {
        if (startDownloadTime == 0) {
            onDownloadStart();
        }

        if (!hasResponded()) {
            if (err != WinkError.NetworkError.SUCCESS)
                onErrorHappened(err, null);
            else
                onDownloadResponse();
        } else if (error != err) {
            error = err;
        }

        long end = System.currentTimeMillis();
        downloadDuration = (int) ((end - startDownloadTime) / 1000L);
        calcSpeed();
        lastCalcSpeedTime = 0L;
    }

    private int calcBytes;
    private long lastCalcSpeedTime;

    synchronized void onReceiveBytes(int length) {
        downloadBytes += length;
        calcBytes = length;
        lastReceiveTime = System.currentTimeMillis();
    }

    public synchronized void calcSpeed() {
        if (lastCalcSpeedTime == 0L) {
            return;
        }

        long end = System.currentTimeMillis();
        long last = (end - startDownloadTime);
        if (last > 0)
            avgSpeed = downloadBytes * 1000L / last;

        last = (end - lastCalcSpeedTime);
        if (last <= 0)
            return;

        currentSpeed = calcBytes * 1000L / last;
        if (currentSpeed > maxSpeed)
            maxSpeed = currentSpeed;

        WLog.e("downloadSpeed", "currentSpeed = %s //// downloadBytes = %s //// last = %s", currentSpeed, calcBytes,
                last);

        lastCalcSpeedTime = end;
        calcBytes = 0;
    }
}
