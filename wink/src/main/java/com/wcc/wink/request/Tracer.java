package com.wcc.wink.request;


import com.wcc.wink.util.Utils;

/**
 * Created by wenbiao.xie on 2016/6/15.
 */
public class Tracer {
    public long startTime;
    public long endTime;
    // 下载网络请求响应间隔
    public int connectNetworkUsedTime;
    // 下载过程持续时间
    public int usedTime;
    // 平均速度
    public long avgSpeed;
    // 即时速度
    public long currentSpeed;
    // 最高速度
    public long maxSpeed;
    // 尝试次数
    public int tryTimes;

    public long getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(long avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public int getConnectNetworkUsedTime() {
        return connectNetworkUsedTime;
    }

    public void setConnectNetworkUsedTime(int connectNetworkUsedTime) {
        this.connectNetworkUsedTime = connectNetworkUsedTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(long maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(int usedTime) {
        this.usedTime = usedTime;
    }

    public int getTryTimes() {
        return tryTimes;
    }

    public void setTryTimes(int tryTimes) {
        this.tryTimes = tryTimes;
    }

    public long getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(long currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    @Override
    public String toString() {
        return "Tracer{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", connectNetworkUsedTime=" + connectNetworkUsedTime +
                ", usedTime=" + usedTime +
                ", maxSpeed=" + Utils.speedOf(maxSpeed) +
                ", avgSpeed=" + Utils.speedOf(avgSpeed) +
                ", tryTimes=" + tryTimes +
                '}';
    }
}
