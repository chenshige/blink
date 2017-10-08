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

package com.blink.browser.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.DisplayUtil;

public class SeekBarSummaryPreference extends Preference
        implements OnSeekBarChangeListener {

    private static final int SEEKBAR_MAX_PROGRESS = 100;// value is the default maximum value of the SeekBar

    private int mProgress;
    private int mMax = SEEKBAR_MAX_PROGRESS;

    private TextView mTextViewProgress;
    private SeekBar mSeekBar;
    private CharSequence mSummary;


    public SeekBarSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SeekBarSummaryPreference(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SeekBarSummaryPreference(Context context) {
        this(context,null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.browser_seekbar_preference, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTextViewProgress = (TextView) view.findViewById(
                R.id.progress);

        mSeekBar = (SeekBar) view.findViewById(
                R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(isEnabled());
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mProgress);

        //view加载完成时回调
        mTextViewProgress.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setSummary(mSummary);
            }
        });
    }


    @Override
    public void setSummary(CharSequence summary) {
        if (!TextUtils.isEmpty(summary)){
            this.mSummary = summary;
        }
        if (mTextViewProgress != null && mSeekBar != null) {
            mTextViewProgress.setText(summary);
            float seekbarWidth = mSeekBar.getMeasuredWidth();
            int mThumbOffset = mSeekBar.getThumbOffset();
            float x = mSeekBar.getX();
            float t_x = x + (mSeekBar.getProgress() * (seekbarWidth - mThumbOffset * 2)) / mMax + mThumbOffset;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mTextViewProgress.getLayoutParams();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                params.setMarginStart((int)(t_x - mTextViewProgress.getMeasuredWidth() / 2f));
            }else {
                params.setMargins((int)(t_x - mTextViewProgress.getMeasuredWidth() / 2f), DisplayUtil.dip2px(getContext(),5),0,0);
            }
            
            mTextViewProgress.setLayoutParams(params);
            mTextViewProgress.invalidate();
        }
    }

    @Override
    public CharSequence getSummary() {
        return mSummary;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress)
                : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    public void onProgressChanged(
            SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
