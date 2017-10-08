package com.blink.browser.homepages.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.BrowserHistoryPage.HistoryQuery;
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
 * 从历史中添加网址到导航栏
 */
@SuppressLint("ValidFragment")
public class AddFromHistoryPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOGTAG = AddFromHistoryPage.class.getSimpleName();
    private View mRoot;
    private ListView mListView;
    private View mEmptyView;
    private BookmarksAdapter mCursorAdapter;
    private WebNavigationEditable mEditable;

    @SuppressLint("ValidFragment")
    public AddFromHistoryPage(WebNavigationEditable editable) {
        this.mEditable = editable;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }
        mRoot = inflater.inflate(R.layout.add_from_history, container, false);
        mEmptyView = mRoot.findViewById(android.R.id.empty);
        mListView = (ListView) mRoot.findViewById(R.id.history_list);
        mCursorAdapter = new BookmarksAdapter(getActivity());
        mListView.setAdapter(mCursorAdapter);
        // Start the loaders
        LoaderManager lm = getLoaderManager();
        lm.restartLoader(0, null, this);
        return mRoot;
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if (mCursorAdapter != null) {
            mCursorAdapter.changeCursor(null);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri.Builder combinedBuilder = BrowserContract.History.CONTENT_URI.buildUpon();
        String sort = BrowserContract.Combined.DATE_LAST_VISITED + " DESC";
        String where = BrowserContract.Combined.DATE_LAST_VISITED + " > 0";
        return new CursorLoader(getActivity(), combinedBuilder.build(),
                HistoryQuery.PROJECTION, where, null, sort);
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

    class BookmarksAdapter extends ThreadedCursorAdapter<HistoryBean> {

        private LayoutInflater mInflater;

        BookmarksAdapter(Context context) {
            super(context, null);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, ViewGroup parent) {
            return mInflater.inflate(R.layout.add_from_bookmark_item, parent, false);
        }

        private ViewHolder createViewHolder(View itemView) {
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.mItemView = itemView;
            viewHolder.mContent = itemView.findViewById(R.id.content);
            viewHolder.mIconView = (RoundImageView) itemView.findViewById(R.id.bookmark_item_icon);
            viewHolder.mTitleView = (TextView) itemView.findViewById(R.id.bookmark_item_title);
            viewHolder.mSelectedView = (ImageView) itemView.findViewById(R.id.bookmark_item_complete);
            viewHolder.mPlaceView = itemView.findViewById(R.id.place_view);
            itemView.setTag(viewHolder);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mEditable.isEdit()) return;
                    HistoryBean data = viewHolder.mData;
                    if (data != null) {
                        int position = mEditable.doUrlContained(data.mUrl);
                        if (position == -1) {
                            boolean isAddSuccess = mEditable.addNewNavigation(data.mTitle, data.mUrl, false);
                            if(isAddSuccess) {
                                viewHolder.onSelect(true);
                                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QLMORE_EVENTS, AnalyticsSettings.ID_ADD);
                            }
                        } else {
                            boolean isDeleteSuccess = mEditable.deleteNavigation(position);
                            if(isDeleteSuccess) {
                                viewHolder.onSelect(false);
                            }
                        }
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void bindView(View view, int position, HistoryBean item) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = createViewHolder(view);
            }
            viewHolder.mData = item;
            if (item.mTitle != null) {
                viewHolder.mTitleView.setText(item.mTitle);
            }
            viewHolder.mIconView.setImageBitmap(null);
            if (item.mIcon == null) {
                viewHolder.mIconView.setDefaultIconByUrl(item.mUrl);
            } else {
                viewHolder.mIconView.setImageBitmap(item.mIcon);
            }
            viewHolder.onSelect(mEditable.doUrlContained(item.mUrl) != -1);
            if (position == getCount() - 1) {
                viewHolder.mPlaceView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mPlaceView.setVisibility(View.GONE);
            }
        }

        @Override
        public HistoryBean getRowObject(Cursor cursor, HistoryBean item) {
            if (item == null) {
                item = new HistoryBean();
            }
            item.mUrl = cursor.getString(HistoryQuery.INDEX_URL);
            item.mTitle = cursor.getString(HistoryQuery.INDEX_TITE);
            if(TextUtils.isEmpty(item.mTitle)) {
                item.mTitle = UrlUtils.getHost(item.mUrl);
            }
            byte[] data = cursor.getBlob(HistoryQuery.TOUCH_ICON);
            if (data != null) {
                item.mIcon = ImageUtils.decodeByteToBitmap(data);
            }
            return item;
        }


        @Override
        public HistoryBean getLoadingObject() {
            return new HistoryBean();
        }

        @Override
        protected long getItemId(Cursor c) {
            return c.getInt(HistoryQuery.INDEX_ID);
        }
    }

    static class ViewHolder {
        HistoryBean mData;
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

    static class HistoryBean {
        public String mTitle;
        public Bitmap mIcon;
        public String mUrl;
    }

}
