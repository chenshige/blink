/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blink.browser;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.handler.BrowserHandler;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.provider.BrowserContract.Bookmarks;
import com.blink.browser.provider.BrowserProvider2;
import com.blink.browser.util.BroadcastUtils;
import com.blink.browser.util.Logger;
import com.blink.browser.util.ImageBackgroundGenerator;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.ThreadedCursorAdapter;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.view.BookmarksPopWindow;
import com.blink.browser.view.RoundImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class BrowserBookmarksAdapter extends
        ThreadedCursorAdapter<BrowserBookmarksItem> {

    private LayoutInflater mInflater;
    private Context mContext;
    private OnBookmarksClickListener mBookmarksClickListener;
    private BookmarksPopWindow mMarkPopWindow;
    private boolean mIsEditMode = false;
    private List<String> mSelectStatusList;
    private boolean mIsToAddWeight = false;

    /**
     * Create a new BrowserBookmarksAdapter.
     */
    public BrowserBookmarksAdapter(Context context, OnBookmarksClickListener onBookmarksClickListener, boolean isToAddWeight) {
        // Make sure to tell the CursorAdapter to avoid the observer and auto-requery
        // since the Loader will do that for us.
        super(context, null);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mBookmarksClickListener = onBookmarksClickListener;
        mIsToAddWeight = isToAddWeight;
    }

    private void setListener(int pos, BrowserBookmarksItem item) {
        mMarkPopWindow.setClickListener(new BookmarkItemCLickListener(pos, item));
    }

    @Override
    protected long getItemId(Cursor c) {
        return c.getLong(BookmarksLoader.COLUMN_INDEX_ID);
    }

    @Override
    public View newView(Context context, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();
        View view = mInflater.inflate(R.layout.bookmark_thumbnail, parent, false);
        viewHolder.mParentView = (RelativeLayout) view.findViewById(R.id.parent);
        viewHolder.mBgView = view.findViewById(R.id.item_bg);
        viewHolder.mThumbView = (RoundImageView) view.findViewById(R.id.bookmark_item_thumb);
        viewHolder.mTitleView = (TextView) view.findViewById(R.id.bookmark_item_title);
        viewHolder.mMoreView = (ImageView) view.findViewById(R.id.bookmark_item_more);
        viewHolder.mDateView = (TextView) view.findViewById(R.id.bookmark_item_time);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, int position, BrowserBookmarksItem object) {
        bindGridView(view, position, mContext, object);
    }

    String getTitle(Cursor cursor) {
        int type = cursor.getInt(BookmarksLoader.COLUMN_INDEX_TYPE);
        switch (type) {
            case Bookmarks.BOOKMARK_TYPE_OTHER_FOLDER:
                return mContext.getString(R.string.other_bookmarks);
        }
        return cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
    }


    void bindGridView(View view, int position, Context context, BrowserBookmarksItem item) {
        // We need to set this to handle rotation and other configuration change
        // events. If the padding didn't change, this is a no op.
        if (item.date != 0) {
            assert item.url != null;//RM-337
        } else {
            return;
        }

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            viewHolder.mParentView = (RelativeLayout) view.findViewById(R.id.parent);
            viewHolder.mBgView = view.findViewById(R.id.item_bg);
            viewHolder.mThumbView = (RoundImageView) view.findViewById(R.id.bookmark_item_thumb);
            viewHolder.mTitleView = (TextView) view.findViewById(R.id.bookmark_item_title);
            viewHolder.mMoreView = (ImageView) view.findViewById(R.id.bookmark_item_more);
            viewHolder.mDateView = (TextView) view.findViewById(R.id.bookmark_item_time);
        }

        if (item.title != null) {
            viewHolder.mTitleView.setText(item.title);
            if (!BrowserProvider2.DEFAULT_BOOKMARK_TIME.equals(item.time + "") && !mIsToAddWeight) {
                viewHolder.mMoreView.setVisibility(View.VISIBLE);
                if (mSelectStatusList != null && item.id != null) {
                    if (mSelectStatusList.contains(item.id)) {
                        viewHolder.mMoreView.setImageResource(R.drawable.ic_browser_check_box_selected);
                    } else {
                        viewHolder.mMoreView.setImageResource(R.drawable.ic_browser_check_box_unselected);
                    }
                } else {
                    viewHolder.mMoreView.setImageResource(R.drawable.ic_browser_home_other_more_gray);
                }
            } else {
                viewHolder.mMoreView.setVisibility(View.GONE);
            }
        }

        viewHolder.mThumbView.setBackgroundBg(mContext.getResources().getColor(R.color.snap_item_backgroud));
        viewHolder.mThumbView.setImageBitmap(null);
        if (item.thumbnail == null) {
            viewHolder.mThumbView.setDefaultIconByUrl(item.url);
        } else {
            viewHolder.mThumbView.setRoundBg(ImageBackgroundGenerator.getBackgroundColor(item.thumbnail));
            viewHolder.mThumbView.setImageBitmap(item.thumbnail);
        }

        viewHolder.mParentView.setOnClickListener(new BookmarkItemCLickListener(position, item));
        viewHolder.mMoreView.setOnClickListener(new BookmarkItemCLickListener(position, item));
        if (!mIsToAddWeight) {
            viewHolder.mParentView.setOnLongClickListener(new BookmarkItemLongClickListener(item));
        }
    }

    @Override
    public BrowserBookmarksItem getRowObject(Cursor c,
                                             BrowserBookmarksItem item) {
        if (item == null) {
            item = new BrowserBookmarksItem();
        }
        item.thumbnail = null;
        item.thumbnail = BrowserBookmarksPage.getBitmap(c,
                BookmarksLoader.COLUMN_INDEX_TOUCH_ICON, null);
        item.hasThumbnail = item.thumbnail != null;
        item.favicon = BrowserBookmarksPage.getBitmap(c,
                BookmarksLoader.COLUMN_INDEX_FAVICON, null);
        item.isFolder = c.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) != 0;
        item.title = getTitle(c);
        item.url = c.getString(BookmarksLoader.COLUMN_INDEX_URL);
        item.id = c.getString(BookmarksLoader.COLUMN_INDEX_ID);
        item.time = c.getLong(BookmarksLoader.COLUMN_INDEX_CREATE_TIME);
        item.date = Long.parseLong(getTimeTitle(item.time));
        return item;
    }

    @Override
    public BrowserBookmarksItem getLoadingObject() {
        BrowserBookmarksItem item = new BrowserBookmarksItem();
        return item;
    }

    private class BookmarkItemCLickListener implements View.OnClickListener {
        private String mUrl;
        private int mPosition;
        BrowserBookmarksItem mItem;

        public BookmarkItemCLickListener(int pos, BrowserBookmarksItem item) {
            mPosition = pos;
            mItem = item;
        }

        @Override
        public void onClick(View view) {


            switch (view.getId()) {
                case R.id.parent:
                    if (mBookmarksClickListener != null && mSelectStatusList == null) {
                        BrowserHandler.getInstance().handlerPostDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mBookmarksClickListener.onBookmarkItemClick(mItem);
                            }
                        }, 200);
                    }
                    break;
                case R.id.bookmark_item_more:
                    if (mSelectStatusList != null && !BrowserProvider2.DEFAULT_BOOKMARK_TIME.equals(mItem.time + "")) {
                        if (mSelectStatusList.contains(mItem.id)) {
                            removeSelectedList(mItem.id);
                        } else {
                            addSelectedList(mItem.id);
                        }
                        notifyDataSetChanged();
                    } else {
                        if (mBookmarksClickListener != null) {
                            mBookmarksClickListener.onBookmarkSelectClick(mItem);
                        }
                        if (mMarkPopWindow == null) {
                            mMarkPopWindow = new BookmarksPopWindow(mContext);
                        }
                        setListener(mPosition, mItem);
                        mMarkPopWindow.show(view);
                        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARK_EVENTS, AnalyticsSettings.ID_MENU);
                    }

                    break;
                case R.id.popwindow_delete:
                    removeBookmark(getItem(mPosition).getString(BookmarksLoader.COLUMN_INDEX_ID));
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARKMENU_EVENTS, AnalyticsSettings
                            .ID_DELETE);

                    break;
                case R.id.popwindow_update:
                    mMarkPopWindow.dismiss();
                    updateBookmark(mPosition);
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARKMENU_EVENTS, AnalyticsSettings
                            .ID_MODIFY);
                    break;
                case R.id.popwindow_add_desktop:
                    if (mMarkPopWindow != null) {
                        mMarkPopWindow.dismiss();
                    }
                    addToDesktop(mItem);
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARKMENU_EVENTS, AnalyticsSettings
                            .ID_DESKTOP);
                    break;
                case R.id.popwindow_add_homepage:
                    if (mMarkPopWindow != null) {
                        mMarkPopWindow.dismiss();
                    }
                    addToHome(mItem, view);
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARKMENU_EVENTS, AnalyticsSettings
                            .ID_ADDTOHOMEPAGE);
                    break;
                default:
                    break;
            }
        }
    }

    private class BookmarkItemLongClickListener implements View.OnLongClickListener {
        BrowserBookmarksItem mItem;

        public BookmarkItemLongClickListener(BrowserBookmarksItem item) {
            mItem = item;
        }

        @Override
        public boolean onLongClick(View v) {
            Logger.e(mItem.id);
            if (!BrowserProvider2.DEFAULT_BOOKMARK_TIME.equals(mItem.time + "")) {
                addSelectedList(mItem.id);
                if (mBookmarksClickListener != null) {
                    mBookmarksClickListener.onLongClick();
                }
                notifyDataSetChanged();
            }

            return true;
        }
    }

    private void addSelectedList(String id) {
        if (mSelectStatusList == null || id == null) {
            mSelectStatusList = new ArrayList<>();
        }
        if (!mSelectStatusList.contains(id)) {
            mSelectStatusList.add(id);
            mIsEditMode = true;
        }
    }

    private void removeSelectedList(String id) {
        if (mSelectStatusList == null || id == null) {
            return;
        }
        int idx = mSelectStatusList.indexOf(id);
        if (idx > -1) {
            mSelectStatusList.remove(idx);
        }
        if (mSelectStatusList.size() <= 0) {
            mSelectStatusList = null;
            mIsEditMode = false;
            if (mBookmarksClickListener != null) mBookmarksClickListener.onSelectNull(true);
        }
    }

    public void removeAllSelected() {
        if (mSelectStatusList == null) return;
        for (String id : mSelectStatusList) {
            removeBookmark(id);
        }
        mSelectStatusList = null;
        mIsEditMode = false;
        notifyDataSetChanged();
    }

    public void clearAllSelected() {
        if (mSelectStatusList == null) return;
        mSelectStatusList = null;
        mIsEditMode = false;
        notifyDataSetChanged();
    }

    public Boolean isEditmode() {
        return mIsEditMode;
    }

    private void updateBookmark(int position) {
        Intent intent = new Intent(mContext, AddBookmarkPage.class);
        Cursor cursor = getItem(position);
        Bundle item = new Bundle();
        item.putString(BrowserContract.Bookmarks.TITLE,
                cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE));
        item.putString(BrowserContract.Bookmarks.URL,
                cursor.getString(BookmarksLoader.COLUMN_INDEX_URL));
        byte[] data = cursor.getBlob(BookmarksLoader.COLUMN_INDEX_FAVICON);
        if (data != null) {
            item.putParcelable(BrowserContract.Bookmarks.FAVICON,
                    ImageUtils.decodeByteToBitmap(data));
        }
        item.putLong(BrowserContract.Bookmarks._ID,
                cursor.getLong(BookmarksLoader.COLUMN_INDEX_ID));
        item.putLong(BrowserContract.Bookmarks.PARENT,
                cursor.getLong(BookmarksLoader.COLUMN_INDEX_PARENT));
        item.putLong(BrowserContract.Bookmarks.DATE_CREATED, System.currentTimeMillis());
        intent.putExtra(AddBookmarkPage.EXTRA_EDIT_BOOKMARK, item);
        intent.putExtra(AddBookmarkPage.EXTRA_IS_FOLDER,
                cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) == 1);
        mContext.startActivity(intent);
    }

    public interface OnBookmarksClickListener {
        void onBookmarkItemClick(BrowserBookmarksItem item);

        void onBookmarkSelectClick(BrowserBookmarksItem item);

        void onDeleteBookmark(BrowserBookmarksItem item);

        void onLongClick();

        void onSelectNull(Boolean select);
    }

    private void removeBookmark(String id) {
        if (mMarkPopWindow != null) mMarkPopWindow.dismiss();
        final Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, Long.valueOf(id)
        );
        final ContentResolver cr = mContext.getContentResolver();
        new Thread() {
            @Override
            public void run() {
                cr.delete(uri, null, null);
            }
        }.start();
    }

    public boolean onBackPress() {
        if (mIsEditMode) {
            mIsEditMode = false;
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    private String getTimeTitle(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int month = cal.get(cal.MONTH);
        if (month >= 0 && month < 10) {
            return cal.get(cal.YEAR) + "0" + cal.get(cal.MONTH);
        }
        return cal.get(cal.YEAR) + "" + cal.get(cal.MONTH);
    }

    private class ViewHolder {
        RelativeLayout mParentView;
        View mBgView;
        RoundImageView mThumbView;
        TextView mTitleView;
        ImageView mMoreView;
        TextView mDateView;
    }

    private void addToHome(BrowserBookmarksItem item, View view) {
        if (item != null) {
            RecommendUrlEntity info = new RecommendUrlEntity();
            info.setDisplayName(item.title);
            info.setWeight(0);
            info.setUrl(item.url);
            info.setImageUrl(mContext.getString(R.string.recommend_mark_default_icon));
            List<RecommendUrlEntity> list = DatabaseManager.getInstance().findByArgs(RecommendUrlEntity.class,
                    RecommendUrlEntity.Column.URL + "=?", new String[]{item.url});
            if (list == null || list.size() == 0) {
                DatabaseManager.getInstance().insert(info);
                ToastUtil.showShortToast(view.getContext(), R.string.add_to_homepage_success);
            } else {
                ToastUtil.showShortToast(view.getContext(), R.string.right_recommend_add_link_repeat);
            }
        }
    }

    private void addToDesktop(BrowserBookmarksItem item) {
        if (item.favicon != null) {
            BroadcastUtils.sendShortCutToDesktop(mContext, item.title, item.url, item.favicon);
        } else {
            BroadcastUtils.sendShortCutToDesktop(mContext, item.title, item.url, R.drawable.ic_browser_recommend_blank);
        }
        ToastUtil.showShortToast(mContext, R.string.add_to_desktop_success);
    }
}
