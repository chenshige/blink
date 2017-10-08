// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.DatabaseManager;
import com.blink.browser.R;
import com.blink.browser.bean.UrlInfo;
import com.blink.browser.database.BrowserSQLiteHelper;
import com.blink.browser.util.SqliteEscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UrlSortAdapter extends BaseAdapter {
    private static final int HEADER = 0;
    private static final int BODY = 1;
    private static final int RECOMEND_URL_TAG = 1;
    private static final int HISTORY_URL_TAG = 2;
    private static final int INPU_TURL_TAG = 3;
    private Context mContext;
    private LayoutInflater mInflater;
    private List<UrlInfo> mList;
    private List<UrlInfo> mSearchList;
    private EditText mEditView;
    private String mCurrSearchString;
    private Handler mainH = new Handler();
    private View.OnClickListener mListener;
    // 只在每次刷新之前预编译一次
    private Pattern pattern;

    public UrlSortAdapter(Context mContext, List<UrlInfo> list, View.OnClickListener listener) {
        this.mContext = mContext;
        if (list == null) {
            this.mList = new ArrayList<UrlInfo>();
        } else {
            this.mList = list;
        }
        this.mListener = listener;
    }

    /**
     * 当ListView数据发生变化时,调用此方法来更新ListView
     */
    private void updateListView() {

        if (mSearchList == null) {
            return;
        }
        Collections.reverse(this.mList = mSearchList);
        notifyDataSetChanged();

    }

    public int getCount() {
        return this.mList.size();
    }

    public Object getItem(int position) {
        return mList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        UrlInfo info = mList.get(position);
        if (info.isHeader()) {
            return HEADER;
        } else {
            return BODY;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public View getView(final int position, View view, ViewGroup arg2) {
        ViewHolder viewHolder = null;
        UrlInfo mContent = mList.get(position);
        if (this.getItemViewType(position) == HEADER) {
            if (view == null) {
                viewHolder = new ViewHolder();
                view = LayoutInflater.from(mContext).inflate(R.layout.item_url_header, null);
                viewHolder.tvUrl = (TextView) view.findViewById(R.id.url_item);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.tvUrl.setText(mCurrSearchString);
        } else {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.item_url, null);
                viewHolder = new ViewHolder();
                viewHolder.tvTitle = (TextView) view.findViewById(R.id.url_title);
                viewHolder.tvUrl = (TextView) view.findViewById(R.id.url_item);
                viewHolder.selectUrl = (ImageView) view.findViewById(R.id.select_url);
                viewHolder.urlIcon = (ImageView) view.findViewById(R.id.icon_url);
                viewHolder.selectUrl.setOnClickListener(mListener);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.selectUrl.setTag(mContent);
            viewHolder.tvTitle.setText(mContent.getDisplayname());
            viewHolder.tvUrl.setText(mCurrSearchString == null ? mContent.getUrl() : spanWrap(mCurrSearchString, mContent.getUrl()));
            if (mContent.getDatatype() == HISTORY_URL_TAG) {
                viewHolder.urlIcon.setImageResource(R.drawable.ic_browser_search_url_default_icon);
            } else {
                viewHolder.urlIcon.setImageResource(R.drawable.ic_browser_search_input_recommend);
            }
        }

        viewHolder.data = mContent;
        return view;

    }

    public static class ViewHolder {
        public TextView tvTitle;
        public TextView tvUrl;
        public UrlInfo data;
        public ImageView selectUrl;
        public ImageView urlIcon;
    }

    /**
     * 刷新数据
     *
     * @param keyword
     */
    public int refrush(String keyword) {
        /*
        1.  检索浏览历史数据
		2.  检索hot url列表数据
		 */
        mCurrSearchString = keyword;
        mSearchList = new ArrayList<UrlInfo>();
        if (keyword != null && !"".equals(keyword)) {
            mSearchList.add(new UrlInfo(keyword, true));
        }
        int count = findDataFromHistory(keyword, mSearchList);
        int update = findDataFromHotUrl(keyword, mSearchList);
//		Collections.sort(urls,new SortCompare());

        mainH.post(searchRun);

        return update + count;
    }

    private Runnable searchRun = new Runnable() {
        @Override
        public void run() {
            updateListView();
        }
    };

    /**
     * 检索历史浏览记录
     *
     * @param keyword
     */
    private int findDataFromHistory(String keyword, List<UrlInfo> baseUrl) {
        String sql = String.format("select * from " + BrowserSQLiteHelper.TALBE_HISTORY_URL + " where url like '%%%s%%' order by date desc limit 3", SqliteEscape.encodeSql(keyword));
        Cursor cursor = DatabaseManager.getInstance().findBySql(sql, null);
        int size = 0;
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }

            do {
                UrlInfo info = new UrlInfo();
                info.setUrl(cursor.getString(cursor.getColumnIndex("url")));
                info.setDisplayname(cursor.getString(cursor.getColumnIndex("title")));
                info.setDatatype(HISTORY_URL_TAG);
                baseUrl.add(info);
                size++;
            } while (cursor.moveToNext());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }

    /**
     * 检索热门的url (区分国家)
     *
     * @param keyword
     */
    private int findDataFromHotUrl(String keyword, List<UrlInfo> baseUrl) {
        String sql = String.format("select * from " + BrowserSQLiteHelper.TABLE_RECOMMEND_WEB_URL + " where url like '%%%s%%' limit 3", SqliteEscape.encodeSql(keyword));

        Cursor cursor = DatabaseManager.getInstance().findBySql(sql, null);
        int size = 0;
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                return 0;
            }
            do {
                UrlInfo info = new UrlInfo();
                info.setUrl(cursor.getString(cursor.getColumnIndex("url")));
                info.setDisplayname(cursor.getString(cursor.getColumnIndex("displayname")));
                info.setDatatype(RECOMEND_URL_TAG);
                baseUrl.add(info);
                size++;
            } while (cursor.moveToNext());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }

    public SpannableString spanWrap(String keyword, String str) {
        if (str == null || str.isEmpty()) {
            return new SpannableString("");
        }
        SpannableString s = new SpannableString(str);
//		Matcher m = pattern.matcher(s);
        int index = str.indexOf(keyword);
        if (index >= 0) {
            s.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.select_key_words)), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new StyleSpan(Typeface.BOLD), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }
}
