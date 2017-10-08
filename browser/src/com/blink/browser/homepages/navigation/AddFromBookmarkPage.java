package com.blink.browser.homepages.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.BookmarksLoader;
import com.blink.browser.BrowserBookmarksItem;
import com.blink.browser.R;
import com.blink.browser.UrlUtils;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.ColorUtils;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.ThreadedCursorAdapter;
import com.blink.browser.view.RoundImageView;

import java.util.Map;

/**
 * 从书签中添加首页标签
 */
@SuppressLint("ValidFragment")
public class AddFromBookmarkPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private View mRoot;
    private ListView mListView;
    private View mEmptyView;
    private BookmarksAdapter mCursorAdapter;
    private WebNavigationEditable mEditable;

    @SuppressLint("ValidFragment")
    public AddFromBookmarkPage(WebNavigationEditable editable) {
        this.mEditable = editable;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }
        mRoot = inflater.inflate(R.layout.add_from_bookmark, container, false);
        mEmptyView = mRoot.findViewById(android.R.id.empty);
        mListView = (ListView) mRoot.findViewById(R.id.bookmark_list);
        mCursorAdapter = new BookmarksAdapter(getActivity());
        mListView.setAdapter(mCursorAdapter);
        // Start the loaders
        LoaderManager lm = getLoaderManager();
        lm.restartLoader(0, null, this);
        return mRoot;
    }

    public int getCount() {
        if (mCursorAdapter == null) return 0;
        if (mCursorAdapter.getCount() != 0) {
            mListView.setVisibility(View.VISIBLE);
        }
        return mCursorAdapter.getCount();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if (mCursorAdapter != null) {
            mCursorAdapter.changeCursor(null);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String accountType = "";
        String accountName = "";
        return new BookmarksLoader(getActivity(),
                accountType, accountName);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mListView.setVisibility(View.VISIBLE);
        mCursorAdapter.changeCursor(cursor);
        boolean empty = mCursorAdapter.isEmpty();
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    public void onDataSetChange() {
        if (mCursorAdapter != null) {
            mCursorAdapter.notifyDataSetChanged();
        }
    }

    class BookmarksAdapter extends ThreadedCursorAdapter<BrowserBookmarksItem> {

        private LayoutInflater mInflater;
        private Context mContext;

        public BookmarksAdapter(Context context) {
            super(context, null);
            mInflater = LayoutInflater.from(context);
            mContext = context;
        }

        @Override
        public View newView(Context context, ViewGroup parent) {
            return mInflater.inflate(R.layout.add_from_bookmark_item, parent, false);
        }

        private ViewHolder createViewHolder(View itemView) {
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.mItemView = itemView;
            viewHolder.mIconView = (RoundImageView) itemView.findViewById(R.id.bookmark_item_icon);
            viewHolder.mContent = itemView.findViewById(R.id.content);
            viewHolder.mTitleView = (TextView) itemView.findViewById(R.id.bookmark_item_title);
            viewHolder.mSelectedView = (ImageView) itemView.findViewById(R.id.bookmark_item_complete);
            viewHolder.mPlaceView = itemView.findViewById(R.id.place_view);
            itemView.setTag(viewHolder);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mEditable.isEdit()) return;
                    BrowserBookmarksItem data = viewHolder.mData;
                    if (data != null) {
                        int position = mEditable.doUrlContained(data.url);
                        if (position == -1) {
                            boolean isAddSuccess = mEditable.addNewNavigation(data.title, data.url, false);
                            if (isAddSuccess) {
                                viewHolder.onSelect(true);
                                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QLMORE_EVENTS, AnalyticsSettings.ID_ADD);
                            }
                        } else {
                            boolean isDeleteSuccess = mEditable.deleteNavigation(position);
                            if (isDeleteSuccess) {
                                viewHolder.onSelect(false);
                            }
                        }
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void bindView(View view, int position, BrowserBookmarksItem item) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = createViewHolder(view);
            }
            viewHolder.mData = item;
            if (item.title != null) {
                viewHolder.mTitleView.setText(item.title);
            }
            viewHolder.mIconView.setBackgroundBg(ColorUtils.getColor(R.color.snap_item_backgroud));
            viewHolder.mIconView.setImageBitmap(null);
            if (item.thumbnail == null) {
                viewHolder.mIconView.setDefaultIconByUrl(item.url);
            } else {
                viewHolder.mIconView.setImageBitmap(item.thumbnail);
            }
            if (mEditable.doUrlContained(item.url) != -1) {
                viewHolder.mSelectedView.setVisibility(View.VISIBLE);
                viewHolder.mContent.setBackgroundColor(ColorUtils.getColor(R.color.navigation_on_select_bg));
            } else {
                viewHolder.mSelectedView.setVisibility(View.GONE);
                viewHolder.mContent.setBackgroundColor(ColorUtils.getColor(R.color.white));
            }
            if (position == getCount() - 1) {
                viewHolder.mPlaceView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mPlaceView.setVisibility(View.GONE);
            }
        }

        @Override
        public BrowserBookmarksItem getRowObject(Cursor c, BrowserBookmarksItem item) {
            if (item == null) {
                item = new BrowserBookmarksItem();
            }
            item.thumbnail = null;
            item.thumbnail = getBitmap(c,
                    BookmarksLoader.COLUMN_INDEX_TOUCH_ICON, null);
            item.hasThumbnail = item.thumbnail != null;
            item.favicon = getBitmap(c,
                    BookmarksLoader.COLUMN_INDEX_FAVICON, null);
            item.isFolder = c.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) != 0;
            item.url = c.getString(BookmarksLoader.COLUMN_INDEX_URL);
            item.title = getTitle(c);
            if (TextUtils.isEmpty(item.title)) {
                item.title = UrlUtils.getHost(item.url);
            }
            item.id = c.getString(BookmarksLoader.COLUMN_INDEX_ID);
            item.time = c.getLong(BookmarksLoader.COLUMN_INDEX_CREATE_TIME);
            return item;
        }

        String getTitle(Cursor cursor) {
            int type = cursor.getInt(BookmarksLoader.COLUMN_INDEX_TYPE);
            switch (type) {
                case BrowserContract.Bookmarks.BOOKMARK_TYPE_OTHER_FOLDER:
                    return mContext.getString(R.string.other_bookmarks);
            }
            return cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
        }

        @Override
        public BrowserBookmarksItem getLoadingObject() {
            return new BrowserBookmarksItem();
        }

        @Override
        protected long getItemId(Cursor c) {
            return c.getLong(BookmarksLoader.COLUMN_INDEX_ID);
        }
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex, Bitmap bitmap) {
        if (cursor == null) {
            return null;
        }
        byte[] data = cursor.getBlob(columnIndex);
        return ImageUtils.getBitmap(data, bitmap);
    }

    static class ViewHolder {
        BrowserBookmarksItem mData;
        View mItemView;
        View mContent;
        RoundImageView mIconView;
        TextView mTitleView;
        ImageView mSelectedView;
        View mPlaceView;

        void onSelect(boolean select) {
            if (select) {
                mSelectedView.setVisibility(View.VISIBLE);
                mContent.setBackgroundColor(ColorUtils.getColor(R.color.navigation_on_select_bg));
            } else {
                mSelectedView.setVisibility(View.GONE);
                mContent.setBackgroundColor(ColorUtils.getColor(R.color.white));
            }
        }

    }

}
