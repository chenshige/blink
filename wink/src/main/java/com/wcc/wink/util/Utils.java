package com.wcc.wink.util;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;

/**
 * Created by wenbiao.xie on 2016/6/13.
 */
public class Utils {
    public final static String TEMP_SUFFIX = ".wmp";
    public final static int KILO_BYTES = 1024;
    public final static int MEGA_BYTES = 1 << 20;
    public final static int GIGA_BYTES = 1 << 30;

    public static File getTempFile(File file) {
        if (file.getAbsolutePath().endsWith(TEMP_SUFFIX))
            return file;

        final String path = file.getAbsolutePath() + TEMP_SUFFIX;
        return new File(path);
    }

    public static <T> boolean isEmpty(Collection<T> c) {
        return c == null || c.size() == 0;
    }

    public static <T> boolean isEmpty(T[] objs) {
        return objs == null || objs.length == 0;
    }

    public static <E> int sizeOf(Collection<E> c) {
        return isEmpty(c) ? 0 : c.size();
    }


    /**
     * 创建文件，包括必要的父目录的创建，如果未创建
     *
     * @param file 待创建的文件
     * @return 返回操作结果
     * @throws IOException 创建失败，将抛出该异常
     */
    public static boolean create(File file) throws IOException {
        if (file.exists()) {
            return true;
        }

        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            return false;
        }

        return file.createNewFile();
    }

    /**
     * 创建大空文件，并指定文件长度
     *
     * @param file   文件对象
     * @param length 文件大小
     * @throws IOException
     */
    public static void createEmptyFile(File file, long length) throws IOException {
        if (file.exists()) {
            if (file.length() == length)
                return;

            file.delete();
        }

        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("failed to create parent file " + parent);
        }

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(length);
        } finally {
            Streams.safeClose(raf);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param path 指定路径
     * @return
     */
    public static boolean isExist(String path) {
        if (TextUtils.isEmpty(path))
            throw new IllegalArgumentException("path is empty!");

        File file = new File(path);
        return file.exists();
    }

    public static boolean IsSDKLevelAbove(int nLevel) {
        return android.os.Build.VERSION.SDK_INT >= nLevel;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static long getBlockSizeLong(StatFs stat) {
        if (IsSDKLevelAbove(18)) {
            return stat.getBlockSizeLong();
        } else {
            return stat.getBlockSize();
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static long getAvailableBlocks(StatFs stat) {
        if (IsSDKLevelAbove(18)) {
            return stat.getAvailableBlocksLong();
        } else {
            return stat.getAvailableBlocks();
        }
    }

    /**
     * 检查当前sdcard剩余空间大小
     */
    public static long getAvailableExternalStorageSize() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = getBlockSizeLong(stat);
            long availableBlocks = getAvailableBlocks(stat);
            long availableSize = availableBlocks * blockSize;
            return availableSize - 5 * 1024 * 1024;// 预留5M的空间
        }
        return -1;
    }

    public static String speedOf(long speed) {
        WLog.d("", "speed=%s", String.valueOf(speed));

//        speed <<= 3;
        final int K = 1024;
        final int M = 1 << 20;
        final int G = 1 << 30;
        if (speed >= G) {
            return String.format("%.1f Gb/s", (float) speed / G);
        } else if (speed >= M) {
            float f = (float) speed / M;
            return String.format(f > 100 ? "%.0f Mb/s" : "%.1f Mb/s", f);
        } else if (speed >= K) {
            float f = (float) speed / K;
            return String.format(f > 100 ? "%.0f Kb/s" : "%.1f Kb/s", f);
        } else
            return String.format("%d b/s", speed);
    }

    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }
}
