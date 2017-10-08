package com.blink.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.R;

public class SearchInputHintView extends LinearLayout implements View.OnClickListener {
    private String[] mHintBeforeInput;
    private String[] mHintAfterInput;
    private TextView mHintTextView1, mHintTextView2,
            mHintTextView3, mHintTextView4, mHintTextView5;
    private IHintClickListener mHintListener;

    public SearchInputHintView(Context context) {
        super(context);
        init();
    }

    public SearchInputHintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHintTextView1 = (TextView) findViewById(R.id.hint1);
        mHintTextView2 = (TextView) findViewById(R.id.hint2);
        mHintTextView3 = (TextView) findViewById(R.id.hint3);
        mHintTextView4 = (TextView) findViewById(R.id.hint4);
        mHintTextView5 = (TextView) findViewById(R.id.hint5);

        mHintTextView1.setOnClickListener(this);
        mHintTextView2.setOnClickListener(this);
        mHintTextView3.setOnClickListener(this);
        mHintTextView4.setOnClickListener(this);
        mHintTextView5.setOnClickListener(this);
        setDataBefore();
    }

    private void init() {
        mHintBeforeInput = getContext().getResources().getStringArray(R.array.search_input_hint_before);
        mHintAfterInput = getContext().getResources().getStringArray(R.array.search_input_hint_after);
    }

    public void setDataBefore() {
        mHintTextView1.setText(mHintBeforeInput[0]);
        mHintTextView2.setText(mHintBeforeInput[1]);
        mHintTextView3.setText(mHintBeforeInput[2]);
        mHintTextView4.setText(mHintBeforeInput[3]);
        mHintTextView5.setText(mHintBeforeInput[4]);
    }

    public void setDataAfter() {
        mHintTextView1.setText(mHintAfterInput[0]);
        mHintTextView2.setText(mHintAfterInput[1]);
        mHintTextView3.setText(mHintAfterInput[2]);
        mHintTextView4.setText(mHintAfterInput[3]);
        mHintTextView5.setText(mHintAfterInput[4]);
    }


    @Override
    public void onClick(View view) {
        if (mHintListener != null && view instanceof TextView) {
            mHintListener.onHintClick(((TextView) view).getText().toString());
        }
    }

    public void registerHintListener(IHintClickListener listener) {
        mHintListener = listener;
    }

    public interface IHintClickListener {
        void onHintClick(String text);
    }

    public void setStyleMode(boolean incognito) {
        this.setBackgroundColor(incognito ? getResources().getColor(R.color.search_input_prompt_background_color_incognito) : getResources().getColor(R.color.search_input_prompt_background_color));
        mHintTextView1.setTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.search_url_input_color_incognito));
        mHintTextView2.setTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.search_url_input_color_incognito));
        mHintTextView3.setTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.search_url_input_color_incognito));
        mHintTextView4.setTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.search_url_input_color_incognito));
        mHintTextView5.setTextColor(incognito ? getResources().getColor(R.color.search_input_text_color_incognito) : getResources().getColor(R.color.search_url_input_color_incognito));
    }
}
