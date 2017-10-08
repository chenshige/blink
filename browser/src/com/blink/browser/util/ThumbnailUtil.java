package com.blink.browser.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import com.blink.browser.bean.WebTaskInfo;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThumbnailUtil implements Closeable {

    private Context mContext;
    private static ThumbnailUtil sInstance;
    private ExecutorService mExecutor;
    private int mThreadCount = 10;
    private int mThumbnailW;
    private int mThumbnailH;
    private int mMultiple = 3;
    private Bitmap mDefaultThumbnail;
    private String mDefaultPath;
    private String mOldDefaultPath;
    private boolean mFirst = true;


    private ThumbnailUtil(Context context) {
        this.mContext = context;
        init();
    }

    public static ThumbnailUtil getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThumbnailUtil(context);
        }
        return sInstance;
    }

    private void init() {
        ThreadFactory mThreadFactory = new ThreadFactory() {

            private AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable arg0) {
                // TODO Auto-generated method stub
                Thread thread = new Thread(arg0, "thumbnail thread #" + mCount.getAndIncrement());
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        };
        mExecutor = Executors.newFixedThreadPool(mThreadCount, mThreadFactory);
        mThumbnailW = DisplayUtil.getScreenWidth(mContext) / mMultiple;
        mThumbnailH = DisplayUtil.getScreenHeight(mContext) / mMultiple;
        mDefaultPath = (String) SharedPreferencesUtils.get("default_thumbnail_path", null);
    }

    public void showThumbnail(final ImageView imageView, final WebTaskInfo info, final int itemWidth) {
        if (imageView != null && info != null) {
            Thumbnailer thumbnailer = new Thumbnailer(Thumbnailer.TYPE_GET, new CallBacker() {

                @Override
                public void saveCallBack(String localFile) {
                }

                @Override
                public void getCallBack(Bitmap bitmap) {
                    info.setThumbnail(bitmap);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(bitmap);
                }
            });
            thumbnailer.setLocalFile(info.getLocalFile());
            mExecutor.submit(thumbnailer);
        }
    }

    public void showIcon(final ImageView imageView, final String iconPath) {
        if (imageView != null && iconPath != null) {
            Thumbnailer thumbnailer = new Thumbnailer(Thumbnailer.TYPE_GET, new CallBacker() {

                @Override
                public void saveCallBack(String localFile) {
                }

                @Override
                public void getCallBack(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
            thumbnailer.setLocalFile(iconPath);
            mExecutor.submit(thumbnailer);
        }
    }

    public WebTaskInfo getThumbnail(View view, final WebTaskInfo info) {
        if (info.getThumbnail() != null && !info.getThumbnail().isRecycled()) {
//            info.getThumbnail().recycle();
            info.setThumbnail(null);
            System.gc();
        }
        if (view != null) {
            if (view instanceof WebView) {
                info.setThumbnail(convertWebViewToDrawable(view));
            } else {
                info.setThumbnail(convertViewToDrawable(view));
            }
        }
        Thumbnailer thumbnailer = new Thumbnailer(Thumbnailer.TYPE_SAVE, new CallBacker() {

            @Override
            public void saveCallBack(String localFile) {
                info.setLocalFile(localFile);
                WebTaskDbUtil.update(info);
            }

            @Override
            public void getCallBack(Bitmap bitmap) {

            }
        });
        thumbnailer.setOldPath(info.getLocalFile());
        thumbnailer.setBitmap(info.getThumbnail());
        mExecutor.submit(thumbnailer);
        return info;
    }

    public void updateDefaultThumbnail(View view, final WebTaskInfo info) {
        mOldDefaultPath = mDefaultPath;
        if (mDefaultThumbnail != null && !mDefaultThumbnail.isRecycled()) {
//            mDefaultThumbnail.recycle();
            mDefaultThumbnail = null;
            System.gc();
        }
        mDefaultThumbnail = convertViewToDrawable(view);
        Thumbnailer thumbnailer = new Thumbnailer(Thumbnailer.TYPE_SAVE, new CallBacker() {

            @Override
            public void saveCallBack(String localFile) {
                mDefaultPath = localFile;
                SharedPreferencesUtils.put("default_thumbnail_path", mDefaultPath);
                info.setLocalFile(localFile);
                WebTaskDbUtil.update(info);
            }

            @Override
            public void getCallBack(Bitmap bitmap) {

            }
        });
        thumbnailer.setOldPath(mOldDefaultPath);
        thumbnailer.setBitmap(mDefaultThumbnail);
        mExecutor.submit(thumbnailer);
    }

    public void showDefaultThumbanil(final ImageView imageView, final WebTaskInfo info) {
        imageView.setScaleType(ImageView.ScaleType.FIT_START);
        if (mDefaultThumbnail != null) {
            imageView.setImageBitmap(mDefaultThumbnail);
        } else {
            Thumbnailer thumbnailer = new Thumbnailer(Thumbnailer.TYPE_GET, new CallBacker() {

                @Override
                public void saveCallBack(String localFile) {
                }

                @Override
                public void getCallBack(Bitmap bitmap) {
                    mDefaultThumbnail = bitmap;
                    imageView.setImageBitmap(bitmap);
                }
            });
            if (mDefaultPath != null && !"".equals(mDefaultPath)) {
                thumbnailer.setLocalFile(mDefaultPath);
                mExecutor.submit(thumbnailer);
                return;
            } else if (info.getLocalFile() != null && !"".equals(info.getLocalFile())) {
                mDefaultPath = info.getLocalFile();
                thumbnailer.setLocalFile(info.getLocalFile());
                mExecutor.submit(thumbnailer);
            } else {

            }
        }
    }

    public synchronized void initDefaultThumbnail(View view, WebTaskInfo info) {
        if (!mFirst) {
            return;
        }
        if (mDefaultPath == null || mDefaultPath.equals("") || !FileUtils.fileIsExists(mDefaultPath)) {
            updateDefaultThumbnail(view, info);
        }
        mFirst = false;
    }

    public boolean isShouldUpdate(WebTaskInfo info) {
        if (info != null) {
            return mDefaultThumbnail == null && (mDefaultPath == null || "".equals(mDefaultPath)) && (info.getLocalFile() == null || "".equals(info.getLocalFile()));
        }
        return false;
    }

    public Bitmap convertWebViewToDrawable(View view) {
        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheBackgroundColor(Color.WHITE);
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        view.buildDrawingCache();
        Bitmap cache = view.getDrawingCache(true);
        Bitmap bitmap = null;
        if (cache != null && cache.getWidth() > 0) {
            bitmap = Bitmap.createScaledBitmap(cache, (int) (cache.getWidth() * 0.5), (int) (cache.getHeight() * 0.5), true);
        }
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public Bitmap convertViewToDrawable(View view,float ... scale) {
        float xScale = 0.5f,yScale=0.5f ;
        if(scale != null && scale.length > 0){
            xScale = scale[0] ;
            if(scale.length == 1){
                yScale = xScale ;
            }else{
                yScale = scale[1] ;
            }
        }
        Bitmap bitmap = null;
        if (view != null) {
            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheBackgroundColor(Color.WHITE);
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            view.buildDrawingCache();
            Bitmap cache = view.getDrawingCache(true);
            if (cache != null && cache.getWidth() > 0) {
                bitmap = Bitmap.createScaledBitmap(cache, (int) (cache.getWidth() * xScale), (int) (cache.getHeight() * yScale), true);
            }
            view.setDrawingCacheEnabled(false);
        }
        return bitmap;
    }


    public void removeThumbnail(WebTaskInfo info) {
        if (info != null) {
            if (info.getThumbnail() != null && !info.getThumbnail().isRecycled()) {
//                info.getThumbnail().recycle();
                info.setThumbnail(null);
                System.gc();
            }
        }
    }

    private static class Thumbnailer implements Runnable, Closeable {

        public static int TYPE_SAVE = 1;
        public static int TYPE_GET = 2;
        private int type = TYPE_SAVE;
        private CallBacker callBacker;
        private Bitmap bitmap;
        private String localFile;
        private String oldPath;

//        public interface CallBacker {
//            void saveCallBack (String localFile);
//            void getCallBack (Bitmap bitmap);
//        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public void setLocalFile(String localFile) {
            this.localFile = localFile;
        }

        public void setOldPath(String oldPath) {
            this.oldPath = oldPath;
        }

        public Thumbnailer(int type, CallBacker callBacker) {
            this.type = type;
            this.callBacker = callBacker;
        }

        @Override
        public void run() {
            if (type == TYPE_SAVE) {
                if (bitmap != null) {
                    String picName = System.currentTimeMillis() + "";
                    FileUtils.saveBitmap(bitmap, picName);
                    if (oldPath != null && !oldPath.equals("")) {
                        FileUtils.deleteFile(oldPath);
                    }
                    if (callBacker != null) {
                        callBacker.saveCallBack(FileUtils.SDPATH + picName + ".JPEG");
                    }
                }
            } else if (type == TYPE_GET) {
                if (localFile != null && !"".equals(localFile)) {
                    if (callBacker != null) {
                        callBacker.getCallBack(FileUtils.getBitmap(localFile));
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {

        }
    }

    public String getDefaultPath() {
        return mDefaultPath;
    }

    @Override
    public void close() throws IOException {
        mExecutor.shutdown();
        mExecutor = null;
        sInstance = null;
        if (mDefaultThumbnail != null && !mDefaultThumbnail.isRecycled()) {
            mDefaultThumbnail.recycle();
            mDefaultThumbnail = null;
        }
    }

    public interface CallBacker {
        void saveCallBack(String iconPath);

        void getCallBack(Bitmap bitmap);
    }
}
