package com.wcc.wink.request;

import android.content.Context;
import android.text.TextUtils;

import com.wcc.wink.Resource;
import com.wcc.wink.loader.UrlFetcher;
import com.wcc.wink.util.Singleton;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wenbiao.xie on 2015/11/7.
 */
public class DownloaderFactory {

    public static Downloader createDownloader(Context context, WinkRequest task) {
        DownloadInfo entity = task.getEntity();
        if (entity.getResource() == null || entity.getLoader() == null)
            throw new IllegalStateException("wink lost resource or loader");

        String url = entity.getResource().getUrl();
        try {
            do {
                URL uri = new URL(url);
                String schema = uri.getProtocol().toLowerCase();
                if ("http".equals(schema) || "https".equals(schema)) {
                    return buildHttpDownloader(context, task);
                }

                DownloaderFactory factory = DownloaderFactory.get();
                if (factory.support(schema)) {
                    return factory.build(context, schema, task);
                }

                throw new RuntimeException("wink not support schema " + schema);
            } while (false);

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    static Downloader buildHttpDownloader(Context context, WinkRequest task) {
        DownloadInfo entity = task.getEntity();
        UrlFetcher<Resource> fetcher = entity.getLoader().getUrlFetcher(entity.getResource());
        if (fetcher != null) {
            return new AntiStealingLinkDownloader(task, fetcher);
        } else {
            return new OkURLDownloader(task);
        }
    }

    private final Map<String, SchemaProtocolFactory> schemaFactories;
    private final static Singleton<DownloaderFactory> _instance = new Singleton<DownloaderFactory>() {
        @Override
        protected DownloaderFactory create() {
            return new DownloaderFactory();
        }
    };

    public static DownloaderFactory get() {
        return _instance.get();
    }

    private DownloaderFactory() {
        schemaFactories = new HashMap<>();
    }

    public boolean support(String schema) {
        if (TextUtils.isEmpty(schema))
            throw new IllegalArgumentException("params schema is empty");

        return schemaFactories.containsKey(schema.toLowerCase());
    }

    public void register(SchemaProtocolFactory factory) {
        if (factory == null)
            throw new IllegalArgumentException("params schema is empty or factory is null");

        String[] schemas = factory.supportSchemas();
        if (schemas == null || schemas.length == 0)
            throw new RuntimeException("cannot register a schema factory supports none schema");

        for (String s : schemas)
            schemaFactories.put(s.toLowerCase(), factory);
    }

    public void unregister(String schema) {
        if (TextUtils.isEmpty(schema))
            throw new IllegalArgumentException("params schema is empty");

        schemaFactories.remove(schema.toLowerCase());
    }

    private Downloader build(Context context, String schema, WinkRequest request) {
        SchemaProtocolFactory factory = schemaFactories.get(schema);
        if (factory == null) {
            throw new RuntimeException("no valid downloader for schema " + schema);
        }

        return factory.build(context, request);
    }

}
