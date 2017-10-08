package com.wcc.wink;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wcc.wink.annotations.ModelTable;
import com.wcc.wink.loader.GenericLoaderFactory;
import com.wcc.wink.loader.ResourceLoader;
import com.wcc.wink.loader.ResourceLoaderFactory;
import com.wcc.wink.model.CachedModelDao;
import com.wcc.wink.model.GenericModelFactory;
import com.wcc.wink.model.KeyGenerator;
import com.wcc.wink.model.Model;
import com.wcc.wink.model.ModelDao;
import com.wcc.wink.model.ResourceModelFactory;
import com.wcc.wink.model.WinkSQLiteHelper;
import com.wcc.wink.model.SQLiteModelDao;
import com.wcc.wink.module.ManifestParser;
import com.wcc.wink.module.WinkModule;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.DownloadState;
import com.wcc.wink.request.Downloader;
import com.wcc.wink.request.DownloaderFactory;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.request.SimpleURLModelDao;
import com.wcc.wink.request.SimpleURLResource;
import com.wcc.wink.request.SimpleURLResourceLoader;
import com.wcc.wink.request.WinkRequest;
import com.wcc.wink.util.Comparators;
import com.wcc.wink.util.NetworkHelper;
import com.wcc.wink.util.Objects;
import com.wcc.wink.util.Utils;
import com.wcc.wink.util.WLog;
import com.wcc.wink.util.WizardlyRunnable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.wcc.wink.WinkError.NetworkError;

/**
 * 下载管理统一接口类
 * Created by wenbiao.xie on 2015/11/5.
 */
public final class Wink {

    private static final String TAG = "Wink";
    private static volatile Wink _instance = null;

    private final HashMap<String, WinkRequest> mWaitingTasks;
    private final Map<String, WinkRequest> mRunningTasks;

    private final Map<String, WinkRequest> mFinishedTasks;
    private final Map<String, WinkRequest> mUnFinishedTasks;
    private final ModelDao<String, DownloadInfo> mModelDao;

    private final Context mContext;
    private ExecutorService mExecutorService;
    private Handler mHandler = null;
    private boolean mAllowSchedule = true;
    private int mSilentRunningTaskCount = 0;
    private GenericLoaderFactory mGenericFactory;
    private GenericModelFactory mModelFactory;
    private HandlerThread mScheduleThread;
    private WinkNotificationController mController;
    private WinkCallback mCallback;
    private WinkStatHandler mStatHandler;
    private Handler mUiHandler;
    private final WinkSetting mSetting;
    private WeakReference<SpeedWatcher> mWatcherRef = new WeakReference<SpeedWatcher>(null);

    private Wink(Context context) {
        this(context, null);
    }

