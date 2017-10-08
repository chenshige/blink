package com.blink.browser.download.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.support.WinkEvent;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.Logger;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.view.DownloadsPopWindow;
import com.tcl.framework.notification.NotificationCenter;
import com.tcl.framework.notification.Subscriber;
import com.wcc.wink.SpeedWatcher;
import com.wcc.wink.Wink;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.util.Utils;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuAdapter;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuLayout;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class DownloadingAdapter extends SwipeMenuAdapter<DownloadingAdapter.ViewHolder> implements View.OnClickListener,
        SwipeMenuLayout.OnSwipeStateListener {
    private static final String TAG = "DownloadingAdapter";

    public final static int ITEM_VIEW_DOWNLOADING_ITEM = 1;
    public final static int ITEM_VIEW_DOWNLOADED_ITEM = 2;

    private final Context mContext;
    private List<DownloadInfo> mDownloadTasks = new ArrayList<>();
    private int[] mItemLayouts = {0, R.layout.item_downloading, R.layout.item_downloaded};

    private final LayoutInflater mLayoutInflater;

    private int mDownloadType = 1;
    private DownloadsPopWindow mPopWindow;
    private OnDownloadChangeListener mOnDownloadChangeListener;
    private List<DownloadInfo> mSwipeTasks = new ArrayList<>();

    public DownloadingAdapter(Context context, OnDownloadChangeListener listener) {
        this.mContext = context;
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mOnDownloadChangeListener = listener;
        this.setOnSwipeStateListener(this);
    }

    protected int itemContentViewLayout(int viewType) {
        if (viewType < mItemLayouts.length)
            return mItemLayouts[viewType];

        return 0;
    }

    public void setDownloadingTasks(Collection<DownloadInfo> tasks) {
        mDownloadTasks.clear();
        if (!Utils.isEmpty(tasks)) {
            mDownloadTasks.clear();
            mDownloadTasks.addAll(tasks);
        }
    }

    public void setDownloadedTasks(Collection<DownloadInfo> tasks, int type) {
        mDownloadType = type;
        if (!Utils.isEmpty(tasks)) {
            mDownloadTasks.clear();
            mDownloadTasks.addAll(tasks);
        }
    }

    private void refreshProgress(DownloadInfo data) {
        int index = mDownloadTasks.indexOf(data);
        if (index == -1) {
            return;
        }

        if (!inSwipeList(data)) {
            notifyItemChanged(index, data);
        }
    }

    private void onStatusChanged(DownloadInfo data) {
        int index = mDownloadTasks.indexOf(data);
        if (data.getDownloadState() == ResourceStatus.DOWNLOADED) {
            mDownloadTasks.remove(data);
            notifyDataSetChanged();
            if (mOnDownloadChangeListener != null) {
                mOnDownloadChangeListener.onDownloaded();
            }
        } else if (index != -1) {
            if (!inSwipeList(data)) {
                notifyItemChanged(index, data);
            }
        }
    }

    private RecyclerView mAttachedRecyclerView;
    private SpeedWatcher speedWatcher = new SpeedWatcher() {
        @Override
        public void onSpeedChanged(Collection<DownloadInfo> tasks) {
            if (mAttachedRecyclerView != null && !mAttachedRecyclerView.isComputingLayout()) {
                notifyDataSetChanged();
            }
        }
    };

    private Subscriber<WinkEvent> eventSubscriber = new Subscriber<WinkEvent>() {
        @Override
        public void onEvent(WinkEvent event) {

            if (event.event == WinkEvent.EVENT_PROGRESS) {
                refreshProgress(event.entity);
            } else if (event.event == WinkEvent.EVENT_STATUS_CHANGE) {
                onStatusChanged(event.entity);
            }
        }
    };

    @Override
    public View onCreateContentView(ViewGroup parent, int viewType) {
        int layout = itemContentViewLayout(viewType);
        return mLayoutInflater.inflate(layout, parent, false);
    }

    @Override
    public ViewHolder onCompatCreateViewHolder(View realContentView, int viewType) {
        if (realContentView == null)
            throw new RuntimeException("no content view for viewType = " + viewType);

        if (viewType == ITEM_VIEW_DOWNLOADING_ITEM) {
            return new DownloadingItemHolder(mContext, realContentView);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DownloadInfo data = getItemData(position);
        holder.bindView(data);
        holder.attach(this);
    }

    public DownloadInfo getItemData(int position) {
        if (mDownloadTasks != null) {
            return mDownloadTasks.get(position);
        }
        return null;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        NotificationCenter.defaultCenter().subscriber(WinkEvent.class, eventSubscriber);
        Wink.get().setSpeedWatcher(speedWatcher);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        NotificationCenter.defaultCenter().unsubscribe(WinkEvent.class, eventSubscriber);
    }

    @Override
    public int getItemCount() {
        return mDownloadTasks.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mDownloadType == ITEM_VIEW_DOWNLOADING_ITEM) {
            return ITEM_VIEW_DOWNLOADING_ITEM;
        } else {
            return ITEM_VIEW_DOWNLOADED_ITEM;
        }
    }

    private void handle(View v, DownloadInfo data) {
        if (mPopWindow == null) {
            mPopWindow = new DownloadsPopWindow(v.getContext());
        }
        Boolean downloading = true;
        if (data.getDownloadState() == ResourceStatus.DOWNLOADED) {
            downloading = false;
        }
        mPopWindow.setDownloadInfo(data);
        mPopWindow.setDownloading(downloading);
        mPopWindow.setClickListener(this);
        mPopWindow.show(v);

    }

    private void action(View view, DownloadInfo data) {
        int state = data.getDownloadState();
        switch (state) {
            case ResourceStatus.WAIT:
            case ResourceStatus.DOWNLOADING:
                ((ImageView) view).setImageResource(R.drawable.ic_browser_download_play);
                Wink.get().stop(data.getKey());
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings.ID_PAUSE);
                break;
            case ResourceStatus.DOWNLOAD_FAILED:
                ((ImageView) view).setImageResource(R.drawable.ic_browser_download_pause);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings.ID_RETRY);
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.RETRY_EVENTS, AnalyticsSettings.ID_URL, data
                        .getUrl());
                Wink.get().wink(data.getResource());
                break;
            case ResourceStatus.INIT:
            case ResourceStatus.PAUSE:
                ((ImageView) view).setImageResource(R.drawable.ic_browser_download_pause);
                Wink.get().wink(data.getResource());
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings.ID_CONTINUE);
                break;
            case ResourceStatus.DELETED:
                break;
            case ResourceStatus.DOWNLOADED:
                handle(view, data);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        DownloadInfo data = (DownloadInfo) v.getTag();

        switch (v.getId()) {
            case R.id.item_action:
                action(v, data);
                break;
            case R.id.item_container:
                Logger.e(data.getLocalFilePath());
                openFile(mContext, data);
                break;
            case R.id.popwindow_copylink:
                if (mPopWindow != null) {
                    mPopWindow.dismiss();
                }
                if (mPopWindow.getDownloadInfo() != null) {
                    copy(v.getContext(), mPopWindow.getDownloadInfo().getUrl());
                    ToastUtil.showShortToast(v.getContext(), R.string.copylink_success);
                }
                break;
            case R.id.popwindow_rename:
                if (mPopWindow != null) {
                    mPopWindow.dismiss();
                }
                if (mOnDownloadChangeListener != null && mPopWindow.getDownloadInfo() != null) {
                    mOnDownloadChangeListener.itemRename(mPopWindow.getDownloadInfo().getId());
                }
                break;
            case R.id.popwindow_share:
                if (mPopWindow.getDownloadInfo() != null) {
                    sharePage(mContext, mPopWindow.getDownloadInfo().getTitle(), mPopWindow.getDownloadInfo().getUrl());
                }
                break;
            case R.id.popwindow_delete:

                break;
        }
    }


    private void openFile(Context context, DownloadInfo data) {
        File file = new File(data.getLocalFilePath());
        Intent intent = new Intent();

        String type = null;
        String fileExtension = FileUtils.getFileExtensionFromUrl(data.getUrl()).toLowerCase();
        if (fileExtension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        }

        if (!TextUtils.isEmpty(type) && type.startsWith("application/vnd.oma.drm")) {
            DrmManagerClient client = new DrmManagerClient(context);
            type = client.getOriginalMimeType(file.getAbsolutePath());
        }

        if (TextUtils.isEmpty(type)) {
            ToastUtil.showShortToast(context, R.string.unknown_file);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), type);
            try {
                if (null != intent.resolveActivity(context.getPackageManager())) {
                    context.startActivity(intent);
                } else {
                    ToastUtil.showShortToast(context, R.string.cannot_open_the_file);
                }
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSwipeState(int adapterPosition, int menuState) {
        if (adapterPosition == -1) return;

        DownloadInfo info = mDownloadTasks.get(adapterPosition);
        if (info.getDownloadState() == ResourceStatus.DOWNLOADED) return;

        switch (menuState) {
            case SwipeMenuRecyclerView.MENU_OPEN:
                mSwipeTasks.add(info);
                break;
            case SwipeMenuRecyclerView.MENU_CLOSE:
                try {
                    for (DownloadInfo data : mSwipeTasks) {
                        if (data.getKey().equals(info.getKey())) {
                            mSwipeTasks.remove(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    static abstract class ViewHolder extends RecyclerView.ViewHolder {

        private final Context context;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
        }

        public Context getContext() {
            return context;
        }

        public void setItemText(int itemId, int textId) {
            String text = context.getResources().getString(textId);
            setItemText(itemId, text);
        }

        public void setItemText(int itemId, int textId, Object... args) {
            String text = context.getResources().getString(textId, args);
            setItemText(itemId, text);
        }

        public void setItemText(int itemId, CharSequence text) {
            TextView view = (TextView) itemView.findViewById(itemId);
            if (view != null)
                view.setText(text);
        }

        public void setProgress(int progress) {
            ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.progress);
            if (bar != null) {
                bar.setProgress(progress);
            }
        }

        public void loadIcon(int imageViewId, String url) {
            ImageView view = (ImageView) itemView.findViewById(imageViewId);
            if (view != null) {
                //ImageLoader.getInstance().loadIcon(context, url, R.drawable.ic_video_default, view);
            }
        }

        public void loadThumbnail(String url) {
            ImageView view = (ImageView) itemView.findViewById(R.id.item_icon);
            //ImageLoader.getInstance().loadIcon(context, url, R.drawable.ic_video_default, view);
        }

        public void setViewOnClickListener(int viewId, View.OnClickListener listener) {
            View v = itemView.findViewById(viewId);
            if (v != null) {
                v.setOnClickListener(listener);
            }
        }

        public void setViewTag(int viewId, Object tag) {
            View v = itemView.findViewById(viewId);
            if (v != null) {
                v.setTag(tag);
            }
        }

        public abstract void bindView(DownloadInfo data);

        protected void attach(DownloadingAdapter adapter) {

        }
    }

    public void deleteItem(int postion) {
        DownloadInfo data = getItemData(postion);
        deleteItem(data);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADINGDELETE_EVENTS, AnalyticsSettings
                .ID_URL, data.getUrl());
    }

    public void deleteItem(DownloadInfo data) {
        if (data == null) return;

        mDownloadTasks.remove(data);
        notifyDataSetChanged();
        Wink.get().delete(data.getKey(), true);
    }

    private void copy(Context context, CharSequence text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    public interface OnDownloadChangeListener {
        void onDownloaded();

        void itemRename(long id);

        void deleteItem(int postion);
    }

    private void sharePage(Context c, String title, String url) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, title + "\n" + url);
        try {
            c.startActivity(Intent.createChooser(send, c.getString(
                    R.string.choosertitle_sharevia)));
        } catch (android.content.ActivityNotFoundException ex) {
            // if no app handles it, do nothing
        }
    }

    private boolean inSwipeList(DownloadInfo data) {
        for (DownloadInfo info :
                mSwipeTasks) {
            if (data.getKey().equals(info.getKey())) {
                return true;
            }
        }
        return false;
    }
}
