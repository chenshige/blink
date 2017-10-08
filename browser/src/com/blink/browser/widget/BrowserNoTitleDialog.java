package com.blink.browser.widget;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.blink.browser.R;


/**
 * 多选Dialog
 */
public class BrowserNoTitleDialog extends BrowserDialog{

    private String mMassage;

    public BrowserNoTitleDialog(Context context) {
        this(context, R.style.BrowserDialog);
    }

    public BrowserNoTitleDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public BrowserNoTitleDialog(Context context, String content) {
        this(context, R.style.BrowserDialog);
        mMassage = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.browser_notitle_dialog,null);
        setBrowserContentView(view);

        TextView contet = (TextView) view.findViewById(R.id.content_text);
        if (!TextUtils.isEmpty(mMassage)){
            contet.setText(mMassage);
        }
    }


    /**
     * 内容设置
     *
     * @param massge 字符串
     */
    public BrowserDialog setBrowserMessage(String massge) {
        mMassage = massge;
        return this;
    }

    /**
     * 内容设置
     *
     * @param rid 字符串id
     */
    public BrowserDialog setBrowserMessage(int rid) {
        mMassage = getContext().getText(rid).toString();
        return this;
    }

}
