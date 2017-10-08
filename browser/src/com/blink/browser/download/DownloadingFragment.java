package com.blink.browser.download;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.download.support.WinkManageEvent;
import com.blink.browser.download.ui.DownloadingAdapter;
import com.tcl.framework.notification.NotificationCenter;
import com.tcl.framework.notification.Subscriber;
import com.wcc.wink.Wink;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.util.Comparators;
import com.wcc.wink.util.Objects;
import com.wcc.wink.util.Utils;
import com.yanzhenjie.recyclerview.swipe.Closeable;
import com.yanzhenjie.recyclerview.swipe.OnSwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DownloadingFragment extends Fragment implements View.OnClickListener, DownloadingAdapter
        .OnDownloadChangeListener {
    SwipeMenuRecyclerView mRecyclerView;
    DownloadingAdapter mDownloadingAdapter;
    private View mViews;
    private RelativeLayout mNoDownloads;
    private List<DownloadInfo> mDownloading;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        mViews = inflater.inflate(R.layout.fragment_downloading, container, false);
        mRecyclerView = (SwipeMenuRecyclerView) mViews.findViewById(R.id.swipe_recyclerview);
        mNoDownloads = (RelativeLayout) mViews.findViewById(R.id.nodownload);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new NoAlphaItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setSwipeMenuCreator(swipeMenuCreator);
        mRecyclerView.setSwipeMenuItemClickListener(menuItemClickListener);
        mRecyclerView.setLongPressDragEnabled(true);
        mDownloadingAdapter = new DownloadingAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mDownloadingAdapter);

        NotificationCenter.defaultCenter().subscriber(WinkManageEvent.class, eventSubscriber);

        return mViews;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        Wink wink = Wink.get();
        mDownloading = wink.getDownloadingResources();

        if (!Utils.isEmpty(mDownloading)) {
            Collections.sort(mDownloading, downloadingComp);
        }

        mDownloadingAdapter.setDownloadedTasks(mDownloading, DownloadingAdapter.ITEM_VIEW_DOWNLOADING_ITEM);
        mDownloadingAdapter.notifyDataSetChanged();
        showNoDownload();

    }

    private void showNoDownload() {
        mNoDownloads.setVisibility(mDownloading != null && mDownloading.size() > 0 ? View.GONE : View.VISIBLE);
    }

    Comparator<DownloadInfo> downloadingComp = new Comparator<DownloadInfo>() {
        @Override
        public int compare(DownloadInfo lhs, DownloadInfo rhs) {
            int v = Comparators.compare(rhs.getTracer().startTime, lhs.getTracer().startTime);
            if (v == 0) {
                v = Comparators.compare(rhs.getId(), lhs.getId());
                if (v == 0)
                    v = Objects.compare(lhs.getTitle(), rhs.getTitle(),
                            String.CASE_INSENSITIVE_ORDER);
            }
            return v;
        }
    };

    private Subscriber<WinkManageEvent> eventSubscriber = new Subscriber<WinkManageEvent>() {
        @Override
        public void onEvent(WinkManageEvent event) {
            refreshData();
        }
    };


    @Override
    public void onClick(View view) {

    }

    /**
     * 菜单创建器。
     */
    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
            int width = getResources().getDimensionPixelSize(R.dimen.browser_info_item);

            // MATCH_PARENT 自适应高度，保持和内容一样高；也可以指定菜单具体高度，也可以用WRAP_CONTENT。
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            // 添加右侧的，如果不添加，则右侧不会出现菜单。
            {
                SwipeMenuItem closeItem = new SwipeMenuItem(getActivity())
                        .setBackgroundDrawable(R.drawable.selector_blue)
                        .setImage(R.drawable.ic_browser_incognito_clear)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(closeItem); // 添加一个按钮到右侧菜单。
            }
        }
    };

    /**
     * 菜单点击监听。
     */
    private OnSwipeMenuItemClickListener menuItemClickListener = new OnSwipeMenuItemClickListener() {
        @Override
        public void onItemClick(Closeable closeable, int adapterPosition, int menuPosition, int direction) {
            closeable.smoothCloseMenu();// 关闭被点击的菜单。

            if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
                try {
                    mDownloadingAdapter.deleteItem(adapterPosition);
                    mDownloading.remove(adapterPosition);
                    showNoDownload();
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.DOWNLOADING_EVENTS, AnalyticsSettings.ID_DELETE);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (direction == SwipeMenuRecyclerView.LEFT_DIRECTION) {
            }
        }
    };

    @Override
    public void onDownloaded() {
        refreshData();
    }

    @Override
    public void itemRename(long id) {

    }

    @Override
    public void deleteItem(int postion) {

    }
}
