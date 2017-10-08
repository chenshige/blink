package com.blink.browser.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.URLUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    public static String SDPATH = Environment.getExternalStorageDirectory()
            + "/browser/";

    /**
     * apk下载存放路径
     */
    public static final String LOCATION_APK = "/Download/";
    private static int BUFFER = 1024;

    public static String saveBitmap(Bitmap bitmap, String picName) {
        return saveBitmap(bitmap, picName, Bitmap.CompressFormat.JPEG);
    }

    public static String saveBitmap(Bitmap bitmap, String picName, Bitmap.CompressFormat fmt) {
        File f = null;
        FileOutputStream out = null;
        try {
            f = new File(SDPATH, picName + "." + fmt.toString());
            if (f.exists()) {
                f.delete();
            }
            out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            return f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(out);
        }
        return f.getAbsolutePath();
    }

    // 获取制定目录下的所有文件
    public static List<String> getAllFiles(String absolute_dir) {
        File file = new File(absolute_dir);
        File[] subFile = file.listFiles();

        List<String> files = new ArrayList<>();
        for (int index = 0; index < subFile.length; index++) {
            // 判断是否为文件夹
            if (!subFile[index].isDirectory()) {
                String filename = subFile[index].getName();
                // 判断是否为MP4结尾
                if (filename.trim().toLowerCase().endsWith(".txt")) {
                    files.add(filename);
                }
            }
        }
        return files;
    }

    /**
     * 递归删除一个文件夹和下面的所有文件、子文件夹
     *
     * @param dir 要删除的文件夹
     * @return true 删除成功；false 删除失败
     */
    public static boolean deleteDir(File dir) {
        return deleteDir(dir, true);
    }

    /**
     * @param deRoot 是否删除根目录
     */
    public static boolean deleteDir(File dir, boolean deRoot) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return true;

        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete();
            else if (file.isDirectory())
                deleteDir(file);
        }
        if (deRoot) {
            return dir.delete();
        } else {
            return true;
        }
    }

    public static boolean fileIsExists(String path) {
        try {
            File f = new File(path);
            return f.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createFileDir(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return f.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean createFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) return true;
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    public static void saveBitmap(Bitmap bm, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(out);
        }
    }

    public static Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        FileInputStream in = null;
        File file = null;
        if (!TextUtils.isEmpty(path)) {
            file = new File(path);
            if (!file.exists()) {
                return null;
            }
            try {
                in = new FileInputStream(file);
                bitmap = FormatTools.getInstance().InputStream2Bitmap(in);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
               closeStream(in);
            }
        }
        return bitmap;
    }

    public static void unzip(InputStream zipFile, String location) {
        ZipInputStream zin = null;
        ZipEntry ze = null;
        FileOutputStream fout = null;
        try {
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            zin = new ZipInputStream(zipFile);
            while ((ze = zin.getNextEntry()) != null) {
                File path = new File(location, ze.getName());
                if (ze.isDirectory()) {
                    if (!path.isDirectory()) {
                        path.mkdirs();
                    }
                } else {
                    File zipF = path;
                    fout = new FileOutputStream(zipF, false);
                    try {
                        byte buffer[] = new byte[4096];
                        int realLength;
                        while ((realLength = zin.read(buffer)) > 0) {
                            fout.write(buffer, 0, realLength);
                        }
                    } finally {
                        closeStream(fout);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(fout,zin);
        }
    }

    public static String getLocationApkDir() {
        return LOCATION_APK;
    }

    public static String getLocalDir() {
        String localDir = Environment.getExternalStorageDirectory().getPath();
        localDir += LOCATION_APK;
        return localDir;
    }

    public static String getApkPath(String url, String contentDisposition,
                                    String mimeType) {
        return URLUtil.guessFileName(url, contentDisposition, mimeType);
    }

    public static String getExternalStorageApkPath(String url) {
        return getLocalDir() + getApkPath(url, null, null);
    }

    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(File file) {
        long size = 0;

        FileInputStream fis = null;
        try {
            if (file.exists()) {
                try {
                    fis = new FileInputStream(file);
                    try {
                        size = fis.available();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(fis);
        }

        return size;
    }

    /**
     * 获取指定文件夹
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSizes(File file) {
        long size = 0;

        try {
            if (file.exists()) {
                File flist[] = file.listFiles();
                for (int i = 0; i < flist.length; i++) {
                    if (flist[i].isDirectory()) {
                        size = size + getFileSizes(flist[i]);
                    } else {
                        size = size + getFileSize(flist[i]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 格式化文件大小
     *
     * @param size
     * @return
     */
    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(
                size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getFileNameFromUrl(String url) {
        String fileName = "";
        if (!TextUtils.isEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            int query = url.lastIndexOf('?');
            if (query > 0) {
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            fileName = 0 <= filenamePos ? url.substring(filenamePos + 1) : url;
        }
        return fileName;
    }

    public static String getFileExtensionFromUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) {
                url = url.substring(0, fragment);
            }

            String param = "";
            int query = url.lastIndexOf('?');
            if (query > 0) {
                param = url.substring(query, url.length());
                url = url.substring(0, query);
            }

            int filenamePos = url.lastIndexOf('/');
            String filename =
                    0 <= filenamePos ? url.substring(filenamePos + 1) : url;

            // if the filename contains special characters, we don't
            // consider it valid for our matching purposes:
            if (!filename.isEmpty()) {
                int dotPos = filename.lastIndexOf('.');
                if (0 <= dotPos) {
                    return filename.substring(dotPos + 1);
                } else {
                    int pos = param.lastIndexOf('.');
                    if (0 <= pos) {
                        String exten = param.substring(pos + 1);
                        int andpos = exten.indexOf('&');
                        if (andpos > 0) {
                            return exten.substring(0, andpos);
                        } else {
                            return param.substring(pos + 1);
                        }
                    }
                }
            }
        }

        return "";
    }

    /**
     * 根据文件名读取assert文件
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String getFromAsset(Context context, String fileName) {
        String result = "";

        InputStream in = null;
        try {
            in = context.getResources().getAssets().open(fileName);
            int length = in.available();
            byte[] buffer = new byte[length];
            in.read(buffer);
            result = new String(buffer, 0, buffer.length, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(in);
        }
        return result;
    }


    /**
     * 拷贝文件
     *
     * @param fromFile
     * @param toFile
     * @return
     */
    public static int CopySdcardFile(String fromFile, String toFile) {
        if (fileIsExists(toFile)) {
            return 0;
        }
        InputStream fosfrom = null;
        OutputStream fosto = null;
        try {
            fosfrom = new FileInputStream(fromFile);
            fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            closeStream(fosfrom, fosto);
        }
    }

    /**
     * save file
     *
     * @param context
     * @param fileName
     * @param buffer
     */
    public static void saveFile(Context context, String fileName, byte[] buffer) {
        GZIPOutputStream gos = null;
        try {
            FileOutputStream fos = context.openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            gos = new GZIPOutputStream(fos);
            gos.write(buffer);
            gos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(gos);
        }
    }

    public static List<String> readByLine(String targetPath) {
        List<String> lines = new ArrayList<>();
        File file = new File(targetPath);
        if (!file.exists()) return lines;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(reader);
        }
        return lines;
    }

    public static void writeByLines(List<String> lines, String outputPath, boolean isAppend) {
        if (lines == null || lines.size() == 0) return;
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPath, isAppend);
            for (String ad : lines) {
                writer.write(ad + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(writer);
        }
    }

    public static boolean writeByLine(String line, String outputPath, boolean isAppend) {
        if (TextUtils.isEmpty(line)) return false;
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPath, isAppend);
            writer.write(line + "\n");
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeStream(writer);
        }
    }

    private static void closeStream(Closeable... streams) {
        if (streams != null) {
            try {
                for (Closeable stream : streams) {
                    if (stream != null) {
                        stream.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createFileIfNotExists(String path) {
        if (!fileIsExists(path)) {
            createFile(path);
        }
    }

    /**
     * read file
     *
     * @param context
     * @param fileName
     * @return
     */
    public static byte[] readFile(Context context, String fileName) throws IOException {
        GZIPInputStream gis = null;
        byte[] buffer = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if (!new File(context.getFileStreamPath(fileName).getAbsolutePath()).exists())
                return null;
            FileInputStream fis = context.openFileInput(fileName);
            gis = new GZIPInputStream(fis);
            byte data[] = new byte[BUFFER];
            int count;
            while ((count = gis.read(data, 0, BUFFER)) != -1) {
                baos.write(data, 0, count);
            }
            buffer = baos.toByteArray();
        } catch (FileNotFoundException | StreamCorruptedException e) {
            e.printStackTrace();
        } finally {
            closeStream(gis,baos);
        }
        return buffer;
    }

    public synchronized static boolean unZipFile(String zipFilePath, String filePath) {
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            File dir = new File(filePath);
            dir.mkdirs();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    new File(filePath + File.separator + entry.getName()).mkdirs();
                    continue;
                }

                bis = new BufferedInputStream(zipFile.getInputStream(entry));
                File file = new File(filePath + File.separator + entry.getName());

                File parent = file.getParentFile();
                if (parent != null && (!parent.exists())) {
                    parent.mkdirs();
                }

                final int BUFFER = 2048;

                FileOutputStream fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos, BUFFER);

                int count;
                byte data[] = new byte[BUFFER];
                while ((count = bis.read(data, 0, BUFFER)) != -1) {
                    bos.write(data, 0, count);
                }
                bos.flush();
            }
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeStream(bis,bos);
        }
        return true;
    }

    public synchronized static void deleteFile(String fileName) {
        deleteFileOrDirectory(new File(fileName));
    }

    public synchronized static void deleteFileOrDirectory(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        }

        File[] fileList = file.listFiles();
        if (fileList == null || fileList.length == 0) {
            // the directory is empty
            file.delete();
        } else {
            for (File child : fileList) {
                deleteFileOrDirectory(child);
            }
            file.delete();
        }
    }

    /**
     * 拷贝文件
     *
     * @param sourceFile
     * @param outputFile
     */
    public static void copyFile(InputStream sourceFile, OutputStream outputFile) {
        try {
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = sourceFile.read(buffer)) >= 0) {
                outputFile.write(buffer, 0, count);
            }
            outputFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(outputFile,sourceFile);
        }
    }

    /**
     * 多文件压缩
     *
     * @param files
     * @param zippath
     * @param zipname
     */

    public static void ZipMultiFile(List<File> files, String zippath, String zipname) {
        try {
            File zipFile = new File(zippath);
            InputStream input = null;
            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

            for (int i = 0; i < files.size(); ++i) {
                input = new FileInputStream(files.get(i));
                zipOut.putNextEntry(new ZipEntry(zipname + File.separator + files.get(i).getName()));
                int temp = 0;
                while ((temp = input.read()) != -1) {
                    zipOut.write(temp);
                }
                input.close();
            }

            zipOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 取配置文件属性集合
    public static Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            // 通过流文件来进行properties文件读取的,要将文件放入到assets文件夹或者raw文件夹中
            is = context.getAssets().open(file);
            properties.load(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(is);
        }
        return properties;
    }

    public static String getProperties(Context context, String file, String key) {
        Properties properties = loadConfig(context, file);
        return properties.getProperty(key, "");
    }

    /**
     * 设置外置SDCard的默认下载路径
     *
     * @param SDCard
     * @return
     */
    public static String getDefaultPath(Context context, String SDCard) {
        if (TextUtils.isEmpty(SDCard) || context == null) {
            return null;
        }

        File file = context.getExternalFilesDir(null);
        if (file == null) {
            return null;
        }
        String appoint_path = file.getAbsolutePath();
        if (!TextUtils.isEmpty(appoint_path)) {
            appoint_path = SDCard + appoint_path.substring(Environment.getExternalStorageDirectory().getAbsolutePath
                            ().length()
                    , appoint_path.lastIndexOf(File.separator)) + LOCATION_APK;
            if (!TextUtils.isEmpty(appoint_path) && FileUtils.createFileDir(appoint_path)) {
                return appoint_path;
            } else if (!TextUtils.isEmpty(appoint_path)) {
                //创建失败时，直接返回路径，留给系统的DownloadManager创建;失败的原因系统BUG,步骤：本次应用能创建文件夹，恢复出厂设置后就不能写入创建了
                return appoint_path;
            }
        }

        return null;
    }

    public static boolean GenerateImage(String imgStr, String imgFilePath) {
        if (imgStr == null) // 图像数据为空
            return false;
        OutputStream out = null;

        try {
            out = new FileOutputStream(imgFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try {
            // Base64解码
            byte[] b = Base64.decode(imgStr, Base64.DEFAULT);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// 调整异常数据
                    b[i] += 256;
                }
            }

            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            closeStream(out);
        }
    }
}
