// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.blink.browser.adapter.InputWordAdapter;
import com.blink.browser.adapter.UrlSortAdapter;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.bean.InputUrlEntity;
import com.blink.browser.bean.InputWordBean;
import com.blink.browser.bean.UrlInfo;
import com.blink.browser.database.SqlBuild;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.SystemTintBarUtils;
import com.blink.browser.view.SearchInputHintView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UrlSearchActivity extends FragmentActivity implements View.OnClickListener, TextWatcher, InputWordAdapter
        .InputWordListener, SearchInputHintView.IHintClickListener {
    private static final int WAIT_TIME = 10;
    private boolean mIncognito;

    private Handler mSearchHandler;
    private EditText mSearchView;

    private View mSearchContainer;
    private ListView mUrlList;
    private ImageView mClearText;
    private RecyclerView mFlowLayout;
    private UrlSortAdapter mAdapter;
    private ImageView mPerformUrl;
    private String mUrl;
    private View mRootView;
    private int mCurrHeight = -1;
    private SearchInputHintView mInputHintView;
    private ImageView mDeleteAll;
    private ImageView mGoBack;
    private boolean isFirstEnter = false;
    private ImageView mCancel;
    private RelativeLayout mContainer;
    private RelativeLayout mEditText;
    private InputWordAdapter mInputWordAdapter;
    private FrameLayout mCandidate;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // TODO Auto-generated method stub

            if (mRootView == null) return;
            Rect r = new Rect();
            mRootView.getWindowVisibleDisplayFrame(r);
            int visitHeight = mRootView.getBottom() - r.bottom;
            if (mCurrHeight == visitHeight) {
                return;
            }
            mCurrHeight = visitHeight;
            if (visitHeight == 0 && mInputHintView != null) {
                mInputHintView.setVisibility(View.GONE);
            } else {
                mInputHintView.setVisibility(View.VISIBLE);
            }
            mSearchContainer.scrollTo(0, (visitHeight));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_url_search);
        mSearchContainer = findViewById(R.id.search_root);
        mRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalListener);
        initActionBar();
        init();
        DisplayUtil.changeScreenBrightnessIfNightMode(this);
        SystemTintBarUtils.setSystemBarColor(this);
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams
                    .FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void init() {
        mUrl = getIntent().getStringExtra(BaseUi.INPUT_SEARCH_URL);
        mIncognito = getIntent().getBooleanExtra(BaseUi.INPUT_SEARCH_INCOGNITO, false);
        mSearchContainer.setOnClickListener(this);
        mSearchView = (EditText) findViewById(R.id.et_view_url_search);
        mUrlList = (ListView) findViewById(R.id.view_url_list);
        mClearText = (ImageView) findViewById(R.id.iv_view_url_txt_clear);
        mInputHintView = (SearchInputHintView) findViewById(R.id.search_hint_view);
        mFlowLayout = (RecyclerView) findViewById(R.id.input_url_flow);
        mCandidate = (FrameLayout) findViewById(R.id.url_search_candidate);
        mFlowLayout.setLayoutManager(new LinearLayoutManager(this));
        ((LinearLayoutManager) mFlowLayout.getLayoutManager()).setStackFromEnd(true);
        mInputWordAdapter = new InputWordAdapter(this);
        mInputWordAdapter.setOnclickListener(this);
        mFlowLayout.setAdapter(mInputWordAdapter);
        mContainer = (RelativeLayout) findViewById(R.id.view_url_search_container);
        mContainer.setOnClickListener(this);
        mPerformUrl = (ImageView) findViewById(R.id.perform_search_url);
        mCancel = (ImageView) findViewById(R.id.back_home_cancel);
        mEditText = (RelativeLayout) findViewById(R.id.url_search_edittext);
        mCancel.setOnClickListener(this);
        mPerformUrl.setVisibility(View.GONE);
        mPerformUrl.setOnClickListener(this);
        mAdapter = new UrlSortAdapter(this, null, this);
        mUrlList.setAdapter(mAdapter);
        initListener();
        touchListener(mUrlList);
        touchListener(mFlowLayout);
        mSearchHandler = new Handler();
        mSearchView.requestFocus();
        if (!TextUtils.isEmpty(mUrl)) {
            isFirstEnter = true;
            mSearchView.setText(mUrl);
            mSearchView.setSelection(0, mUrl.length());
        }
        mInputHintView.registerHintListener(this);
        setSearchViewStyle(mIncognito);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.url_search_input_enter, R.anim.url_search_input_exit);
        InputMethodUtils.hideKeyboard(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIncognito) {
            initFlowLayout();
        }
        BrowserAnalytics.onResume(this);
    }

    private void initListener() {

        mUrlList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                UrlSortAdapter.ViewHolder viewHolder = (UrlSortAdapter.ViewHolder) view.getTag();
                fireSearch(viewHolder.tvUrl.getText().toString());
            }
        });
        mClearText.setOnClickListener(this);
        mSearchView.addTextChangedListener(this);

        mSearchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_ENTER:
                        String content = mSearchView.getText().toString().trim();
                        if (content != null && content.length() != 0) {
                            fireSearch(content);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * 默认是进入８个默认输入网址的
     *
     * @param content
     */
    private void fireSearch(String content) {
        boolean isInputUrl = true;
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SEARCH_EVENTS, AnalyticsSettings
                .ID_CONTENT, content);
        UrlUtils.sCurrentSearchKey = content;
        if (UrlUtils.isSearch(content)) {
            String filterUrl = UrlUtils.filterBySearchEngine(this, content);
            if (filterUrl != null) {
                content = filterUrl;
                isInputUrl = true;
            }
        }
        /**
         * TODO URL的过滤处理
         * 自定义的一些scheme , 如 ： abc://
         */
        finishActivity(content, mIncognito ? false : isInputUrl);
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        if (TextUtils.isEmpty(mSearchView.getText().toString().trim())) {
            mUrlList.setVisibility(View.GONE);
            mFlowLayout.setVisibility(isFirstEnter ? View.GONE : View.VISIBLE);
            mClearText.setVisibility(View.GONE);
            mCancel.setVisibility(View.VISIBLE);
            mPerformUrl.setVisibility(View.GONE);
            mDeleteAll.setVisibility(isHasItemView() && !mIncognito ? View.VISIBLE : View.GONE);
        } else {
            mDeleteAll.setVisibility(View.GONE);
            mUrlList.setVisibility(isFirstEnter ? View.GONE : View.VISIBLE);
            mFlowLayout.setVisibility(View.GONE);
            mClearText.setVisibility(View.VISIBLE);
            mPerformUrl.setVisibility(View.VISIBLE);
            mCancel.setVisibility(View.GONE);
        }
        isFirstEnter = false;
    }


    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        //
    }

    @Override
    public void afterTextChanged(Editable e) {
        mSearchHandler.removeCallbacks(runnable);
        mSearchHandler.postDelayed(runnable, WAIT_TIME);
        if (mInputHintView != null && e.toString().length() == 0) {
            mInputHintView.setDataBefore();
        } else if (mInputHintView != null && e.toString().length() > 0) {
            mInputHintView.setDataAfter();
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String content = mSearchView.getText().toString();
            mAdapter.refrush(content);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.perform_search_url:
                String content = mSearchView.getText().toString().trim();
                if (!TextUtils.isEmpty(content))
                    fireSearch(content);
                break;
            case R.id.iv_view_url_txt_clear:
                mSearchView.setText("");
                break;
            case R.id.actionbar_right_icon:
                deleteAllInputWordData();
                break;
            case R.id.select_url:
                UrlInfo mContent = (UrlInfo) v.getTag();
                mSearchView.setText(mContent.getUrl());
                mSearchView.setSelection(mContent.getUrl().length());
                break;
            case R.id.back_home_cancel:
                finish();
                break;
            case R.id.actionbar_left_icon:
                finish();
                break;
            case R.id.search_root:
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        InputMethodUtils.hideKeyboard(this);
        super.onPause();
    }

    private void setSearchViewStyle(boolean incognito) {
        mCandidate.setVisibility(incognito ? View.GONE : View.VISIBLE);
        mDeleteAll.setVisibility(View.GONE);
        mContainer.setBackgroundColor(incognito ? getResources().getColor(R.color.search_url_input_color_incognito) : getResources().getColor(R.color.search_url_input_color));
        mSearchView.setHintTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.url_search_color_hint));
        mEditText.setBackground(incognito ? getResources().getDrawable(R.drawable.search_url_shape_incognito) : getResources().getDrawable(R.drawable.search_url_shape));
        mPerformUrl.setImageResource(incognito ? R.drawable.ic_browser_input_search_perform_incognito : R.drawable.ic_browser_input_search_perform);
        mCancel.setImageResource(incognito ? R.drawable.ic_browser_incognito_cancel: R.drawable.ic_browser_cancel);
        mSearchView.setTextColor(incognito ? getResources().getColor(R.color.white) : getResources().getColor(R.color.black));
        mInputHintView.setStyleMode(incognito);
    }

    private void touchListener(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        InputMethodUtils.hideKeyboard(UrlSearchActivity.this);
                        break;
                }
                return false;
            }
        });
    }

    private void finishActivity(String url, boolean isInputUrl) {
        Intent intent = new Intent();
        intent.putExtra(Controller.URL_SEARCH_RESULT_URL, url);
        intent.putExtra(Controller.URL_SEARCH_RESULT_IS_INPUT, isInputUrl);
        intent.putExtra(Controller.URL_SEARCH_RESULT_INPUT_WORD, mSearchView.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onHintClick(String text) {
        int index = mSearchView.getText().toString().length();
        mSearchView.setSelection(index, index);
        Editable editable = mSearchView.getText();
        editable.insert(index, text);
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SEARCH_EVENTS, AnalyticsSettings
                .ID_INPUTASSIS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.combined, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.layout_custom_actionbar);
        View actionbarView = actionBar.getCustomView();
        mGoBack = (ImageView) actionbarView.findViewById(R.id.actionbar_left_icon);
        mGoBack.setOnClickListener(this);
        mDeleteAll = (ImageView) actionbarView.findViewById(R.id.actionbar_right_icon);
        mDeleteAll.setImageResource(R.drawable.ic_browser_input_search_delete);
        mDeleteAll.setOnClickListener(this);
    }

    private List getInputWordData() {
        List<InputWordBean> list = new ArrayList<>();
        Cursor cursor = DatabaseManager.getInstance().findBySql(SqlBuild.INPUT_word_SQL, null);
        try {
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        InputWordBean iw = new InputWordBean();
                        iw.setmIndex(cursor.getInt(0));
                        iw.setmContent(cursor.getString(1));
                        iw.setmInputWord(cursor.getString(2));
                        if (!TextUtils.isEmpty(iw.getmInputWord())) {
                            list.add(iw);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            //this is not need handle
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Collections.reverse(list);
        return list;
    }

    private void initFlowLayout() {
        mInputWordAdapter.setData(getInputWordData());
        mDeleteAll.setVisibility(isHasItemView() && isShowInputWord() ? View.VISIBLE : View.GONE);
    }

    private void deleteInputWordData(int id) {
        DatabaseManager.getInstance().deleteById(InputUrlEntity.class, id);
    }

    private void deleteAllInputWordData() {
        DatabaseManager.getInstance().deleteAllData(InputUrlEntity.class);
        mDeleteAll.setVisibility(View.GONE);
        initFlowLayout();
    }

    @Override
    public void openUrl(String url) {
        fireSearch(url);
    }

    @Override
    public void deleteWord(int id) {
        deleteInputWordData(id);
        initFlowLayout();
    }

    private boolean isHasItemView() {
        return mInputWordAdapter.getItemCount() > 0;
    }

    private boolean isShowInputWord() {
        return View.VISIBLE == mFlowLayout.getVisibility();
    }
}
