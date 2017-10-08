package com.blink.browser.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.MainPageController;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.search.SearchEnginePreference;
import com.blink.browser.util.ClickUtil;

public class SearchCardView extends RelativeLayout implements View.OnClickListener {

    private int mSearchCardCorners;
    private int mSearchCardColorInit;
    private int mSearchCardColorNormalEnd;
    private int mSearchCardColorNoTraceEnd;
    private int mSearchCardTextNormalColor;
    private int mSearchCardTextNotaceColor;
    private ImageView mSearchEngineIcon;
    private ImageView mVoiceIcon;
    private TextView mText;
    private ImageView mIncognitoIcon;
    private MainPageController mMainPageController;
    private boolean mIsClick = true;

    public SearchCardView(Context context, MainPageController mainPageController) {
        super(context);
        mMainPageController = mainPageController;
        initView();
    }

    public void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.search_card_view, this, true);
        setBackgroundResource(R.drawable.search_card_shape);
        mSearchEngineIcon = (ImageView) findViewById(R.id.select_search_engine_icon);
        mVoiceIcon = (ImageView) findViewById(R.id.voice_icon);
        mIncognitoIcon = (ImageView) findViewById(R.id.incognito_icon);
        mText = (TextView) findViewById(R.id.search_text);
        mText.setOnClickListener(this);
        mVoiceIcon.setOnClickListener(this);
        setOnClickListener(this);
        mSearchEngineIcon.setOnClickListener(this);
        setSearchEngineIcon();
        initStyle();
    }

    private void initStyle() {
        mSearchCardCorners = (int) getContext().getResources().getDimension(R.dimen.search_card_corners);
        mSearchCardColorInit = getContext().getResources().getColor(R.color.search_title_bar_color);
        mSearchCardColorNoTraceEnd = getContext().getResources().getColor(R.color.toolbar_incognito_background_color);
        mSearchCardColorNormalEnd = getContext().getResources().getColor(R.color.search_title_bar_color);
        mSearchCardTextNormalColor = getContext().getResources().getColor(R.color.url_search_color_hint);
        mSearchCardTextNotaceColor = getContext().getResources().getColor(R.color.input_url_hint_color_incognito);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.voice_icon:
                mMainPageController.startVoiceRecognizer();
                break;
            case R.id.search_card_view:
            case R.id.search_text:
                if (!ClickUtil.clickShort(view.getId())) {
                    mMainPageController.setSearchViewMoveStyle();
                }
                break;
            case R.id.select_search_engine_icon:
                mMainPageController.openSelectSearchEngineView(mSearchEngineIcon);
                break;
        }
    }

    public void setState(float state) {
        setClickSearchEngine(state == 1);
    }

    public void setSearchEngineIcon() {
        SearchEnginePreference.getDefaultSearchIcon(getContext(), mSearchEngineIcon);
    }

    public void onResume() {
        setSearchEngineIcon();
    }

    public void setIsCanClick(boolean isCanClick) {
        mIsClick = isCanClick;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !mIsClick;
    }

    private void setClickSearchEngine(boolean mIsClick) {
        mSearchEngineIcon.setClickable(mIsClick);
        mSearchEngineIcon.setEnabled(mIsClick);
    }

    public void onIncognito(boolean incognito) {
        mIncognitoIcon.setVisibility(incognito ? VISIBLE : GONE);
    }
}
