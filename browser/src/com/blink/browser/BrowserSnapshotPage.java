/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.handler.BrowserHandler;
import com.blink.browser.provider.SnapshotProvider.Snapshots;
import com.blink.browser.util.ImageBackgroundGenerator;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.view.RoundImageView;
import com.blink.browser.view.moplbutton.MorphAction;
import com.blink.browser.view.moplbutton.MorphingButton;

import java.util.ArrayList;
import java.util.List;

public class BrowserSnapshotPage extends Fragment implements
        LoaderManager.LoaderCallbacks {

    public static final String EXTRA_ANIMATE_ID = "animate_id";
    private static final int LOADER_SNAPSHOTS = 1;
    private static final String[] PROJECTION = new String[]{
            Snapshots._ID,
            Snapshots.TITLE,
            Snapshots.VIEWSTATE_SIZE,
            Snapshots.THUMBNAIL,
            //regard this filed to Touch icon
            Snapshots.FAVICON,
            Snapshots.URL,
            Snapshots.DATE_CREATED,
            Snapshots.VIEWSTATE_PATH,
    };
    private static final int SNAPSHOT_ID = 0;
    private static final int SNAPSHOT_TITLE = 1;
    private static final int SNAPSHOT_VIEWSTATE_SIZE = 2;
    private static final int SNAPSHOT_THUMBNAIL = 3;
    private static final int SNAPSHOT_TOUCHICON = 4;
    private static final int SNAPSHOT_URL = 5;
    private static final int SNAPSHOT_DATE_CREATED = 6;
    private static final int SNAPSHOT_VIEWSTATE_PATH = 7;

    ListView mList;
    View mEmpty;
    SnapshotAdapter mAdapter;
    CombinedBookmarksCallbacks mCallback;
    long mAnimateId;
    private MorphingButton mClearSelect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCallback = (CombinedBookmarksCallbacks) getActivity();
        Bundle bundle = getArguments();
        if (bundle != null) {
            mAnimateId = bundle.getLong(EXTRA_ANIMATE_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.snapshots, container, false);
        mEmpty = view.findViewById(android.R.id.empty);
        mList = (ListView) view.findViewById(R.id.snapshot_listview);
        setupGrid(inflater);
        getLoaderManager().initLoader(LOADER_SNAPSHOTS, null, this);

        mClearSelect = (MorphingButton) view.findViewById(R.id.morph_clear);
        mClearSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMorphButtonClicked(mClearSelect);
            }
        });

        MorphAction.morphToSquare(getActivity(), mClearSelect, 0, R.string.remove_items_all);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getLoaderManager().destroyLoader(LOADER_SNAPSHOTS);
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
            mAdapter = null;
        }
    }

    void setupGrid(LayoutInflater inflater) {
        View item = inflater.inflate(R.layout.snapshot_item, mList, false);
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        if (loader.getId() == LOADER_SNAPSHOTS) {
            if (mAdapter == null) {
                mAdapter = new SnapshotAdapter(getActivity(), (Cursor) data);
                mList.setAdapter(mAdapter);
            } else {
                mAdapter.changeCursor((Cursor) data);
            }

            boolean empty = mAdapter.isEmpty();
            mList.setVisibility(empty ? View.GONE : View.VISIBLE);
            mEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_SNAPSHOTS) {
            return new CursorLoader(getActivity(),
                    Snapshots.CONTENT_URI, PROJECTION,
                    null, null, Snapshots.DATE_CREATED + " DESC");
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.snapshots_context, menu);
        // Create the header, re-use BookmarkItem (has the layout we want)
        BookmarkItem header = new BookmarkItem(getActivity());
        header.setEnableScrolling(true);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        populateBookmarkItem(mAdapter.getItem(info.position), header);
        menu.setHeaderView(header);
    }

    private void populateBookmarkItem(Cursor cursor, BookmarkItem item) {
        item.setName(cursor.getString(SNAPSHOT_TITLE));
        item.setUrl(cursor.getString(SNAPSHOT_URL));
        item.setFavicon(getBitmap(cursor, SNAPSHOT_TOUCHICON));
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex) {
        byte[] data = cursor.getBlob(columnIndex);
        if (data == null) {
            return null;
        }
        return ImageUtils.decodeByteToBitmap(data);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_context_menu_id) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            deleteSnapshot(info.id);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    void deleteSnapshot(long id) {
        final Uri uri = ContentUris.withAppendedId(Snapshots.CONTENT_URI, id);
        final ContentResolver cr = getActivity().getContentResolver();
        new Thread() {
            @Override
            public void run() {
                cr.delete(uri, null, null);
            }
        }.start();
    }

    private class SnapshotAdapter extends ResourceCursorAdapter {
        private List<String> mSelectStatusList;
        private boolean mIsEditMode = false;

        public SnapshotAdapter(Context context, Cursor c) {
            super(context, R.layout.snapshot_item, c, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            int position = cursor.getPosition();
            long id = cursor.getLong(SNAPSHOT_ID);
            RoundImageView thumbnail = (RoundImageView) view.findViewById(R.id.snap_item_thumb);
            thumbnail.setRoundBg(Color.WHITE);
            thumbnail.setImageDrawable(null);

            RelativeLayout parentView = (RelativeLayout) view.findViewById(R.id.snap_item_parent);
            TextView title = (TextView) view.findViewById(R.id.snap_item_title);
            title.setText(TextUtils.isEmpty(cursor.getString(SNAPSHOT_TITLE)) ? getActivity().getResources()
                    .getString(R.string.snap_shot_no_title) : cursor.getString(SNAPSHOT_TITLE));
            ImageView more = (ImageView) view.findViewById(R.id.snapshot_more);

            if (mSelectStatusList != null) {
                if (mSelectStatusList.contains(String.valueOf(id))) {
                    more.setImageResource(R.drawable.ic_browser_check_box_selected);
                } else {
                    more.setImageResource(R.drawable.ic_browser_check_box_unselected);
                }
            } else {
                more.setImageResource(R.drawable.ic_browser_home_close);
            }

            parentView.setOnClickListener(new SnapshotItemClick(id, cursor, position));
            more.setOnClickListener(new SnapshotItemClick(id, cursor, position));
            parentView.setOnLongClickListener(new SnapshotItemLongClickListener(id, cursor, position));

            byte[] thumbBlob = cursor.getBlob(SNAPSHOT_TOUCHICON);
            thumbnail.setImageDrawable(null);

            if (thumbBlob != null) {
                Bitmap thumbBitmap = ImageUtils.decodeByteToBitmap(thumbBlob);
                if (thumbBitmap != null) {
                    thumbnail.setRoundBg(ImageBackgroundGenerator.getBackgroundColor(thumbBitmap));
                    thumbnail.setImageBitmap(thumbBitmap);
                } else {
                    thumbnail.setDefaultIconByUrl(cursor.getString(SNAPSHOT_URL));
                }
            } else {
                thumbnail.setDefaultIconByUrl(cursor.getString(SNAPSHOT_URL));
            }
        }

        @Override
        public Cursor getItem(int position) {
            return (Cursor) super.getItem(position);
        }

        private class SnapshotItemClick implements View.OnClickListener {
            long mId;
            Cursor mCursor;
            int mPosition;

            public SnapshotItemClick(long id, Cursor cursor, int pos) {
                mId = id;
                mCursor = cursor;
                mPosition = pos;
            }

            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.snap_item_parent:
                        BrowserHandler.getInstance().handlerPostDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mSelectStatusList == null) {
                                    mCallback.openSnapshot(mId);
                                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SAVEDPAGE_EVENTS,
                                            AnalyticsSettings
                                                    .ID_CLICK);
                                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SAVEDPAGECLICK_EVENTS,
                                            AnalyticsSettings
                                                    .ID_URL, mCursor.getString(SNAPSHOT_URL));
                                }
                            }
                        }, 200);

                        break;
                    case R.id.snapshot_more:
                        if (mSelectStatusList != null) {
                            if (mSelectStatusList.contains(String.valueOf(mId))) {
                                removeSelectedList(String.valueOf(mId));
                            } else {
                                addSelectedList(String.valueOf(mId));
                            }
                            notifyDataSetChanged();
                        } else {
                            deleteSnapshot(mId);
                            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SAVEDPAGE_EVENTS, AnalyticsSettings
                                    .ID_DELETE);
                            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SAVEDPAGEDELETE_EVENTS, AnalyticsSettings
                                    .ID_URL, mCursor.getString(SNAPSHOT_URL));
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        private class SnapshotItemLongClickListener implements View.OnLongClickListener {
            long mId;
            Cursor mCursor;
            int mPosition;

            public SnapshotItemLongClickListener(long id, Cursor cursor, int pos) {
                mId = id;
                mCursor = cursor;
            }

            @Override
            public boolean onLongClick(View v) {
                addSelectedList(String.valueOf(mId));
                BrowserSnapshotPage.this.onLongClick();
                notifyDataSetChanged();
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
                BrowserSnapshotPage.this.onSelectNull(true);
            }
        }

        public void removeAllSelected() {
            if (mSelectStatusList == null) return;
            for (String id : mSelectStatusList) {
                deleteSnapshot(Long.parseLong(id));
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

        public Boolean isEditMode() {
            return mIsEditMode;
        }
    }

    public void onLongClick() {
        mClearSelect.setVisibility(View.VISIBLE);
        MorphAction.morphMove(mClearSelect, MorphAction.integer(getActivity(), R.integer
                .mb_animation));
        ((SnapshotActivity) getActivity()).setActionBarEdit(true);
    }

    public void onSelectNull(Boolean select) {
        MorphAction.morphMoveOut(mClearSelect, MorphAction.integer(getActivity(), R.integer
                .mb_animation));
        ((SnapshotActivity) getActivity()).setActionBarEdit(false);
    }

    private void onMorphButtonClicked(final MorphingButton btnMorph) {
        onSelectNull(true);
        mAdapter.removeAllSelected();
    }

    public void removeSelectClear() {
        if (mAdapter != null && mAdapter.isEditMode()) {
            mAdapter.clearAllSelected();
            onSelectNull(true);
        }
    }
}
