// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.adapter.SelectLanguageAdapter;

import java.util.Locale;

/**
 * Webview translate dialog
 */
public class SelectLanguageDialog extends DialogFragment implements View.OnClickListener, SelectLanguageAdapter.translateListener {
    private SelectLanguageAdapter mSelectLanguageAdapter;
    private RelativeLayout mSelectLanguage;
    private RecyclerView mLanguageList;
    private TextView mDefaultLanguage;
    private TextView mTranslate;
    private ImageView mArrow;
    private TextView mCancel;
    private View mView;

    private boolean mIsSelectMode = false;
    private String mCountryShort;
    private String mlanguage;
    private TranslateListener mTranslateListener;
    private Context mContext;

    public interface TranslateListener {
        void translate(String language, String countryShort);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.select_language_layout, container);
        initView();
        initListener();
        mView.setMinimumWidth(getResources().getDisplayMetrics().widthPixels);
        return mView;
    }

    private void initView() {
        mDefaultLanguage = (TextView) mView.findViewById(R.id.system_default_language);
        mSelectLanguage = (RelativeLayout) mView.findViewById(R.id.select_language);
        mLanguageList = (RecyclerView) mView.findViewById(R.id.language_list);
        mSelectLanguageAdapter = new SelectLanguageAdapter(mContext);
        mArrow = (ImageView) mView.findViewById(R.id.translate_arrow);
        mLanguageList.setLayoutManager(new LinearLayoutManager(mContext));
        mTranslate = (TextView) mView.findViewById(R.id.translate);
        mCancel = (TextView) mView.findViewById(R.id.cancel);
        mLanguageList.setAdapter(mSelectLanguageAdapter);
        setmDefaultLanguage();
    }

    private void initListener() {
        mSelectLanguageAdapter.setOnclickListener(this);
        mDefaultLanguage.setOnClickListener(this);
        mSelectLanguage.setOnClickListener(this);
        mTranslate.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mTranslate.setText(getResources().getString(R.string.webview_translation).toUpperCase());
        mCancel.setText(getResources().getString(R.string.cancel).toUpperCase());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.system_default_language:
            case R.id.select_language:
                selectMode(mIsSelectMode ? false : true);
                break;
            case R.id.translate:
                mTranslateListener.translate(mlanguage, mCountryShort);
            case R.id.cancel:
                dismiss();
                break;
            default:
                dismiss();
        }
    }

    private void setmDefaultLanguage() {
        mDefaultLanguage.setText(Locale.getDefault().getDisplayName());
        mCountryShort = Locale.getDefault().getDisplayCountry();
        mlanguage = Locale.getDefault().getDisplayName();
    }

    private void selectMode(boolean isSelectLanguage) {
        mIsSelectMode = isSelectLanguage;
        mDefaultLanguage.setVisibility(isSelectLanguage ? View.GONE : View.VISIBLE);
        mTranslate.setVisibility(isSelectLanguage ? View.GONE : View.VISIBLE);
        mCancel.setVisibility(isSelectLanguage ? View.GONE : View.VISIBLE);
        mLanguageList.setVisibility(isSelectLanguage ? View.VISIBLE : View.GONE);
        mArrow.setImageResource(isSelectLanguage ? R.drawable.ic_browser_dialog_arrow_up : R.drawable.ic_browser_dialog_arrow_down);
    }

    @Override
    public void translateListener(String language, String mCountry) {
        selectMode(false);
        mDefaultLanguage.setText(language);
        mlanguage = language;
        mCountryShort = mCountry;
    }

    public void setTranslateListener(TranslateListener translateListener) {
        mTranslateListener = translateListener;
    }

    public void setContext(Context context) {
        mContext = context;
    }
}
