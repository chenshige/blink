/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * * Neither the name of The Linux Foundation nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.blink.browser.preferences;

import android.content.Context;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.blink.browser.R;
import com.blink.browser.widget.BrowserSwitchButton;

public class BrowserSwitchPreference extends SwitchPreference {
    private final Listener mListener = new Listener();
    private int mVisibility;

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }
            BrowserSwitchPreference.this.setChecked(isChecked);
        }
    }

    public BrowserSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BrowserSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BrowserSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        String summary = (String) getSummary();
        return LayoutInflater.from(getContext()).inflate(TextUtils.isEmpty(summary) ?
                R.layout.browser_switch_preference : R.layout.browser_summary_switch_preference, null);
    }

    public void setDeviderVisibility(int visibility) {
        this.mVisibility = visibility;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        BrowserSwitchButton mSwitch = (BrowserSwitchButton) view.findViewById(R.id.switchbutton);
        if (mSwitch != null) {
            mSwitch.setFocusable(false);
            mSwitch.setChecked(isChecked());
            mSwitch.setOnCheckedChangeListener(mListener);
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.divider);
        if (imageView != null) {
            imageView.setVisibility(mVisibility);
        }

    }
}
