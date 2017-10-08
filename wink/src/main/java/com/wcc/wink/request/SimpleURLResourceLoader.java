package com.wcc.wink.request;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.wcc.wink.Wink;
import com.wcc.wink.loader.AbstractResourceLoader;
import com.wcc.wink.loader.AbstractResourceLoaderFactory;
import com.wcc.wink.loader.DirectUrlFetcher;
import com.wcc.wink.loader.GenericLoaderFactory;
import com.wcc.wink.loader.ResourceLoader;
import com.wcc.wink.loader.ResourceLoaderFactory;
import com.wcc.wink.loader.UrlFetcher;

import java.io.File;
import java.util.Locale;


/**
 * Created by wenbiao.xie on 2016/10/27.
 */

public class SimpleURLResourceLoader extends AbstractResourceLoader<SimpleURLResource> {

    /**
     * The default factory for {@link SimpleURLResourceLoader}.
     */
    public static class Factory extends AbstractResourceLoaderFactory<SimpleURLResource> {

        @Override
        public ResourceLoader<SimpleURLResource, DownloadInfo> build(Context context,
                                                                 GenericLoaderFactory factories) {
            return new SimpleURLResourceLoader(context);
        }

    }

    private final Context context;
    private SimpleURLResourceLoader(Context context) {
        this.context = context;
    }

    @Override
    public DownloadInfo createResource(SimpleURLResource model) {
        DownloadInfo di = new DownloadInfo();
        di.setKey(model.getKey());
        di.setTitle(model.getTitle());
        di.setResource(model);
        di.setUrl(model.getUrl());
        return di;
    }

    @Override
    public UrlFetcher<SimpleURLResource> getUrlFetcher(SimpleURLResource model) {
        return new DirectUrlFetcher<>();
    }

    @Override
    public File getTargetFile(SimpleURLResource model, DownloadInfo resource) {
        File outDir = Wink.get().getSetting().getSimpleResourceStorageDirectory();

        if (outDir == null) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                outDir = context.getExternalFilesDir("winks");
            }

            else {
                outDir = context.getDir("winks", Context.MODE_PRIVATE);
            }
        }

        String key = model.getKey().replace("url_", "");
        String ext = model.mExt;
        if (TextUtils.isEmpty(ext)) {
            return new File(outDir, key);
        }

        String filename = String.format(Locale.US, "%s.%s", key, ext);
        return new File(outDir, filename);
    }
}
