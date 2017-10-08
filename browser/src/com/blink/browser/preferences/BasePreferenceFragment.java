/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.blink.browser.preferences;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.view.FractionTranslateLayout;

import java.lang.reflect.Field;

public abstract class BasePreferenceFragment extends PreferenceFragment {
    public static final String KEY = "key";

    public static final int ANIMA_MODE_NORMAL = 0;
    public static final int ANIMA_MODE_LEFT = 1;
    public static final int ANIMA_MODE_RIGHT = 2;

    private Handler mHandler;
    public int mAnimaMode = ANIMA_MODE_LEFT;
    public ListView mList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = super.onCreateView(inflater, container, bundle);
        if (view != null) {
            view.setBackgroundResource(R.color.settings_background);
            mList = (ListView) view.findViewById(android.R.id.list);
        }

        if (mList == null) {
            return view;
        }

        mList.setBackgroundResource(R.color.white);
        mList.setPadding(0, mList.getPaddingTop(), 0, mList.getPaddingBottom());
        mList.setDivider(null);
        mList.setDividerHeight(0);
        mList.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        mList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mList.setVerticalScrollBarEnabled(false);
        mList.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mList.setDivider(null);
                mList.setDividerHeight(0);
            }
        });

        mList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                                               @Override
                                               public void onChildViewAdded(View parent, View child) {
                                                   //TODO: Temporary turn off
//                                                   findAndResizeSwitchPreferenceWidget(parent);
                                                   onChildViewAddedToHierarchy(parent, child);
                                               }

                                               @Override
                                               public void onChildViewRemoved(View parent, View child) {
                                                   onChildViewRemovedFromHierarchy(parent, child);
                                               }
                                           }
        );

        FractionTranslateLayout layout = new FractionTranslateLayout(getActivity());
        layout.addView(view);

        return layout;
    }

    protected ListView getList() {
        return mList;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAnimaMode != ANIMA_MODE_RIGHT) {
            mAnimaMode = ANIMA_MODE_NORMAL;
        }
    }


    public void setBrowserActionBarTitle(final String title) {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            final TextView actionBarTitle = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_title);
            if (TextUtils.isEmpty(title)) {
                return;
            }

            if (TextUtils.isEmpty(actionBarTitle.getText().toString())) {
                actionBarTitle.setText(title);
                return;
            }

            if (mHandler == null) {
                mHandler = new Handler();
            }

            switch (mAnimaMode) {
                case ANIMA_MODE_NORMAL:
                    actionBarTitle.setText(title);
                    break;
                case ANIMA_MODE_LEFT:
                    startInAnima(actionBarTitle);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            actionBarTitle.setText(title);
                        }
                    }, 150);
                    break;
                case ANIMA_MODE_RIGHT:
                    startOutAnima(actionBarTitle);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            actionBarTitle.setText(title);
                        }
                    }, 150);
                    break;
            }
        }
    }

    private void findAndResizeSwitchPreferenceWidget(View parent) {
        LinearLayout layout = (LinearLayout) parent.findViewById(android.R.id.widget_frame);
        if (layout != null) {
            for (int i = 0; i < layout.getChildCount(); i++) {
                View view = layout.getChildAt(i);
                if (view instanceof Switch) {
                    Switch switchView = (Switch) view;
                    switchView.setThumbTextPadding(0);
                    int width = switchView.getSwitchMinWidth();
                    switchView.setSwitchMinWidth(width / 2);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.setting_fragment_slide_in_right,
                R.animator.setting_fragment_slide_out_left,
                R.animator.setting_fragment_slide_in_left,
                R.animator.setting_fragment_slide_out_right);
        if (!fragment.isAdded()) {
            fragmentTransaction.replace(getId(), fragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss();
        } else {
            fragmentTransaction.show(fragment);
        }

        mAnimaMode = ANIMA_MODE_RIGHT;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void onChildViewAddedToHierarchy(View parent, View child) {

    }

    public void onChildViewRemovedFromHierarchy(View parent, View child) {

    }

    private void startInAnima(View v) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator anim_alpha_left = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0f);
        anim_alpha_left.setDuration(getTitleAnimaDuration());
        ObjectAnimator anim_alpha_right = ObjectAnimator.ofFloat(v, "alpha", 0f, 1.0f);
        anim_alpha_right.setDuration(getTitleAnimaDuration());
        set.play(anim_alpha_left).before(anim_alpha_right);
        set.start();
    }

    private void startOutAnima(View v) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator anim_alpha_left = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0f);
        anim_alpha_left.setDuration(getTitleAnimaDuration());
        ObjectAnimator anim_alpha_right = ObjectAnimator.ofFloat(v, "alpha", 0f, 1.0f);
        anim_alpha_right.setDuration(getTitleAnimaDuration());
        set.play(anim_alpha_left).before(anim_alpha_right);
        set.start();
    }

    private long getTitleAnimaDuration() {
        return getResources().getInteger(R.integer.settings_fragment_anima_time) / 2;
    }


    public boolean onBackPress() {
        return false;
    }

    public void finish() {
        BrowserPreferencesPage preferencesPage = (BrowserPreferencesPage) getActivity();
        if (preferencesPage != null) {
            preferencesPage.back();
            onBackPress();
        }
    }
}
