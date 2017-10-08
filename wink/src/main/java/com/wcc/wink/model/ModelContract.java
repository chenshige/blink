package com.wcc.wink.model;

import android.provider.BaseColumns;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public final class ModelContract {

    public interface Tables {
        String DOWNLOADS_TABLE = "downloads";
        String DOWNLOAD_INFO_VIEW = "download_info";
        String MODEL_TABLE = "models";
        String SIMPLE_TABLE = "simples";
    }

    public interface CommonColumns extends BaseColumns {
        String KEY = "key";
        String TITLE = "title";
        String URL = "url";
        String DATA = "_data";
        String SIZE = "_size";
        String NAME = "name";
    }

    public interface ModelColumns extends BaseColumns {
        String NAME = CommonColumns.NAME;
        String TABLE_NAME = "tableName";
        String MODEL_CLASS = "modelClass";
        String VERSION = "version";
    }

    public interface DownloadColumns extends CommonColumns {

        String RESOURCE_CLASS = "resourceClassName";
        String RESOURCE_KEY = "resourceKey";
        String RESOURCE_MODEL_ID = "resourceModelId";
        String STATE = "downloadState";
        String TOTAL = "totalSizeInBytes";
        String DOWNLOADED = "downloadedSizeInBytes";
        String PROGRESS = "downloadProgress";
        String MODE = "downloadMode";
        String FILE_PATH_URI = "localFilePath";

        String TRACER_START = "startTime";
        String TRACER_END = "endTime";
        String TRACER_CONNECT = "connectNetworkUsedTime";
        String TRACER_DURATION = "usedTime";
        String TRACER_AVG_SPEED = "avgSpeed";
        String TRACER_MAX_SPEED = "maxSpeed";
        String TRACER_TRYS = "tryTimes";

        String DOWNLOADER_TYPE = "downloaderType";
        String DOWNLOAD_RANGES = "ranges";

    }

    public interface URLColumns extends CommonColumns {
        String MIME = "mime";
        String EXT = "ext";
    }
}