    Wink(Context context, WinkSetting setting) {
        this.mContext = context.getApplicationContext();
        if (setting == null) {
            mSetting = WinkSetting.getDefault();
        } else {
            mSetting = new WinkSetting(setting);
        }

        int max = mSetting.getMaxRunningSize() + mSetting.getMaxSilentRunningSize();
        this.mWaitingTasks = new HashMap<>(10);
        this.mRunningTasks = new HashMap<>(max);
        this.mUnFinishedTasks = new HashMap<>(10);
        this.mFinishedTasks = new HashMap<>(10);
        this.mExecutorService = Executors.newFixedThreadPool(max,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, WinkSetting.DEFAULT_THREAD_NAME);
                    }
                });

        this.mUiHandler = new Handler(Looper.getMainLooper());
        this.mHandler = new DownloadHandler(Looper.getMainLooper());

        // 数据库初始化
        WinkSQLiteHelper.initDatabase(context, setting.getSQLInterceptor());
        this.mGenericFactory = new GenericLoaderFactory(mContext);
        this.mModelFactory = new GenericModelFactory(mContext);

        this.mModelDao = new CachedModelDao<>(new SQLiteModelDao(mContext,
                mGenericFactory, mModelFactory), new KeyGenerator<String, DownloadInfo>() {
            @Override
            public String key(DownloadInfo downloadInfo) {
                return downloadInfo.getKey();
            }
        });
    }

    public static Wink initInstance(Context context, WinkModule... modules) {
        if (_instance == null) {
            synchronized (Wink.class) {
                if (_instance == null) {
                    Context applicationContext = context.getApplicationContext();
                    ManifestParser parser = new ManifestParser(applicationContext);
                    List<WinkModule> configModules = parser.parse();

                    WinkBuilder builder = new WinkBuilder(applicationContext);
                    for (WinkModule module : configModules) {
                        module.applyOptions(applicationContext, builder);
                    }

                    for (WinkModule module : modules) {
                        module.applyOptions(applicationContext, builder);
                    }

                    NetworkHelper.sharedHelper().registerNetworkSensor(applicationContext,
                            builder.getNetworkSensor());

                    Wink wink = builder.build();
                    for (WinkModule module : configModules) {
                        module.registerComponents(applicationContext, wink);
                    }

                    for (WinkModule module : modules) {
                        module.registerComponents(applicationContext, wink);
                    }
                    // 注入支持URL直接下载功能
                    wink.registerLoaderFactory(SimpleURLResource.class, new SimpleURLResourceLoader.Factory());
                    wink.registerModelFactory(SimpleURLResource.class, new SimpleURLModelDao.Factory());

                    wink.init();
                    _instance = wink;
                }
            }
        }
        return _instance;
    }

    public static Wink get() {
        return _instance;
    }

    public WinkSetting getSetting() {
        return mSetting;
    }

    protected void init() {
        mScheduleThread = new DownloadHandlerThread("wink", this);
        mScheduleThread.start();

        setRunLoop(mScheduleThread.getLooper());
    }

    public <E extends Resource> void registerLoaderFactory(Class<E> clz,
                                                           ResourceLoaderFactory<E, DownloadInfo> factory) {
        mGenericFactory.register(clz, DownloadInfo.class, factory);
    }

    public <E extends Resource> void registerModelFactory(Class<E> resourceClass,
                                                          ResourceModelFactory<String, E> factory) {

        Class cls = factory.getClass();
        if (!cls.isAnnotationPresent(ModelTable.class)) {
            throw new IllegalArgumentException("factory not annotate with ModelTable");
        }

        ModelTable table = (ModelTable) cls.getAnnotation(ModelTable.class);
        if (!resourceClass.equals(table.model())) {
            throw new IllegalArgumentException("resourceClass not match with factory's annotation ModelTable");
        }

        Model model = Model.fromAnnotation(table);
        Model.Dao dao = Model.get();
        boolean saveOrUpdate;

        if (!dao.exists(model)) {
            saveOrUpdate = true;
            final SQLiteDatabase db = WinkSQLiteHelper.sharedHelper().getWritableDatabase();
            factory.onCreate(db);
        } else {
            Model m = dao.get(table.table());
            if (m.version < model.version) {
                final SQLiteDatabase db = WinkSQLiteHelper.sharedHelper().getWritableDatabase();
                factory.onUpgrade(db, m.version, model.version);
            }

            saveOrUpdate = model.sameTo(m);
        }

        if (saveOrUpdate) {
            dao.saveOrUpdate(model);
        }

        mModelFactory.register(String.class, resourceClass, factory);
    }

    public void setNotificationController(WinkNotificationController controller) {
        this.mController = controller;
    }

    public void setCallback(WinkCallback callback) {
        this.mCallback = callback;
    }

    public void setStatHandler(WinkStatHandler handler) {
        this.mStatHandler = handler;
    }

    public void setSpeedWatcher(SpeedWatcher watcher) {
        this.mWatcherRef = new WeakReference<SpeedWatcher>(watcher);
    }

    void setRunLoop(Looper loop) {
        Looper old = mHandler.getLooper();
        if (old == loop) {
            return;
        }

        // 切换Looper
        if (mScheduleThread != null && old == mScheduleThread.getLooper()) {
            mScheduleThread.quit();
            mScheduleThread = null;
        }

        mHandler = new DownloadHandler(loop);
    }

    private WinkRequest createTask(Resource entity) {
        DownloadInfo info = mModelDao.get(entity.getKey());
        boolean first = false;
        if (info == null) {
            if (entity instanceof DownloadInfo) {
                info = (DownloadInfo) entity;
            } else {
                ResourceLoader<Resource, DownloadInfo> loader = (ResourceLoader<Resource, DownloadInfo>)
                        mGenericFactory.buildResourceLoader(entity.getClass(), DownloadInfo.class);

                info = loader.createResource(entity);
                info.setLoader(loader);
                info.setResource(entity);
                first = true;
            }
        }

        WinkRequest task = new WinkRequest(mContext, info);
        task.setFirstDownloadForEntity(first);
        return task;
    }

    public int wink(String url) {
        return wink(new SimpleURLResource(url), DownloadMode.NORMAL);
    }

    public int wink(String url, String title) {
        return wink(new SimpleURLResource(url, title), DownloadMode.NORMAL);
    }

    public int wink(String url, String title, String mimetype) {
        return wink(new SimpleURLResource(url, title, mimetype), DownloadMode.NORMAL);
    }

    public int wink(String url, String title, String mimetype, String referer) {
        return wink(new SimpleURLResource(url, title, mimetype, referer), DownloadMode.NORMAL);
    }

    public int wink(Resource entity) {
        return wink(entity, DownloadMode.NORMAL);
    }

    public synchronized int wink(Resource entity, int mode) {
        WLog.v(TAG, "wink mode = %d, entity key = %s", mode, entity.getKey());
        if (!mGenericFactory.supportResourceLoader(entity.getClass(), DownloadInfo.class)) {
            throw new IllegalArgumentException("not support to wink resource " + entity.getClass());
        }

        if (TextUtils.isEmpty(entity.getKey()))
            return WinkError.INVALID;

        // 增加SD卡空间检测与缓存处理逻辑
        if (Utils.getAvailableExternalStorageSize() < mSetting.getMinimumSdSpace()) {
            WLog.w(TAG, "wink no available ExternalMemorySize");
            if (!DownloadMode.shouldSilent(mode))
                showErrorWithUI(WinkError.INSUFFICIENT_SPACE, entity);

            return WinkError.INSUFFICIENT_SPACE;
        }

        if (!NetworkHelper.sharedHelper().isNetworkAvailable()) {
            WLog.w(TAG, "wink no available network");
            if (!DownloadMode.shouldSilent(mode))
                showErrorWithUI(NetworkError.NO_AVAILABLE_NETWORK, entity);

            return NetworkError.NO_AVAILABLE_NETWORK;
        }

        // 检测是否存在该实体的下载资源
        WinkRequest dt = findByEntity(entity.getKey());
        if (dt != null) {
            final DownloadState state = dt.getState();
            if (state.equals(DownloadState.active) ||
                    state.equals(DownloadState.completed) ||
                    state.equals(DownloadState.ready)) {
                updateDownloadMode(dt, mode);
            } else {
                enqueue(dt, mode);
            }

            return WinkError.EXIST;
        }

        DownloadInfo di = mModelDao.get(entity.getKey());
        if (di != null && di.getDownloadState() == ResourceStatus.DOWNLOADED) {
            File file = new File(di.getLocalFilePath());
            if (!file.exists() || file.length() != di.getTotalSizeInBytes()) {
                mModelDao.delete(di);
            } else {
//                di.setKey();
                return WinkError.ALREADY_COMPLETED;
            }
        }

        enqueue(entity, mode);
        return WinkError.SUCCESS;
    }

    public void stop(String key) {
        Objects.requireNonNull(key, "key should not be null");

        WinkRequest task = findUnfinishedByEntity(key);
        if (task == null)
            return;

        // TODO: 2016/6/15 add stat for stop wink
        asyncCancel(task);
    }

    public Collection<DownloadInfo> getAllResources() {
        return mModelDao.all();
    }

    public List<DownloadInfo> getDownloadedResources() {
        Collection<DownloadInfo> caches = mModelDao.all();
        if (Utils.isEmpty(caches))
            return null;

        List<DownloadInfo> manuals = new ArrayList<>();
        for (DownloadInfo di : caches) {
            final int state = di.getDownloadState();
            if (state == ResourceStatus.DOWNLOADED) {
                if (!DownloadMode.shouldSilent(di.getDownloadMode())) {
                    manuals.add(di);
                }
            }
        }
        return manuals;
    }

    public List<DownloadInfo> getDownloadingResources() {
        Collection<DownloadInfo> caches = mModelDao.all();
        Collection<WinkRequest> unfinish = mUnFinishedTasks.values();
        boolean cacheNotEmpty = !Utils.isEmpty(caches);
        boolean unfinishNotEmpty = !Utils.isEmpty(unfinish);
        WLog.d(TAG, "getDownloadingResources cacheNotEmpty: %b, unfinishNotEmpty: %b", cacheNotEmpty,
                unfinishNotEmpty);
        if (!cacheNotEmpty && !unfinishNotEmpty)
            return null;

        ArrayList<DownloadInfo> all = new ArrayList<>();
        if (cacheNotEmpty) {
            all.addAll(caches);
        }

        if (unfinishNotEmpty) {
            if (cacheNotEmpty) {
                for (WinkRequest wr : unfinish) {
                    DownloadInfo di = wr.getEntity();
                    if (!caches.contains(di)) {
                        all.add(di);
                    }
                }
            } else {
                for (WinkRequest wr : unfinish) {
                    DownloadInfo di = wr.getEntity();
                    all.add(di);
                }
            }
        }

        List<DownloadInfo> manuals = new ArrayList<>();
        for (DownloadInfo di : all) {

            final int state = di.getDownloadState();
            if (state != ResourceStatus.DOWNLOADED && state != ResourceStatus.DELETED) {

                if (!DownloadMode.shouldSilent(di.getDownloadMode())) {
                    manuals.add(di);
                }
            }
        }

        return manuals;
    }

    public synchronized void resumeFailed() {
        // 非WIFI环境不启动自动恢复下载任务
        if (!NetworkHelper.sharedHelper().isWifiActive())
            return;

        Collection<? extends DownloadInfo> caches = mModelDao.all();
        List<DownloadInfo> manuals = new ArrayList<>();
        List<DownloadInfo> autos = new ArrayList<>();
        boolean got = false;
        for (DownloadInfo di : caches) {
            final int state = di.getDownloadState();

            if (state == ResourceStatus.DOWNLOAD_FAILED) {

                if (DownloadMode.shouldSilent(di.getDownloadMode())) {
                    autos.add(di);
                } else {
                    manuals.add(di);
                }

                got = true;
            }
        }

        if (!got)
            return;

        Comparator<DownloadInfo> comp = new Comparator<DownloadInfo>() {
            @Override
            public int compare(DownloadInfo lhs, DownloadInfo rhs) {
                int value = Comparators.compare(rhs.getDownloadMode(), lhs.getDownloadMode());
                if (value == 0) {
                    value = Comparators.compare(lhs.getTracer().startTime,
                            rhs.getTracer().startTime);
                }
                return value;
            }
        };


        if (!manuals.isEmpty()) {
            Collections.sort(manuals, comp);
            for (DownloadInfo di : manuals) {
                wink(di.getResource(), di.getDownloadMode());
            }
        }

        if (!autos.isEmpty()) {
            Collections.sort(autos, comp);
            for (DownloadInfo di : autos) {
                wink(di.getResource(), di.getDownloadMode());
            }
        }
    }

    private boolean isCurrentIoThread() {
        return (Thread.currentThread() == mHandler.getLooper().getThread());
    }

    private void updateDownloadMode(WinkRequest task, int newMode) {

        if (!isCurrentIoThread()) {
            Message msg = mHandler.obtainMessage(MSG_UPDATE_MODE, newMode, 0, task);
            msg.sendToTarget();
            return;
        }

        int old = task.getEntity().getDownloadMode();
        if (old == newMode)
            return;

        boolean oldSilent = DownloadMode.shouldSilent(old);
        boolean newSilent = DownloadMode.shouldSilent(newMode);
        // 用户触发的下载任务，不能更改为静默下载任务
        if (!oldSilent && newSilent)
            return;

        task.getEntity().setDownloadMode(newMode);
        // 静默下载任务，更改为用户触发任务时，会影响静默任务数量
        if (oldSilent && !newSilent) {
            if (task.getState().equals(DownloadState.active)) {
                if (mSilentRunningTaskCount > 0)
                    mSilentRunningTaskCount--;
            }
        }
    }

    private void asyncCancel(WinkRequest task) {
        DownloadInfo entity = task.getEntity();
        entity.setDownloadState(ResourceStatus.PAUSE);
        notifyStatusChanged(DownloadState.stopped, task);

        Message msg = mHandler.obtainMessage(MSG_CANCEL, task);
        msg.sendToTarget();

        final WinkNotificationController controller = mController;
        if (controller != null) {
            mUiHandler.post(new WizardlyRunnable<DownloadInfo>(entity) {
                @Override
                protected void doRun(DownloadInfo downloadInfo) {
                    controller.showPauseNotification(downloadInfo);
                }
            });
        }
    }

    private void cancelTask(WinkRequest task) {
        if (task.getState().equals(DownloadState.ready)) {
            onCancel(task);
            deleteEntity(task.requestDelete(), task.getEntity());
        } else if (task.getState().equals(DownloadState.active)) {
            Downloader downloader = task.getDownloader();
            downloader.cancel(task.requestDelete());
        }
    }

    private void enqueue(Resource entity, int mode) {
        if (!isCurrentIoThread()) {
            Message msg = mHandler.obtainMessage(MSG_QUEUE, mode, 0, entity);
            msg.sendToTarget();
            return;
        }

        boolean in;
        synchronized (mUnFinishedTasks) {
            in = mUnFinishedTasks.containsKey(entity.getKey());
            if (in) {
                WLog.w(TAG, "already enqueue", entity.getTitle());
                return;
            }
        }

        WinkRequest task = createTask(entity);
        onAdd(task);
        enqueue(task, mode);
    }

    private void enqueue(WinkRequest task, int mode) {
        if (!isCurrentIoThread()) {
            Message msg = mHandler.obtainMessage(MSG_QUEUE, mode, 1, task);
            msg.sendToTarget();
            return;
        }

        boolean in;
        synchronized (mUnFinishedTasks) {
            in = mUnFinishedTasks.containsKey(task.getEntity().getKey());
            if (!in) {
                WLog.w(TAG, " it may be already cancelled!", task.getEntity().getTitle());
                return;
            }
        }

        task.getEntity().setDownloadMode(mode);
        task.onRequestStart();

        onWaiting(needNotifyWaiting(), task);
        if (!task.requestDelete()) {
            mModelDao.saveOrUpdate(task.getEntity());
        }

        start();
        final WinkNotificationController controller = mController;
        if (controller != null) {
            mUiHandler.post(new WizardlyRunnable<DownloadInfo>(task.getEntity()) {
                @Override
                protected void doRun(DownloadInfo downloadInfo) {
                    controller.showProgressNotification(downloadInfo,
                            downloadInfo.getDownloadProgress());
                }
            });
        }
    }

    public synchronized void start() {
        mAllowSchedule = true;
        postSchedule();
    }

    private void deleteEntity(boolean deletefile, DownloadInfo entity) {
        if (!deletefile)
            return;

        mModelDao.delete(entity);
        WLog.i(TAG, "delete app %s from wink info caches", entity.getTitle());
        if (!TextUtils.isEmpty(entity.getLocalFilePath())) {
            File file = new File(entity.getLocalFilePath());
            File cacheFile = Utils.getTempFile(file);
            if (cacheFile.exists()) {
                boolean ret = cacheFile.delete();
                WLog.i(TAG, "delete cache file %s for %s, result: %b", cacheFile.getAbsolutePath(),
                        entity.getTitle(), ret);
            } else if (file.exists()) {
                boolean ret = file.delete();
                WLog.i(TAG, "delete file %s for %s, result: %b", file.getAbsolutePath(), entity.getTitle(), ret);
            }
        }
    }

    public void delete(String key, boolean deletefile) {
        WLog.v(TAG, "delete download info %s, delete file: %b", key, deletefile);
        WinkRequest task = findUnfinishedByEntity(key);
        if (task != null) {
            asyncDelete(task, deletefile, true);
        } else {
            DownloadInfo di = mModelDao.get(key);
            if (di != null) {
                deleteEntity(deletefile, di);
            }
        }
    }

    private void clearAllUnfinishedTasks() {
        if (mUnFinishedTasks.size() == 0) {
            return;
        }

        Collection<WinkRequest> tasks = mUnFinishedTasks.values();

        for (WinkRequest dt : tasks) {
            deleteTask(dt, true, false);
        }

        mUnFinishedTasks.clear();
        final WinkCallback callback = mCallback;
        if (callback == null) return;

        DownloadInfo[] array = new DownloadInfo[tasks.size()];
        int i = 0;
        for (WinkRequest dt : tasks) {
            array[i++] = dt.getEntity();
        }

        callback.onRemove(array);
    }

    private void asyncSave(WinkRequest dt) {
        Message msg = mHandler.obtainMessage(MSG_SAVEORUPDATE, dt);
        msg.sendToTarget();
    }

    public void clear() {
        if (!isCurrentIoThread()) {
            Message msg = mHandler.obtainMessage(MSG_CLEAR);
            msg.sendToTarget();
            return;
        }

        clearAllUnfinishedTasks();
    }

    private void asyncDelete(WinkRequest dt, boolean deletefile, boolean notify) {
        Message msg = mHandler.obtainMessage(MSG_DELETE, dt);
        msg.arg1 = (deletefile ? 1 : 0);
        msg.arg2 = (notify ? 1 : 0);
        msg.sendToTarget();
    }

    private void postSchedule() {
        if (!mAllowSchedule)
            return;

        mHandler.removeMessages(MSG_SCHEDULE);
        mHandler.sendEmptyMessageDelayed(MSG_SCHEDULE, 100);
    }

    public int downloading() {
        synchronized (mUnFinishedTasks) {
            return mUnFinishedTasks.size();
        }
    }

    public int failed() {
        Collection<? extends DownloadInfo> caches = mModelDao.all();
        int count = 0;
        for (DownloadInfo di : caches) {
            final int state = di.getDownloadState();
            if (state == ResourceStatus.DOWNLOAD_FAILED) {

                if (!DownloadMode.shouldSilent(di.getDownloadMode())) {
                    count++;
                }
            }
        }

        return count;
    }

    private WinkRequest findUnfinishedByEntity(String eid) {
        synchronized (mUnFinishedTasks) {
            WinkRequest dt = mUnFinishedTasks.get(eid);
            return dt;
        }

    }

    private WinkRequest findFinishedByEntity(String eid) {
        synchronized (mFinishedTasks) {
            WinkRequest dt = mFinishedTasks.get(eid);
            return dt;
        }
    }

    private WinkRequest findByEntity(String eid) {
        WinkRequest dt = null;
        dt = findUnfinishedByEntity(eid);

        if (dt == null) {
            dt = findFinishedByEntity(eid);
        }

        return dt;
    }

    private void onAdd(WinkRequest task) {
        synchronized (mUnFinishedTasks) {
            mUnFinishedTasks.put(task.getEntity().getKey(), task);
        }

        final WinkCallback callback = mCallback;
        if (callback != null)
            callback.onAdd(task.getEntity());
    }

    private boolean needNotifyWaiting() {
        return mWaitingTasks.size() > 0 || mRunningTasks.size() >= mSetting.getMaxRunningSize();
    }

    private void onRunning(WinkRequest task) {
        WLog.v(TAG, "onRunning task = %s", task.getEntity().getTitle());
        DownloadInfo entity = task.getEntity();
        entity.setDownloadState(ResourceStatus.DOWNLOADING);

        mWaitingTasks.remove(task.getEntity().getKey());
        task.setPriority(WinkRequest.PRIORITY_ACTIVE_START);
        task.setState(DownloadState.active);
        mRunningTasks.put(task.getEntity().getKey(), task);
        notifyStatusChanged(DownloadState.active, task);
        // 有任务运行，开启速度计算心跳
        tickSpeed();
    }

    private void onWaiting(boolean needNotify, WinkRequest... tasks) {
        WLog.v(TAG, "onWaiting needNotify = %b", needNotify);
        for (int i = 0; i < tasks.length; i++) {
            WinkRequest task = tasks[i];
            mRunningTasks.remove(task.getEntity().getKey());
            task.setPriority(WinkRequest.PRIORITY_CANDIDATE);
            task.setState(DownloadState.ready);
            task.getEntity().setDownloadState(ResourceStatus.WAIT);
            mWaitingTasks.put(task.getEntity().getKey(), task);
        }

        // 等待状态需要同步到数据库，以保持下一次重取数据状态正常
        if (needNotify) {
            notifyStatusChanged(DownloadState.ready, tasks);
        }
    }

    private void onCancel(WinkRequest task) {
        WLog.v(TAG, "onCancel task = %s", task.getEntity().getTitle());

        onStopped((task.getForceStopCause() != NetworkError.SUCCESS ?
                        task.getForceStopCause() : NetworkError.CANCEL),
                task);
    }

    private void showErrorWithUI(final int cause, Resource entity) {
        final WinkNotificationController controller = mController;
        if (controller != null) {
            mUiHandler.post(new WizardlyRunnable<Resource>(entity) {
                @Override
                protected void doRun(Resource entity) {
                    controller.showErrorNotification(entity, cause);
                }
            });
        }
    }

    private void showErrorWithUI(final int cause, Collection<? extends Resource> entities) {
        final WinkNotificationController controller = mController;
        if (controller != null && entities != null && !entities.isEmpty()) {
            mUiHandler.post(new WizardlyRunnable<Collection<? extends Resource>>(entities) {
                @Override
                protected void doRun(Collection<? extends Resource> entities) {
                    for (Resource r : entities)
                        controller.showErrorNotification(r, cause);
                }
            });
        }
    }

    private void onStopped(int cause, WinkRequest... tasks) {
        WLog.v(TAG, "onStopped cause = %d", cause);
        DownloadState newState = (cause == NetworkError.CANCEL ? DownloadState.stopped : DownloadState.paused);
        int state = (cause == NetworkError.CANCEL ? ResourceStatus.PAUSE : ResourceStatus.DOWNLOAD_FAILED);

        for (int i = 0; i < tasks.length; i++) {
            WinkRequest task = tasks[i];
            String key = task.getEntity().getKey();
            boolean running = (mRunningTasks.get(key) != null);
            if (running) {
                mRunningTasks.remove(key);
            } else {
                mWaitingTasks.remove(key);
            }

            task.setPriority(WinkRequest.PRIORITY_DEFAULT);
            task.setState(newState);
            task.detach();
            task.getEntity().setDownloadState(state);

            // 删除
            if ((cause >= WinkError.DELETE_RANGE_START && cause <= WinkError.DELETE_RANGE_MAX)
                    || task.requestDelete()) {
                task.getEntity().setDownloadState(ResourceStatus.INIT);
                task.setState(DownloadState.deleted);
            }
            task.onRequestEnd(cause);
            mUnFinishedTasks.remove(key);

            if (running && task.shouldSilent() && mSilentRunningTaskCount > 0) {
                mSilentRunningTaskCount--;
            }
        }

        if (tasks.length == 1) {

            WinkRequest dt = tasks[0];
            if (!dt.shouldSilent())
                showErrorWithUI(cause, dt.getEntity());

            if (dt.getState().equals(DownloadState.deleted)) {
                mModelDao.delete(dt.getEntity());
            } else {
                mModelDao.saveOrUpdate(dt.getEntity());
            }

            // TODO: 2016/6/14 add stat for stop
            stat(dt);

        } else {
            List<DownloadInfo> entities = new ArrayList<>(tasks.length);
            List<DownloadInfo> deletes = new ArrayList<>(tasks.length);

            for (WinkRequest dt : tasks) {
                if (dt.getState().equals(DownloadState.deleted)) {
                    deletes.add(dt.getEntity());
                } else {
                    entities.add(dt.getEntity());
                }
            }

            if (!Utils.isEmpty(deletes))
                mModelDao.deleteAll(deletes);

            if (!Utils.isEmpty(entities))
                mModelDao.saveOrUpdateAll(entities);

            // TODO: 2016/6/14 add stat for stop
            stat(tasks);
        }

        notifyStatusChanged(newState, cause, tasks);

        // 进入下一次调度
        postSchedule();

        // 没有正在运行的下载任务时，停止计算速度的心跳
        if (mRunningTasks.isEmpty()) {
            stopTickSpeed();
        }
    }
    /*
    private void onDownloadCompleted(WinkRequest task) {
        WLog.v(TAG, "onDownloadCompleted for task %s", task.getEntity().getTitle());
        DownloadInfo entity = task.getEntity();
        task.onRequestEnd(NetworkError.SUCCESS);

        if (!task.shouldSilent()) {
            // TODO: 2016/6/14 add wink history
        }
        // 统计下载完成
        // TODO: 2016/6/14 add stat wink complete

        if (!task.requestDelete())
            mModelDao.saveOrUpdate(entity);

        notifyStatusChanged(DownloadState.active, task);
    } */

    private void stat(WinkRequest request) {
        final WinkStatHandler handler = mStatHandler;
        if (handler == null)
            return;

        handler.handle(request, request.mStat);
    }

    private void stat(WinkRequest[] tasks) {
        if (mStatHandler == null)
            return;
        for (WinkRequest wr : tasks)
            stat(wr);
    }

    private void onCompleted(WinkRequest task) {
        WLog.v(TAG, "onCompleted for task %s", task.getEntity().getTitle());
        task.onRequestEnd(NetworkError.SUCCESS);
        DownloadInfo entity = task.getEntity();
        mRunningTasks.remove(entity.getKey());
        task.setState(DownloadState.completed);
        task.setPriority(WinkRequest.PRIORITY_DEFAULT);
        task.detach();

        entity.setDownloadState(ResourceStatus.DOWNLOADED);
        if (!task.requestDelete())
            mModelDao.saveOrUpdate(entity);

        synchronized (mUnFinishedTasks) {
            mUnFinishedTasks.remove(entity.getKey());
        }

        if (task.shouldSilent() && mSilentRunningTaskCount > 0) {
            mSilentRunningTaskCount--;
        }

        // add stat
        stat(task);

        if (!task.shouldSilent()) {
            final WinkNotificationController controller = mController;
            if (controller != null) {
                mUiHandler.post(new WizardlyRunnable<DownloadInfo>(task.getEntity()) {
                    @Override
                    protected void doRun(DownloadInfo downloadInfo) {
                        controller.showCompletedNotification(downloadInfo);
                    }
                });
            }
        }

        notifyStatusChanged(DownloadState.completed, task);
        // 进入下一次调度
        postSchedule();
        // 没有正在运行的下载任务时，停止计算速度的心跳
        if (mRunningTasks.isEmpty()) {
            stopTickSpeed();
        }
    }

    private void deleteTask(WinkRequest task, boolean deletefile, boolean notify) {
        if (deletefile) {
            task.deleteIt();
        }

        mModelDao.delete(task.getEntity());
        cancelTask(task);

        if (notify) {
            final WinkCallback callback = mCallback;
            if (callback != null)
                callback.onRemove(task.getEntity());

            final WinkNotificationController controller = mController;
            if (controller != null) {
                mUiHandler.post(new WizardlyRunnable<DownloadInfo>(task.getEntity()) {
                    @Override
                    protected void doRun(DownloadInfo downloadInfo) {
                        controller.showDeletedNotification(downloadInfo);
                    }
                });
            }

        }
    }

    private void notifyProgressEvent(final int p, WinkRequest task) {
        WLog.v(TAG, "notifyProgressEvent");
        DownloadInfo entity = task.getEntity();
        if (WLog.isDebug()) {
            WLog.i(TAG, "download progress: %d%%, current: %d, total: %d, cs: %s/s, as: %s/s, ms: %s/s", p,
                    entity.getDownloadedSizeInBytes(), entity.getTotalSizeInBytes(),
                    Utils.convertFileSize(entity.getTracer().currentSpeed),
                    Utils.convertFileSize(entity.getTracer().avgSpeed),
                    Utils.convertFileSize(entity.getTracer().maxSpeed));
        }

        if (task.shouldSilent()) {
            return;
        }

        final WinkCallback callback = mCallback;
        if (callback != null)
            callback.onProgressChanged(entity);

        final WinkNotificationController controller = mController;
        if (controller != null) {
            mUiHandler.post(new WizardlyRunnable<DownloadInfo>(entity) {
                @Override
                protected void doRun(DownloadInfo downloadInfo) {
                    controller.showProgressNotification(downloadInfo, p);
                }
            });
        }
    }

    private void notifyStatusChanged(DownloadState status, int extra, WinkRequest... tasks) {
        if (tasks == null || tasks.length == 0)
            return;

        for (WinkRequest task : tasks) {
            DownloadInfo entity = task.getEntity();
            if (!task.shouldSilent()) {
                final WinkCallback callback = mCallback;
                if (callback != null)
                    callback.onStatusChanged(entity, status);
            }
        }
    }

    private void notifyStatusChanged(DownloadState status, WinkRequest... tasks) {
        notifyStatusChanged(status, 0, tasks);
    }

    private WinkRequest next(List<WinkRequest> tasks) {
        if (Utils.isEmpty(tasks))
            return null;

        Comparator<WinkRequest> comparator = new Comparator<WinkRequest>() {
            @Override
            public int compare(WinkRequest lhs, WinkRequest rhs) {
                return lhs.compareTo(rhs);
            }
        };

        WinkRequest dt = Collections.min(tasks, comparator);
        return dt;
    }

    private WinkRequest nextPriorityly(List<WinkRequest> tasks) {
        if (Utils.isEmpty(tasks))
            return null;

        Comparator<WinkRequest> comparator = new Comparator<WinkRequest>() {
            @Override
            public int compare(WinkRequest lhs, WinkRequest rhs) {
                int lm = lhs.getEntity().getDownloadMode();
                int rm = rhs.getEntity().getDownloadMode();

                int v = Comparators.compare(lm, rm);

                if (v == 0) {
                    return lhs.compareTo(rhs);
                }

                return v;
            }
        };

        WinkRequest dt = Collections.min(tasks, comparator);
        return dt;
    }

    public void stopAll() {
        stopAll(NetworkError.CANCEL);
    }

    private void stopAll(int cause) {

        if (!isCurrentIoThread()) {
            Message msg = mHandler.obtainMessage(MSG_STOPALL, cause);
            msg.sendToTarget();
            return;
        }

        Collection<WinkRequest> all = null;
        synchronized (mUnFinishedTasks) {
            if (mUnFinishedTasks.size() == 0)
                return;

            all = new ArrayList<>(mUnFinishedTasks.values());
        }

        Collection<Resource> entities = new ArrayList<>();
        for (WinkRequest task : all) {
            if (!task.shouldSilent()) {
                entities.add(task.getEntity());
                break;
            }
        }

        if (!entities.isEmpty())
            showErrorWithUI(cause, entities);

        mAllowSchedule = false;     // 临时禁止调度, 以便顺利完成关闭所有任务
        if (mWaitingTasks.size() > 0) {
            Collection<WinkRequest> tasks = mWaitingTasks.values();
            onStopped(cause, tasks.toArray(new WinkRequest[0]));
        }

        if (mRunningTasks.size() > 0) {
            Collection<WinkRequest> tasks = new ArrayList<>(mRunningTasks.values());
            if (!Utils.isEmpty(tasks)) {
                for (WinkRequest task : tasks) {
                    task.setForceStopCause(cause);
                    Downloader downloader = task.getDownloader();
                    if (downloader != null)
                        downloader.cancel(false);
                }
            }
        }
    }

    private void schedulePriorityly() {
        WLog.v(TAG, "schedulePriorityly");
        final int maxRunning = mSetting.getMaxRunningSize();
        final int maxSilentRunning = mSetting.getMaxSilentRunningSize();
        if ((mSilentRunningTaskCount >= maxSilentRunning
                && (mRunningTasks.size() - mSilentRunningTaskCount) >= maxRunning)
                || mWaitingTasks.size() == 0)
            return;

        // 无网络
        if (!NetworkHelper.sharedHelper().isNetworkAvailable()) {
            WLog.w(TAG, "schedule wink no available network");
            stopAll(NetworkError.NO_AVAILABLE_NETWORK);
            return;
        }
//
//        // 增加SD卡空间检测与缓存处理逻辑
        if (Utils.getAvailableExternalStorageSize() < mSetting.getMinimumSdSpace()) {
            stopAll(WinkError.INSUFFICIENT_SPACE);
            return;
        }

        List<WinkRequest> waitingTasks = new ArrayList<>(mWaitingTasks.values());
        WinkRequest dt = nextPriorityly(waitingTasks);

        do {
            if (dt == null)
                return;

            // 状态校验，只有进入等待状态才能调度
            if (dt.getState().equals(DownloadState.ready)) {
                break;
            }

            waitingTasks.remove(dt);
            dt = nextPriorityly(waitingTasks);

        } while (true);


        if (dt.shouldSilent() && !NetworkHelper.sharedHelper().isWifiActive()) {
            WLog.w(TAG, "pause the wink for none wifi");
            onStopped(NetworkError.NO_AVAILABLE_NETWORK, dt);
            return;
        }

        if (dt.shouldSilent()) {
            if (mSilentRunningTaskCount >= maxSilentRunning) {
                WLog.w(TAG, "pause the silent wink for limit");
                return;
            }
            mSilentRunningTaskCount++;
        } else if ((mRunningTasks.size() - mSilentRunningTaskCount) >= maxRunning) {
            WLog.w(TAG, "pause the manual wink for limit");
            return;
        }

        Downloader downloader = dt.getDownloader();
        try {
            if (downloader == null) {
                downloader = DownloaderFactory.createDownloader(mContext, dt);
            }
        } catch (RuntimeException e) {
            WLog.printStackTrace(e);
        }

        if (downloader != null) {
            dt.attach(downloader);
            downloader.setOnDownloadListener(mOnDownloadListener);

            onRunning(dt);
            mExecutorService.submit(new DownloaderWrap(downloader));
            if (waitingTasks.size() > 0) {
                postSchedule();
            }
        } else {
            WLog.w(TAG, "cannot create downloader for this request!!");
            onStopped(NetworkError.SCHEME_NOT_SUPPORT, dt);
        }
    }

    private void onDownloadEvent(WinkRequest task, int event, int param) {
        DownloadInfo entity = task.getEntity();

        if (event == Downloader.EVENT_PREPARING) {
            if (param == Downloader.PREPARE_ACTION_FETCHURL) {
                entity.setDownloadState(ResourceStatus.DOWNLOADING);
                notifyStatusChanged(DownloadState.active, task);
            }
            if (!task.requestDelete()) {
                mModelDao.saveOrUpdate(task.getEntity());
            }
        } else if (event == Downloader.EVENT_PREPARED) {
            entity.setDownloadState(ResourceStatus.DOWNLOADING);
            notifyStatusChanged(DownloadState.active, task);

            if (!task.requestDelete()) {
                mModelDao.saveOrUpdate(task.getEntity());
            }

        } else if (event == Downloader.EVENT_START) {
            if (!task.requestDelete())
                mModelDao.saveOrUpdate(task.getEntity());
            notifyProgressEvent(task.getProgress(), task);
        } else if (event == Downloader.EVENT_PROGRESS) {
            if (!task.requestDelete() && task.needSaveForChanged()) {
                mModelDao.saveOrUpdate(task.getEntity());
            }

            task.setProgress(param);
            notifyProgressEvent(param, task);
        } else if (event == Downloader.EVENT_COMPLETE) {
            if (param == NetworkError.SUCCESS)
                onCompleted(task);
            else
                onStopped(param, task);
        }
    }

    private void tickSpeed() {
        if (mHandler.hasMessages(MSG_SPEED_TICK))
            return;
        mHandler.sendEmptyMessageDelayed(MSG_SPEED_TICK, 1000L);
    }

    private void stopTickSpeed() {
        mHandler.removeMessages(MSG_SPEED_TICK);
    }

    private void calcDownloadSpeeds() {
        Collection<WinkRequest> tasks = mRunningTasks.values();
        if (Utils.isEmpty(tasks)) {
            WLog.w(TAG, "no tasks for calcDownloadSpeeds, stop speed tick!!");
            return;
        }

        List<DownloadInfo> entities = new ArrayList<>(tasks.size());
        for (WinkRequest wr : tasks) {
            wr.calcSpeed();
            entities.add(wr.getEntity());
        }
        final SpeedWatcher watcher = mWatcherRef.get();
        if (watcher != null) {
            watcher.onSpeedChanged(entities);
        }

        tickSpeed();
    }

    private Downloader.OnDownloadListener mOnDownloadListener = new Downloader.OnDownloadListener() {
        @Override
        public void onDownload(WinkRequest task, int event, int param) {
            Message msg = mHandler.obtainMessage(MSG_DOWNLOAD, event, param, task);
            msg.sendToTarget();
        }
    };

    private final static int MSG_SCHEDULE = 1;
    private final static int MSG_QUEUE = 2;
    private final static int MSG_SAVEORUPDATE = 3;
    private final static int MSG_DELETE = 4;
    private final static int MSG_CLEAR = 5;
    private final static int MSG_DOWNLOAD = 6;
    private final static int MSG_CANCEL = 7;
    private final static int MSG_STOPALL = 8;

    private final static int MSG_UPDATE_MODE = 9;
    private final static int MSG_SPEED_TICK = 10;

    private class DownloadHandler extends Handler {

        public DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SCHEDULE: {
                    removeMessages(MSG_SCHEDULE);
                    if (mAllowSchedule) {
                        schedulePriorityly();
                    }
                }
                break;

                case MSG_QUEUE: {
                    int mode = msg.arg1;
                    if (msg.arg2 == 0) {
                        enqueue((Resource) msg.obj, mode);
                    } else {
                        enqueue((WinkRequest) msg.obj, mode);
                    }
                }
                break;

                case MSG_SAVEORUPDATE: {
                    WinkRequest dt = (WinkRequest) msg.obj;
                    if (!dt.requestDelete())
                        mModelDao.saveOrUpdate(dt.getEntity());
                }
                break;

                case MSG_DELETE: {
                    WinkRequest dt = (WinkRequest) msg.obj;
                    boolean deletefile = (msg.arg1 != 0);
                    boolean notify = (msg.arg2 != 0);

                    deleteTask(dt, deletefile, notify);
                }
                break;

                case MSG_CLEAR: {
                    clearAllUnfinishedTasks();
                }
                break;

                case MSG_DOWNLOAD: {
                    WinkRequest dt = (WinkRequest) msg.obj;
                    onDownloadEvent(dt, msg.arg1, msg.arg2);
                }
                break;


                case MSG_CANCEL: {
                    WinkRequest dt = (WinkRequest) msg.obj;
                    cancelTask(dt);
                }
                break;

                case MSG_STOPALL: {
                    stopAll(msg.arg1);
                }
                break;

                case MSG_UPDATE_MODE: {
                    updateDownloadMode((WinkRequest) msg.obj, msg.arg1);
                }
                break;

                case MSG_SPEED_TICK: {
                    calcDownloadSpeeds();
                }
                break;

                default:
                    break;
            }
        }
    }

    private class DownloaderWrap implements Runnable {
        private final Downloader mDownloader;

        public DownloaderWrap(Downloader downloader) {
            this.mDownloader = downloader;
        }

        @Override
        public void run() {
            mDownloader.download();
        }
    }


    private static class DownloadHandlerThread extends HandlerThread {

        WeakReference<Wink> ref;

        public DownloadHandlerThread(String name, Wink manager) {
            super(name);
            this.ref = new WeakReference<>(manager);
        }

        @Override
        public void run() {
            super.run();
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            final Wink manager = ref.get();
            if (manager != null) {
                try {
                    manager.mModelDao.init();
                } catch (Exception e) {
                    WLog.printStackTrace(e);
                }
            }
        }
    }

    public void updateDownloadInfo(DownloadInfo info) {
        mModelDao.saveOrUpdate(info);
    }
}
