package com.blink.browser.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.tcl.framework.log.NLog;
import com.wcc.wink.util.Streams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

public class ContextUtils {
    public static boolean isMainThread() {
        long id = Thread.currentThread().getId();
        return id == Looper.getMainLooper().getThread().getId();
    }

    public static String getCurrentProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager
                .getRunningAppProcesses();

        if (processInfos == null || processInfos.isEmpty())
            return null;

        for (ActivityManager.RunningAppProcessInfo appProcess : processInfos) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }

        return null;
    }

    public static String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = null;
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        File file = new File("/proc/"+pid+"/cmdline");
        try {
            fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            processName = bufferedReader.readLine();
            if (processName != null) {
                processName = processName.trim();
            }
        } catch (Exception e) {
            NLog.printStackTrace(e);
        } finally {
            Streams.safeClose(bufferedReader);
            Streams.safeClose(fileInputStream);
        }

        return processName;
    }



    public static boolean isChildProcess(Context context) {
        String process = getCurrentProcessName();
        if (TextUtils.isEmpty(process)) {
            process = getCurrentProcessName(context);
        }

        String pkName = context.getPackageName();

        if (!TextUtils.isEmpty(process) && !pkName.equals(process))
            return true;

        return false;
    }

    public static String getMetaData(Context context, String name) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        Object value = null;
        try {

            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo != null && applicationInfo.metaData != null) {
                value = applicationInfo.metaData.get(name);
            }

        } catch (NameNotFoundException e) {
            return "";
        }

        return value == null ? "" : value.toString();
    }

    public static boolean install(Context context, String apkPath) {
        if (TextUtils.isEmpty(apkPath)) {
            NLog.w("ContextUtils", "download complete intent has no path param");
            return false;
        }

        File file = new File(apkPath);
        if (!file.exists()) {
            NLog.w("ContextUtils", "file %s not exists", apkPath);
            return false;
        }

        if(isSystemApp(context)){
            return systemInstall(apkPath);
        }else{
            File apkFile = new File(apkPath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
    }

    /**
     * 是否为系统应用
     *
     * @param context
     * @return
     */
    public static boolean isSystemApp(Context context) {

        boolean system =  ((context.getApplicationInfo()).flags & ApplicationInfo.FLAG_SYSTEM) > 0;
        if (!system)
            return false;

        int perm = ContextCompat.checkSelfPermission(context, "android.permission.INSTALL_PACKAGES");
        return  (perm == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * check rootPerssion.
     * @return
     */
    public static boolean hasRooted() {

        PrintWriter writer = null;
        Process process = null;
        try {

            process = Runtime.getRuntime().exec("su");
            writer = new PrintWriter(process.getOutputStream());
            writer.flush();
            writer.close();
            int value = process.waitFor();
            NLog.d("Root", "su return %d", value);
            return value == 0;
        } catch (Exception e) {
            NLog.printStackTrace(e);
        } finally {
            Streams.safeClose(writer);
            if (process!=null) {
                process.destroy();
            }
        }

        return false;
    }

    public static boolean systemInstall(String apkPath) {
        String result = pmInstall(apkPath, null);
        return "success".equalsIgnoreCase(result);
    }

    /**
     * 系统级自动安装
     *
     * @param apkPath 安装包位置
     * @param console 错误流输出内容，当传null时，默认忽略错误流
     * @return 返回安装结果，如果失败返回null，如果成功返回"Success", 详情可参考pm指令
     */
    public static String pmInstall(String apkPath, StringBuilder console) {
        String[] args = { "pm", "install", "-r", apkPath };
        String result = "";
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        InputStreamReader reader = null;
        StringBuilder err = console;

        try {
            process = processBuilder.start();
            // 读取异常流
            errIs = process.getErrorStream();
            reader = new InputStreamReader(errIs, "utf-8");
            BufferedReader br = new BufferedReader(reader);
            String line;
            boolean firstLine = true;
            while ( (line = br.readLine()) != null) {
                if (err == null)
                    continue;

                err.append(line);
                if (firstLine) {
                    firstLine = false;
                } else {
                    err.append("\n");
                }
            }

            br.close();

            // 读取结果流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;
            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }

            if (baos.size() > 0) {
                byte[] data = baos.toByteArray();
                result = new String(data);
            }

        } catch (Exception e) {
            NLog.printStackTrace(e);
        } finally {
            Streams.safeClose(errIs);
            Streams.safeClose(inIs);
            Streams.safeClose(reader);

            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    private static void invokeInstall(String apkPath) throws Exception {
//        Class<?> activityThread = Class.forName("android.app.ActivityThread");
//        Object pm = InvokeUtil.invokeStaticMethod(activityThread, "getPackageManager");
//        Uri uri = Uri.fromFile(new File(apkPath));
//        InvokeUtil.invokeMethod(pm, "installPackage", uri, null, 0, null);
    }
}

