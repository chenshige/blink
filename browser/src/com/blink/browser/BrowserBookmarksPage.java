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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.ArrayMap;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.provider.BrowserContract.Accounts;
import com.blink.browser.provider.BrowserProvider2;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.view.BookmarkExpandableView;
import com.blink.browser.view.BookmarkExpandableView.BookmarkContextMenuInfo;
import com.blink.browser.view.moplbutton.MorphAction;
import com.blink.browser.view.moplbutton.MorphingButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;


/**
 * View showing the user's bookmarks in the browser.
 */

interface BookmarksPageCallbacks {
    // Return true if handled
    boolean onBookmarkSelected(Cursor c, boolean isFolder);

    // Return true if handled
    boolean onOpenInNewWindow(String... urls);

    boolean onBookmarkClick(BrowserBookmarksItem item);
}

public class BrowserBookmarksPage extends Fragment implements
        android.support.v4.app.LoaderManager.LoaderCallbacks, BreadCrumbView.Controller,
        BrowserBookmarksAdapter.OnBookmarksClickListener { //View.OnCreateContextMenuListener,OnChildClickListener

    static final String LOGTAG = "browser";

    static final int LOADER_ACCOUNTS = 1;
    static final int LOADER_BOOKMARKS = 100;

    static final String EXTRA_DISABLE_WINDOW = "disable_new_window";
    static final String PREF_GROUP_STATE = "bbp_group_state";

    static final String ACCOUNT_TYPE = "account_type";
    static final String ACCOUNT_NAME = "account_name";

    private boolean mIsToAddWeight = false;
    private BookmarksPageCallbacks mCallbacks;
    private View mRoot;
    private ListView mListView;
    private boolean mDisableNewWindow;
    private boolean mEnableContextMenu = false;
    private View mEmptyView;
    private SparseArray<BrowserBookmarksAdapter> mBookmarkAdapters = new SparseArray<BrowserBookmarksAdapter>();
    //因为没有账号。
    private JSONObject mState;
    private BrowserBookmarksAdapter mCursorAdapter;

    private MorphingButton mClearSelect;

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Cursor cursor = (Cursor) data;
        if (loader.getId() == LOADER_ACCOUNTS) {
            LoaderManager lm = getLoaderManager();
            int id = LOADER_BOOKMARKS;
            while (cursor.moveToNext()) {
                String accountName = cursor.getString(0);
                String accountType = cursor.getString(1);
                Bundle args = new Bundle();
                args.putString(ACCOUNT_NAME, accountName);
                args.putString(ACCOUNT_TYPE, accountType);
                BrowserBookmarksAdapter adapter = new BrowserBookmarksAdapter(
                        getActivity(), this, mIsToAddWeight);
                mBookmarkAdapters.put(id, adapter);
                boolean expand = true;
                try {
                    expand = mState.getBoolean(accountName != null ? accountName
                            : BookmarkExpandableView.LOCAL_ACCOUNT_NAME);
                } catch (JSONException e) {
                } // no state for accountName
                lm.restartLoader(id, args, this);
                id++;
            }
            // TODO
            getLoaderManager().destroyLoader(LOADER_ACCOUNTS);
        } else if (loader.getId() >= LOADER_BOOKMARKS) {
            if (mCursorAdapter == null) {
                mCursorAdapter = new BrowserBookmarksAdapter(getActivity(), this, mIsToAddWeight);
                mListView.setAdapter(mCursorAdapter);
                mListView.setVisibility(View.VISIBLE);
            }
            mCursorAdapter.changeCursor(cursor);

            if (mCallbacks == null && getActivity() instanceof CombinedBookmarksCallbacks) {
                mCallbacks = new CombinedBookmarksCallbackWrapper(
                        (CombinedBookmarksCallbacks) getActivity());
            }
        }

        boolean empty = mCursorAdapter.isEmpty();
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        if (loader.getId() >= LOADER_BOOKMARKS) {
            if (mCursorAdapter != null) {
                mCursorAdapter.changeCursor(null);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ACCOUNTS) {
            return new AccountsLoader(getActivity());
        } else if (id >= LOADER_BOOKMARKS) {
            String accountType = "";
            String accountName = "";
            if (args != null) { //因为现在没有账号信息，所以args是肯定为空的，不知道后面要不要账号系统。
                accountType = args.getString(ACCOUNT_TYPE);
                accountName = args.getString(ACCOUNT_NAME);
            }
            BookmarksLoader bl = new BookmarksLoader(getActivity(),
                    accountType, accountName);
            return bl;
        } else {
            throw new UnsupportedOperationException("Unknown loader id " + id);
        }
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex) {
        if (cursor == null) {
            return null;
        }
        byte[] data = cursor.getBlob(columnIndex);
        return ImageUtils.getBitmap(data, null);
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex, Bitmap bitmap) {
        if (cursor == null) {
            return null;
        }
        byte[] data = cursor.getBlob(columnIndex);
        return ImageUtils.getBitmap(data, bitmap);
    }

    private MenuItem.OnMenuItemClickListener mContextItemClickListener =
            new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onContextItemSelected(item);
                }
            };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        BookmarkContextMenuInfo info = (BookmarkContextMenuInfo) menuInfo;
        BrowserBookmarksAdapter adapter = getChildAdapter(info.groupPosition);
        Cursor cursor = adapter.getItem(info.childPosition);
        if (!canEdit(cursor)) {
            return;
        }
        boolean isFolder
                = cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) != 0;

        final Activity activity = getActivity();
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.bookmarkscontext, menu);
        if (isFolder) {
            menu.setGroupVisible(R.id.FOLDER_CONTEXT_MENU, true);
        } else {
            menu.setGroupVisible(R.id.BOOKMARK_CONTEXT_MENU, true);
            if (mDisableNewWindow) {
                menu.findItem(R.id.new_window_context_menu_id).setVisible(false);
            }
        }
        BookmarkItem header = new BookmarkItem(activity);
        header.setEnableScrolling(true);
        populateBookmarkItem(cursor, header, isFolder);
        menu.setHeaderView(header);

        int count = menu.size();
        for (int i = 0; i < count; i++) {
            menu.getItem(i).setOnMenuItemClickListener(mContextItemClickListener);
        }
    }

    boolean canEdit(Cursor c) {
        int type = c.getInt(BookmarksLoader.COLUMN_INDEX_TYPE);
        return type == BrowserContract.Bookmarks.BOOKMARK_TYPE_BOOKMARK
                || type == BrowserContract.Bookmarks.BOOKMARK_TYPE_FOLDER;
    }

    private void populateBookmarkItem(Cursor cursor, BookmarkItem item, boolean isFolder) {
        item.setName(cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE));
        if (isFolder) {
            item.setUrl(null);
            Bitmap bitmap =
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_folder_holo_dark);
            item.setFavicon(bitmap);
            new LookupBookmarkCount(getActivity(), item)
                    .execute(cursor.getLong(BookmarksLoader.COLUMN_INDEX_ID));
        } else {
            String url = cursor.getString(BookmarksLoader.COLUMN_INDEX_URL);
            item.setUrl(url);
            Bitmap bitmap = getBitmap(cursor, BookmarksLoader.COLUMN_INDEX_FAVICON, null);
            item.setFavicon(bitmap);
        }
    }

    /**
     * Create a new BrowserBookmarksPage.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SharedPreferences prefs = BrowserSettings.getInstance().getPreferences();
        try {
            mState = new JSONObject(prefs.getString(PREF_GROUP_STATE, "{}"));
        } catch (JSONException e) {
            // Parse failed, clear preference and start with empty state
            prefs.edit().remove(PREF_GROUP_STATE).apply();
            mState = new JSONObject();
        }
        Bundle args = getArguments();
        mDisableNewWindow = args != null && args.getBoolean(EXTRA_DISABLE_WINDOW, false);
        setHasOptionsMenu(true);
        if (mCallbacks == null && getActivity() instanceof CombinedBookmarksCallbacks) {
            mCallbacks = new CombinedBookmarksCallbackWrapper(
                    (CombinedBookmarksCallbacks) getActivity());
        }
    }

    @Override
    public void onBookmarkItemClick(BrowserBookmarksItem item) {
        mCallbacks.onBookmarkClick(item);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.BOOKMARK_EVENTS, AnalyticsSettings.ID_BOOKMARK, item.url);
    }

    @Override
    public void onBookmarkSelectClick(BrowserBookmarksItem item) {

    }

    @Override
    public void onDeleteBookmark(BrowserBookmarksItem item) {
    }

    @Override
    public void onLongClick() {
        if (mIsToAddWeight) return;
        mClearSelect.setVisibility(View.VISIBLE);
        FragmentActivity activity = getActivity();
        MorphAction.morphMove(mClearSelect, MorphAction.integer(activity, R.integer
                .mb_animation));
        if (activity instanceof ComboViewActivity) {
            ((ComboViewActivity) activity).setActionBarEdit(true);
        }
    }

    @Override
    public void onSelectNull(Boolean select) {
        MorphAction.morphMoveOut(mClearSelect, MorphAction.integer(getActivity(), R.integer
                .mb_animation));
        ((ComboViewActivity) getActivity()).setActionBarEdit(false);
    }

    private static class CombinedBookmarksCallbackWrapper
            implements BookmarksPageCallbacks {

        private CombinedBookmarksCallbacks mCombinedCallback;

        private CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks cb) {
            mCombinedCallback = cb;
        }

        @Override
        public boolean onOpenInNewWindow(String... urls) {
            mCombinedCallback.openInNewTab(urls);
            return true;
        }

        @Override
        public boolean onBookmarkClick(BrowserBookmarksItem item) {
            mCombinedCallback.openUrl(item.url);
            return false;
        }

        @Override
        public boolean onBookmarkSelected(Cursor c, boolean isFolder) {
            if (isFolder) {
                return false;
            }
            mCombinedCallback.openUrl(BrowserBookmarksPage.getUrl(c));
            return true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.bookmarks, container, false);
        mEmptyView = mRoot.findViewById(android.R.id.empty);

        mListView = (ListView) mRoot.findViewById(R.id.bookmark_list);
        setEnableContextMenu(mEnableContextMenu); //置长按位false
        // Start the loaders

        LoaderManager lm = getLoaderManager();
        lm.restartLoader(LOADER_BOOKMARKS, null, this);

        mClearSelect = (MorphingButton) mRoot.findViewById(R.id.morph_clear);
        mClearSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMorphButtonClicked(mClearSelect);
            }
        });

        MorphAction.morphToSquare(getActivity(), mClearSelect, 0, R.string.remove_items_all);

        return mRoot;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LoaderManager lm = getLoaderManager();
        lm.destroyLoader(LOADER_ACCOUNTS);
        lm.destroyLoader(LOADER_BOOKMARKS);
        for (int i = 0; i < mBookmarkAdapters.size(); i++) {
            int id = mBookmarkAdapters.keyAt(i);
            lm.destroyLoader(id);
        }
        mBookmarkAdapters.clear();

        if (mCursorAdapter != null) {
            mCursorAdapter.changeCursor(null);
            mCursorAdapter = null;
        }
    }

    private BrowserBookmarksAdapter getChildAdapter(int groupPosition) {
        return null;
    }

    private BreadCrumbView getBreadCrumbs(int groupPosition) {
        return null;
    }

    /* package */
    static Intent createShortcutIntent(Context context, BrowserBookmarksItem item) {
        return BookmarkUtils.createAddToHomeIntent(context, item.url, item.title, item.thumbnail, item.favicon);
    }

    static String getUrl(Cursor c) {
        return c.getString(BookmarksLoader.COLUMN_INDEX_URL);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Resources res = getActivity().getResources();
        int paddingTop = (int) res.getDimension(R.dimen.combo_paddingTop);
        mRoot.setPadding(0, paddingTop, 0, 0);
        getActivity().invalidateOptionsMenu();
    }

    /**
     * BreadCrumb controller callback
     */
    @Override
    public void onTop(BreadCrumbView view, int level, Object data) {
        int groupPosition = (Integer) view.getTag(R.id.group_position);
        Uri uri = (Uri) data;
        if (uri == null) {
            // top level
            uri = BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER;
        }
        loadFolder(groupPosition, uri);
        if (level <= 1) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param uri
     */
    private void loadFolder(int groupPosition, Uri uri) {
        LoaderManager manager = getLoaderManager();
        // This assumes groups are ordered the same as loaders
        BookmarksLoader loader = (BookmarksLoader) ((Loader<?>)
                manager.getLoader(LOADER_BOOKMARKS + groupPosition));
        loader.setUri(uri);
        loader.forceLoad();
    }

    public void setCallbackListener(BookmarksPageCallbacks callbackListener) {
        mCallbacks = callbackListener;
        mIsToAddWeight = true;
    }

    public void setEnableContextMenu(boolean enable) {
        mEnableContextMenu = enable;
        if (mListView != null) {
            if (mEnableContextMenu) {
                registerForContextMenu(mListView);
            } else {
                unregisterForContextMenu(mListView);
                mListView.setLongClickable(false);
            }
        }
    }

    private static class LookupBookmarkCount extends AsyncTask<Long, Void, Integer> {
        Context mContext;
        BookmarkItem mHeader;

        public LookupBookmarkCount(Context context, BookmarkItem header) {
            mContext = context.getApplicationContext();
            mHeader = header;
        }

        @Override
        protected Integer doInBackground(Long... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("Missing folder id!");
            }
            Uri uri = BookmarkUtils.getBookmarksUri(mContext);
            Cursor c = null;
            try {
                c = mContext.getContentResolver().query(uri,
                        null, BrowserContract.Bookmarks.PARENT + "=?",
                        new String[]{params[0].toString()}, null);

                return c.getCount();
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result > 0) {
                mHeader.setUrl(mContext.getString(R.string.contextheader_folder_bookmarkcount,
                        result));
            } else if (result == 0) {
                mHeader.setUrl(mContext.getString(R.string.contextheader_folder_empty));
            }
        }
    }

    static class AccountsLoader extends CursorLoader {

        static String[] ACCOUNTS_PROJECTION = new String[]{
                Accounts.ACCOUNT_NAME,
                Accounts.ACCOUNT_TYPE
        };

        public AccountsLoader(Context context) {
            super(context, Accounts.CONTENT_URI
                            .buildUpon()
                            .appendQueryParameter(BrowserProvider2.PARAM_ALLOW_EMPTY_ACCOUNTS, "false")
                            .build(),
                    ACCOUNTS_PROJECTION, null, null, null);
        }
    }

    public boolean onBackPressed() {
        if (mCursorAdapter != null) {
            return mCursorAdapter.onBackPress();
        }
        return false;
    }

    private void onMorphButtonClicked(final MorphingButton btnMorph) {
        mCursorAdapter.removeAllSelected();
        onSelectNull(true);
    }

    public void removeSelectClear() {
        if (mCursorAdapter != null && mCursorAdapter.isEditmode()) {
            mCursorAdapter.clearAllSelected();
            onSelectNull(true);
        }
    }
}

