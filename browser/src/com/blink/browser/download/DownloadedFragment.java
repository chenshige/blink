package com.blink.browser.download;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.blink.browser.R;
import com.blink.browser.download.support.WinkManageEvent;
import com.blink.browser.download.ui.DownloadedAdapter;
import com.blink.browser.provider.BrowserContract;
import com.tcl.framework.notification.NotificationCenter;
import com.tcl.framework.notification.Subscriber;
import com.wcc.wink.Resource;
import com.wcc.wink.Wink;
import com.wcc.wink.request.DownloadInfo;
import com.wcc.wink.request.ResourceStatus;
import com.wcc.wink.request.SimpleURLResource;
import com.wcc.wink.util.Comparators;
import com.wcc.wink.util.FileUtils;
import com.wcc.wink.util.Objects;
import com.wcc.wink.util.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.blink.browser.download.DownloadActivity.DOWNLOAD_RENAME;

public class DownloadedFragment extends Fragment implements DownloadedAdapter
        .OnDownloadChangeListener {
    RecyclerView mRecyclerView;
    DownloadedAdapter mDownloadAdapter;
    private View mViews;
    private RelativeLayout mNoDownloads;
    private List<DownloadInfo> mDownloaded;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        mViews = inflater.inflate(R.layout.fragment_downloaded, container, false);
        mRecyclerView = (RecyclerView) mViews.findViewById(R.id.swipe_recyclerview);
        mNoDownloads = (RelativeLayout) mViews.findViewById(R.id.nodownload);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new NoAlphaItemAnimator());
        mDownloadAdapter = new DownloadedAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mDownloadAdapter);

        refreshData();
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    private void refreshData() {
        Wink wink = Wink.get();
        mDownloaded = wink.getDownloadedResources();

        if (mDownloaded == null) {
            showNoDownload();
            return;
        }

        if (!Utils.isEmpty(mDownloaded)) {
            Collections.sort(mDownloaded, downloadedComp);
        }

        for (DownloadInfo data : mDownloaded) {
            if (!FileUtils.fileIsExists(data.getLocalFilePath())) {
                data.setDownloadState(ResourceStatus.DELETED);
            }

            Resource res = data.getResource();
            if (res instanceof SimpleURLResource) {
                String mime = ((SimpleURLResource) res).getMimeType();
                if ("application/vnd.android.package-archive".equalsIgnoreCase(mime) && data.getApkIcon() == null) {
                    BrowserDownloadManager.getInstance().apkInfo(data.getLocalFilePath(), getActivity(), data);
                    Wink.get().updateDownloadInfo(data);
                }
            }
        }

        if (mDownloadAdapter != null) {
            mDownloadAdapter.setDownloadedTasks(mDownloaded);
            mDownloadAdapter.notifyDataSetChanged();
        }
        showNoDownload();
    }

    private void showNoDownload() {
        if (mNoDownloads == null) return;
        mNoDownloads.setVisibility(mDownloaded != null && mDownloaded.size() > 0 ? View.GONE : View.VISIBLE);
    }

    Comparator<DownloadInfo> downloadedComp = new Comparator<DownloadInfo>() {
        @Override
        public int compare(DownloadInfo lhs, DownloadInfo rhs) {
            int v = Comparators.compare(rhs.getTracer().endTime, lhs.getTracer().endTime);
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
    public void onDownloaded() {
    }

    @Override
    public void itemRename(long id) {
        Intent intent = new Intent(getActivity(), DownloadRenamePage.class);
        intent.putExtra(BrowserContract.DOWNLOAD_REFERANCE, id);
        getActivity().startActivityForResult(intent, DOWNLOAD_RENAME);
    }

    @Override
    public void deleteItem(DownloadInfo info) {
        try {
            mDownloaded.remove(info);
        } catch (Exception e) {

        }
        showNoDownload();
    }

    public void refresh() {
        refreshData();
    }

    public boolean isDownloaded(String key) {
        Wink wink = Wink.get();
        mDownloaded = wink.getDownloadedResources();

        if (mDownloaded == null) return false;

        for (DownloadInfo data : mDownloaded) {
            if (key.equals(data.getKey())) {
                return true;
            }
        }
        return false;
    }
}
