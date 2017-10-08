package com.blink.browser.menu;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.Tab;
import com.blink.browser.util.Logger;

import java.net.URLConnection;
import java.net.URLEncoder;

public class MenuToolBox extends LinearLayout {
    private LinearLayout mSearchPageLayout, mFontSizeLayout, mSnapshotLayout,
            mSaveLayout, mUserAgent, mWebviewTranslation;
    private ImageView mSearchPageImage, mFontSizeImage, mSnapshotImage,
            mSaveImage, mUserAgentImage, mWebviewTranslationImage;
    private TextView mSearchPageText, mFontSizeText, mSnapshotTextView,
            mSaveText, mUserAgentText, mWebviewTranslateText;
    private OnClickListener mClickListener;
    private BrowserSettings mBrowserSettings;

    public MenuToolBox(Context context) {
        super(context);
    }

    public MenuToolBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    public void initView() {
        mFontSizeLayout = (LinearLayout) findViewById(R.id.font_size_button_id);
        mSearchPageLayout = (LinearLayout) findViewById(R.id.search_button_id);
        mSnapshotLayout = (LinearLayout) findViewById(R.id.snapshot_button_id);
        mUserAgent = (LinearLayout) findViewById(R.id.user_agent_id);
        mWebviewTranslation = (LinearLayout) findViewById(R.id.webview_translation_id);
        mSaveLayout = (LinearLayout) findViewById(R.id.save_button_id);
        mSaveImage = (ImageView) findViewById(R.id.save_common_menu_id);
        mSaveText = (TextView) findViewById(R.id.save_common_menu);
        mSearchPageImage = (ImageView) findViewById(R.id.search_common_menu_id);
        mSearchPageText = (TextView) findViewById(R.id.search_common_menu);
        mFontSizeImage = (ImageView) findViewById(R.id.font_size_common_menu_id);
        mUserAgentImage = (ImageView) findViewById(R.id.user_agent_menu_image);
        mWebviewTranslationImage = (ImageView) findViewById(R.id.webview_translation_menu_image);
        mFontSizeText = (TextView) findViewById(R.id.font_size_common_menu);
        mSnapshotImage = (ImageView) findViewById(R.id.snapshot_common_menu_id);
        mSnapshotTextView = (TextView) findViewById(R.id.snapshot_common_menu);
        mUserAgentText = (TextView) findViewById(R.id.user_agent_menu_text);
        mWebviewTranslateText = (TextView) findViewById(R.id.webview_translation_menu_text);
        mBrowserSettings = BrowserSettings.getInstance();
    }

    public void setOnItemClickListener(OnClickListener listener) {
        if (listener == null) {
            return;
        }
        mClickListener = listener;
        mFontSizeLayout.setOnClickListener(mClickListener);
        mSearchPageLayout.setOnClickListener(mClickListener);

        mSnapshotLayout.setOnClickListener(mClickListener);
        mSaveLayout.setOnClickListener(mClickListener);
        mUserAgent.setOnClickListener(mClickListener);
        mWebviewTranslation.setOnClickListener(mClickListener);
        setOnClickListener(mClickListener);
    }

    public void updateToolBoxMenuState(Tab currentTab, boolean isCanSaveOffLine, boolean isCanAddBookmark) {
        if (currentTab == null) {
            return;
        }
        boolean isHomePage = currentTab.isNativePage();

        int disableColor = getContext().getResources().getColor(R.color.bottom_menu_disabled_text_color);
        int enableColor = getContext().getResources().getColor(R.color.bottom_menu_text_color);
        int clickColor = R.drawable.browser_common_menu_item_bg;

        boolean disableSaveOffLine = false;
        if (currentTab.getWebView() != null && currentTab.getWebView().getUrl() != null) {
            String url = currentTab.getWebView().getUrl();

            String type = "";
            try {
                type = URLConnection.guessContentTypeFromName(URLEncoder.encode(url, "UTF-8"));
            } catch (Exception e) {
                Logger.error("Browser", "can't get mimetype from:" + url);
            }
            if (!TextUtils.isEmpty(type) && type.startsWith("image")) {
                disableSaveOffLine = true;
            }
        }
        mSearchPageImage.setEnabled(!isHomePage);
        mSearchPageLayout.setEnabled(!isHomePage);
        mFontSizeImage.setEnabled(!isHomePage);
        mFontSizeLayout.setEnabled(!isHomePage);
        mSaveImage.setEnabled(!isHomePage);
        mSaveLayout.setEnabled(!isHomePage);
        mWebviewTranslation.setEnabled(!isHomePage);
        if (isHomePage) {
            mFontSizeText.setTextColor(disableColor);
            mSearchPageText.setTextColor(disableColor);
            mWebviewTranslateText.setTextColor(disableColor);
        } else {
            mFontSizeText.setTextColor(enableColor);
            mSearchPageText.setTextColor(enableColor);
            mWebviewTranslateText.setTextColor(enableColor);
        }

        if (isHomePage) {
            mSaveText.setTextColor(disableColor);
        } else {
            mSaveText.setTextColor(enableColor);
        }

        if ((!isHomePage && isCanSaveOffLine) || disableSaveOffLine) {
            mSaveLayout.setEnabled(false);
            mSaveImage.setEnabled(false);
            mSaveText.setTextColor(disableColor);
        }

        if (!isHomePage && isCanSaveOffLine) {
            mSnapshotImage.setImageResource(R.drawable.ic_browser_menu_saved_page_highlight);
        } else {
            mSnapshotImage.setImageResource(R.drawable.browser_saved_page_bg);
        }

        if (mBrowserSettings.getUserAgent() == 3) {
            mUserAgentImage.setImageResource(R.drawable.ic_browser_menu_ua);
        } else {
            mUserAgentImage.setImageResource(R.drawable.ic_browser_menu_ua_disable);
        }

        if (isHomePage) {
            mWebviewTranslationImage.setImageResource(R.drawable.ic_browser_translate_disable);
        } else {
            mWebviewTranslationImage.setImageResource(R.drawable.ic_browser_translate);
        }
        mSnapshotTextView.setTextColor(enableColor);
        mFontSizeLayout.setBackgroundResource(clickColor);
        mSearchPageLayout.setBackgroundResource(clickColor);
        mSnapshotLayout.setBackgroundResource(clickColor);
    }
}
