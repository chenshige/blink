/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.FragmentBreadCrumbs;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.provider.BrowserContract.Combined;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.view.moplbutton.MorphAction;
import com.blink.browser.view.moplbutton.MorphingButton;
import com.blink.browser.widget.BrowserDialog;
import com.blink.browser.widget.BrowserNoTitleDialog;

import java.util.Map;

/**
 * Activity for displaying the browser's history, divided into
 * days of viewing.
 */
public class BrowserHistoryPage extends BaseFragment
        implements OnChildClickListener,
        LoaderManager.LoaderCallbacks, View.OnClickListener {

    static final int LOADER_HISTORY = 1;
    static final int LOADER_MOST_VISITED = 2;

    static final int HISTORY_ITEM_DELETE = 0;
    static final int MOST_VISITED_ITEM_DELETE = 1;
    static final int HISTORY_ALL_DELETE = 2;

    CombinedBookmarksCallbacks mCallback;

    HistoryAdapter mAdapter;
    HistoryChildWrapper mChildWrapper;
    String mMostVisitsLimit;
    ListView mGroupList, mChildList;
    private ViewGroup mPrefsContainer;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private ExpandableListView mHistoryList;
    private View mRoot;
    private BrowserDialog mDialog;

    private int mClearState = 1;
    private MorphingButton mClearAll;
    private boolean mAppear = false;
    private View mShadow;

    public static interface HistoryQuery {
        static final String[] PROJECTION = new String[]{
                Combined._ID, // 0
                Combined.DATE_LAST_VISITED, // 1
                Combined.TITLE, // 2
                Combined.URL, // 3
                Combined.TOUCH_ICON, // 4
                Combined.VISITS, // 5
        };

        static final int INDEX_ID = 0;
        static final int INDEX_DATE_LAST_VISITED = 1;
        static final int INDEX_TITE = 2;
        static final int INDEX_URL = 3;
        static final int TOUCH_ICON = 4;
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader loader) {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //this is only query history table
        Uri.Builder combinedBuilder = BrowserContract.History.CONTENT_URI.buildUpon();
        switch (id) {
            case LOADER_HISTORY: {
                String sort = Combined.DATE_LAST_VISITED + " DESC";
                String where = Combined.DATE_LAST_VISITED + " > 0";
                CursorLoader loader = new CursorLoader(getActivity(), combinedBuilder.build(),
                        HistoryQuery.PROJECTION, where, null, sort);
                return loader;
            }

            case LOADER_MOST_VISITED: {
                Uri uri = combinedBuilder
                        .appendQueryParameter(BrowserContract.PARAM_LIMIT, mMostVisitsLimit)
                        .build();
                String where = Combined.VISITS + " > 0";
                CursorLoader loader = new CursorLoader(getActivity(), uri,
                        HistoryQuery.PROJECTION, where, null, Combined.VISITS + " DESC" + "," + Combined
                        .DATE_LAST_VISITED + " DESC");
                return loader;
            }

            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_HISTORY:
                mAdapter.changeCursor((Cursor) data);
                if (!mAdapter.isEmpty() && mGroupList != null
                        && mGroupList.getCheckedItemPosition() == ListView.INVALID_POSITION) {
                    selectGroup(0);
                }
                checkIfEmpty();
                break;
            case LOADER_MOST_VISITED:
                mAdapter.changeMostVisitedCursor((Cursor) data);
                checkIfEmpty();
                break;
            default:
                throw new IllegalArgumentException();
        }
        int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++) {
            mHistoryList.expandGroup(i);
        }
    }

    void selectGroup(int position) {
        mGroupItemClickListener.onItemClick(null,
                mAdapter.getGroupView(position, false, null, null),
                position, position);
    }

    void checkIfEmpty() {
        if (mAdapter.mMostVisited != null && mAdapter.mHistoryCursor != null) {
            // Both cursors have loaded - check to see if we have data
            if (mAdapter.isEmpty()) {
                mRoot.findViewById(R.id.history).setVisibility(View.GONE);
                mRoot.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
                clearAllAnimation();
            } else {
                if (this.getUserVisibleHint() && mClearAll.getVisibility() == View.GONE) {
                    MorphAction.morphToFailure(getActivity(), mClearAll, 0);
                    mClearAll.setVisibility(View.VISIBLE);
                    MorphAction.morphMoveRotation(mClearAll, MorphAction.integer(getActivity(), R.integer
                            .mb_animation));
                }
                mRoot.findViewById(R.id.history).setVisibility(View.VISIBLE);
                mRoot.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        setHasOptionsMenu(true);

        int mvlimit = getResources().getInteger(R.integer.most_visits_limit);
        mMostVisitsLimit = Integer.toString(mvlimit);
        mCallback = (CombinedBookmarksCallbacks) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.history, container, false);
        mAdapter = new HistoryAdapter(getActivity());
        mShadow = mRoot.findViewById(R.id.history_shadow);
        mClearAll = (MorphingButton) mRoot.findViewById(R.id.morph_clear);
        inflateSinglePane();
        getLoaderManager().restartLoader(LOADER_HISTORY, null, this);
        getLoaderManager().restartLoader(LOADER_MOST_VISITED, null, this);

        return mRoot;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mShadow.setOnClickListener(this);
        mClearAll.setOnClickListener(this);

        onViewPrepared();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.morph_clear:
                onMorphButton2Clicked(mClearAll);
                mShadow.setVisibility(View.VISIBLE);
                break;
            case R.id.history_shadow:
                mShadow.setVisibility(View.GONE);
                MorphAction.morphToFailure(getActivity(), mClearAll, MorphAction.integer(getActivity(), R.integer
                        .mb_animation_short));
                mClearState = 1;
                break;
        }
    }

    private void inflateSinglePane() {
        mHistoryList = (ExpandableListView) mRoot.findViewById(R.id.history);
        mHistoryList.setAdapter(mAdapter);
        mHistoryList.setOnChildClickListener(this);
        registerForContextMenu(mHistoryList);
        mHistoryList.setGroupIndicator(null);
        mHistoryList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
    }

    private void inflateTwoPane(ViewStub stub) {
        stub.setLayoutResource(R.layout.preference_list_content);
        stub.inflate();
        mGroupList = (ListView) mRoot.findViewById(android.R.id.list);
        mPrefsContainer = (ViewGroup) mRoot.findViewById(R.id.prefs_frame);
        mFragmentBreadCrumbs = (FragmentBreadCrumbs) mRoot.findViewById(android.R.id.title);
        mFragmentBreadCrumbs.setMaxVisible(1);
        mFragmentBreadCrumbs.setActivity(getActivity());
        mPrefsContainer.setVisibility(View.VISIBLE);
        mGroupList.setAdapter(new HistoryGroupWrapper(mAdapter));
        mGroupList.setOnItemClickListener(mGroupItemClickListener);
        mGroupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mChildWrapper = new HistoryChildWrapper(mAdapter);
        mChildList = new ListView(getActivity());
        mChildList.setAdapter(mChildWrapper);
        mChildList.setOnItemClickListener(mChildItemClickListener);
        registerForContextMenu(mChildList);
        ViewGroup prefs = (ViewGroup) mRoot.findViewById(R.id.prefs);
        prefs.addView(mChildList);
    }

    private OnItemClickListener mGroupItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(
                AdapterView<?> parent, View view, int position, long id) {
            CharSequence title = ((TextView) view).getText();
            mFragmentBreadCrumbs.setTitle(title, title);
            mChildWrapper.setSelectedGroup(position);
            mGroupList.setItemChecked(position, true);
        }
    };

    private OnItemClickListener mChildItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(
                AdapterView<?> parent, View view, int position, long id) {
            mCallback.openUrl(((HistoryItem) view).getUrl());
        }
    };

    @Override
    public boolean onChildClick(ExpandableListView parent, View view,
                                int groupPosition, int childPosition, long id) {
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.HISTORY_EVENTS, AnalyticsSettings.ID_HISTORY, (
                (HistoryItem) view).getUrl());
        mCallback.openUrl(((HistoryItem) view).getUrl());
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_HISTORY);
        getLoaderManager().destroyLoader(LOADER_MOST_VISITED);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.clear_history_menu_id) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    View getTargetView(ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterContextMenuInfo) {
            return ((AdapterContextMenuInfo) menuInfo).targetView;
        }
        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            return ((ExpandableListContextMenuInfo) menuInfo).targetView;
        }
        return null;
    }

    private static abstract class HistoryWrapper extends BaseAdapter {

        protected HistoryAdapter mAdapter;
        private DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                notifyDataSetInvalidated();
            }
        };

        public HistoryWrapper(HistoryAdapter adapter) {
            mAdapter = adapter;
            mAdapter.registerDataSetObserver(mObserver);
        }
    }

    private static class HistoryGroupWrapper extends HistoryWrapper {

        public HistoryGroupWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        @Override
        public int getCount() {
            return mAdapter.getGroupCount();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getGroupView(position, false, convertView, parent);
        }

    }

    private static class HistoryChildWrapper extends HistoryWrapper {

        private int mSelectedGroup;

        public HistoryChildWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        void setSelectedGroup(int groupPosition) {
            mSelectedGroup = groupPosition;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mAdapter.getChildrenCount(mSelectedGroup);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getChildView(mSelectedGroup, position,
                    false, convertView, parent);
        }

    }

    private class HistoryAdapter extends DateSortedExpandableListAdapter {

        private Cursor mMostVisited, mHistoryCursor;

        HistoryAdapter(Context context) {
            super(context, HistoryQuery.INDEX_DATE_LAST_VISITED);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            mHistoryCursor = cursor;
            super.changeCursor(cursor);
        }

        void changeMostVisitedCursor(Cursor cursor) {
            if (mMostVisited == cursor) {
                return;
            }
            if (mMostVisited != null) {
                mMostVisited.unregisterDataSetObserver(mDataSetObserver);
                mMostVisited.close();
            }
            mMostVisited = cursor;
            if (mMostVisited != null) {
                mMostVisited.registerDataSetObserver(mDataSetObserver);
            }
            notifyDataSetChanged();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            if (moveCursorToChildPosition(groupPosition, childPosition)) {
                Cursor cursor = getCursor(groupPosition);
                return cursor.getLong(HistoryQuery.INDEX_ID);
            }
            return 0;
        }

        @Override
        public int getGroupCount() {
            return super.getGroupCount() + (!isMostVisitedEmpty() ? 1 : 0);
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (groupPosition >= super.getGroupCount()) {
                if (isMostVisitedEmpty()) {
                    return 0;
                }
                return mMostVisited.getCount();
            }
            return super.getChildrenCount(groupPosition);
        }

        @Override
        public boolean isEmpty() {
            if (!super.isEmpty()) {
                return false;
            }
            return isMostVisitedEmpty();
        }

        private boolean isMostVisitedEmpty() {
            return mMostVisited == null
                    || mMostVisited.isClosed()
                    || mMostVisited.getCount() == 0;
        }

        Cursor getCursor(int groupPosition) {
            if (groupPosition >= super.getGroupCount()) {
                return mMostVisited;
            }
            return mHistoryCursor;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            if (groupPosition >= super.getGroupCount()) {
                if (mMostVisited == null || mMostVisited.isClosed()) {
                    throw new IllegalStateException("Data is not valid");
                }
                TextView item;
                if (null == convertView || !(convertView instanceof TextView)) {
                    LayoutInflater factory = LayoutInflater.from(getContext());
                    item = (TextView) factory.inflate(R.layout.history_header, null);
                } else {
                    item = (TextView) convertView;
                }
                item.setText(R.string.most_visited);
                return item;
            }
            return super.getGroupView(groupPosition, isExpanded, convertView, parent);
        }

        @Override
        boolean moveCursorToChildPosition(
                int groupPosition, int childPosition) {
            if (groupPosition >= super.getGroupCount()) {
                if (mMostVisited != null && !mMostVisited.isClosed()) {
                    mMostVisited.moveToPosition(childPosition);
                    return true;
                }
                return false;
            }
            return super.moveCursorToChildPosition(groupPosition, childPosition);
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            HistoryItem item;
            if (null == convertView || !(convertView instanceof HistoryItem)) {
                item = new HistoryItem(getContext());
                // Add padding on the left so it will be indented from the
                // arrows on the group views.
                item.setPadding(item.getPaddingLeft() + 10,
                        item.getPaddingTop(),
                        item.getPaddingRight(),
                        item.getPaddingBottom());
            } else {
                item = (HistoryItem) convertView;
            }

            // Bail early if the Cursor is closed.
            if (!moveCursorToChildPosition(groupPosition, childPosition)) {
                return item;
            }

            final Cursor cursor = getCursor(groupPosition);
            int _id = cursor.getInt(HistoryQuery.INDEX_ID);
            item.setName(cursor.getString(HistoryQuery.INDEX_TITE));
            final String url = cursor.getString(HistoryQuery.INDEX_URL);
            item.setUrl(url);
            byte[] data = cursor.getBlob(HistoryQuery.TOUCH_ICON);

            item.mImageView.setBackgroundBg(Color.WHITE);
            item.mImageView.setImageDrawable(null);
            if (data != null) {
                item.setFavicon(ImageUtils.decodeByteToBitmap(data));
            } else {
                String urltemp = cursor.getString(HistoryQuery.INDEX_URL);
                item.setFavicon(urltemp);
            }

            //this contentValues is saved hitory record‘s temp data
            ContentValues valuesTemp;
            if (item.mCloseTv.getTag() == null) {
                valuesTemp = new ContentValues();
            } else {
                valuesTemp = (ContentValues) item.mCloseTv.getTag();
            }
            valuesTemp.put(Combined._ID, cursor.getInt(HistoryQuery.INDEX_ID));

            item.mCloseTv.setTag(valuesTemp);
            final String deleteUrl = item.getUrl();
            item.mCloseTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues values = (ContentValues) v.getTag();
                    clearData(0, (Integer) values.get(BrowserContract.History._ID));
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.HISTORY_EVENTS, AnalyticsSettings.ID_DELETE);
                }
            });
            return item;
        }
    }

    private void showDialog(final int state, final int positon) {
        mDialog = new BrowserNoTitleDialog(getActivity()) {
            public void onPositiveButtonClick() {
                super.onPositiveButtonClick();
                clearData(state, positon);
            }
        };
        mDialog.setBrowserMessage(getText(
                state == MOST_VISITED_ITEM_DELETE || state == HISTORY_ITEM_DELETE ? R.string.title_clear_history : R
                        .string.title_clear_all_history).toString())
                .setBrowserPositiveButton(R.string.clear)
                .setBrowserNegativeButton(R.string.cancel)
                .show();

    }

    public void clearData(int state, int i) {
        if (state == 0) {
            getActivity().getContentResolver().delete(BrowserContract.History.CONTENT_URI, BrowserContract.History
                    ._ID + "=?", new String[]{i + ""});
        } else if (state == MOST_VISITED_ITEM_DELETE) {
            ContentValues cv = new ContentValues();
            cv.put(BrowserContract.History.VISITS, HISTORY_ITEM_DELETE);
            getActivity().getContentResolver().update(BrowserContract.History.CONTENT_URI, cv, BrowserContract
                    .History.VISITS + ">?", new String[]{HISTORY_ITEM_DELETE + ""});
        } else if (state == HISTORY_ALL_DELETE) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.HISTORY_EVENTS, AnalyticsSettings.ID_DELETEALL);
            getActivity().getContentResolver().delete(BrowserContract.History.CONTENT_URI, BrowserContract.History
                    ._ID + ">?", new String[]{-1 + ""});
        }
    }

    @Override
    public void onPause() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAppear) setUserVisibleHint(true);
    }

    @Override
    protected void lazyLoad() {
        super.lazyLoad();
        if (!mAdapter.isEmpty()) {
            MorphAction.morphToFailure(getActivity(), mClearAll, 0);
            mClearAll.setVisibility(View.VISIBLE);
            MorphAction.morphMoveRotation(mClearAll, MorphAction.integer(getActivity(), R.integer
                    .mb_animation));
        }
    }

    private void onMorphButton2Clicked(final MorphingButton btnMorph) {
        if (mClearState == 0) {
            mClearState++;
            clearData(HISTORY_ALL_DELETE, HISTORY_ITEM_DELETE);
        } else if (mClearState == 1) {
            mClearState = 0;
            MorphAction.morphToSquare(getActivity(), btnMorph, MorphAction.integer(getActivity(), R.integer
                    .mb_animation_short), R.string.clear_history_all);
        }
    }

    private void clearAllAnimation() {
        mShadow.setVisibility(View.GONE);
        if (mClearState == 0 || mClearAll.getVisibility() == View.GONE) return;

        MorphAction.morphToFailure(getActivity(), mClearAll, MorphAction.integer(getActivity(), R.integer
                .mb_animation_short));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                MorphAction.morphMoveOutRotation(mClearAll, MorphAction.integer(getActivity(), R.integer
                        .mb_animation));
            }
        }, MorphAction.integer(getActivity(), R.integer.mb_animation_short));
    }

    public void addClearButton() {
        if (mAdapter != null && !mAdapter.isEmpty() && mClearAll != null && mClearAll.getVisibility() == View.VISIBLE) {
            MorphAction.morphToFailure(getActivity(), mClearAll, 0);
            MorphAction.morphMoveRotation(mClearAll, MorphAction.integer(getActivity(), R.integer
                    .mb_animation));
        }
    }

    public void removeClearButton() {
        if (mShadow != null) {
            mShadow.setVisibility(View.GONE);
        }

        if (mClearAll == null) return;
        if (mClearAll.getVisibility() == View.GONE) return;
        if (mClearState == 0) {
            MorphAction.morphToFailure(getActivity(), mClearAll, MorphAction.integer(getActivity(), R.integer
                    .mb_animation_short));
        }
        mClearState = 1;
        MorphAction.morphMoveOutRotation(mClearAll, 0);
    }

    public void AppearFirst() {
        mAppear = true;
    }
}
