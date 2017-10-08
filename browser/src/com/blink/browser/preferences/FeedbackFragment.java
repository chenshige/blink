package com.blink.browser.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.adapter.ImageAdapter;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.network.Network;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.ActivityUtils;
import com.blink.browser.util.FormatTools;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.NetworkUtils;
import com.blink.browser.util.SharedPreferencesUtils;
import com.blink.browser.util.StringUtil;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.view.LoadingDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by wenhao on 16-6-11.
 */
public class FeedbackFragment extends BasePreferenceFragment implements View.OnClickListener, ImageAdapter
        .OnImageClickListener, View.OnTouchListener, BrowserPreferencesPage.OnRightIconClickListener {

    private static final int UPDATE_SUCCESS = 0;
    private static final int UPDATE_FAIL = 1;

    private static final int PICK_FROM_FILE = 1;
    private static final int IMAGE_MAX_NUM = 3;
    private static final int TEXT_MAX_NUM = 1000;
    private static final int TEXT_NUM_WHEN_TOAST = 800;

    private View mViews;
    private EditText mFeedbackContent, mEmail;
    private LinearLayout mChoose;
    private LoadingDialog mLoading;
    private BrowserPreferencesPage mPreferencesPage;
    private RelativeLayout mContactUsOnFacebook;
    private TextView mTextCountLimitToast;
    private TextView mOptionalTextView;
    private TextView mCountImageText;

    private Uri mSelectImageUri;
    private List<ImageAdapter.ImageItem> mImageList;
    private ImageAdapter mAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        mViews = inflater.inflate(R.layout.setting_feedback, container, false);
        initView();
        return mViews;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FROM_FILE && data != null) {
            mSelectImageUri = data.getData();
            Bitmap bitmap = FormatTools.getBitmap(mPreferencesPage, mSelectImageUri);
            if (bitmap != null) {
                int imageCount = mImageList.size();
                mImageList.add(mImageList.size() - 1, new ImageAdapter.ImageItem(ImageAdapter.IMAGE_NORMAL, bitmap));
                mAdapter.setList(mImageList);
                mCountImageText.setVisibility(View.VISIBLE);
                mCountImageText.setText(getString(R.string.image_count, imageCount, IMAGE_MAX_NUM));
            } else {
                ToastUtil.showLongToast(mPreferencesPage, R.string.get_photo_failure);
            }

        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String content = (String) SharedPreferencesUtils.get(BrowserContract.FEEDBACK_CONTENT, "");
        if (!TextUtils.isEmpty(content)) {
            mFeedbackContent.setText(content);
        }
        mAdapter = new ImageAdapter(mChoose, this);
        mImageList = new ArrayList<>();
        mImageList.add(new ImageAdapter.ImageItem(ImageAdapter.IMAGE_ADD, null));
        mAdapter.setList(mImageList);
    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_feedback).toString());
    }


    private void initView() {
        mFeedbackContent = (EditText) mViews.findViewById(R.id.feedback_content);
        mChoose = (LinearLayout) mViews.findViewById(R.id.choose_pic);
        mEmail = (EditText) mViews.findViewById(R.id.email);
        mContactUsOnFacebook = (RelativeLayout) mViews.findViewById(R.id.contact_us_on_facebook);
        mTextCountLimitToast = (TextView) mViews.findViewById(R.id.text_count_can_input);
        mOptionalTextView = (TextView) mViews.findViewById(R.id.optional);
        mCountImageText = (TextView) mViews.findViewById(R.id.image_count);
        mPreferencesPage = (BrowserPreferencesPage) getActivity();
        mPreferencesPage.disableRightIcons();
        mPreferencesPage.setRightIcon(R.drawable.ic_browser_complete);
        mPreferencesPage.setOnRightIconClickListener(this);
        mFeedbackContent.setOnClickListener(this);
        mFeedbackContent.setOnTouchListener(this);
        mEmail.setOnClickListener(this);
        mContactUsOnFacebook.setOnClickListener(this);
        mFeedbackContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int textLengthInput = s.length();
                if (textLengthInput < TEXT_NUM_WHEN_TOAST) {
                    mTextCountLimitToast.setVisibility(View.GONE);
                } else if (textLengthInput >= TEXT_NUM_WHEN_TOAST && textLengthInput < TEXT_MAX_NUM) {
                    mTextCountLimitToast.setVisibility(View.VISIBLE);
                    mTextCountLimitToast.setText(getString(R.string.text_count_can_input, TEXT_MAX_NUM - textLengthInput));
                    mTextCountLimitToast.setBackgroundColor(FormatTools.getColor(R.color.count_limit_toast_color_1));
                } else {
                    mTextCountLimitToast.setVisibility(View.VISIBLE);
                    mTextCountLimitToast.setText(getString(R.string.text_count_can_input, 0));
                    mTextCountLimitToast.setBackgroundColor(FormatTools.getColor(R.color.count_limit_toast_color_2));
                }
            }
        });
        mEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mEmail.getEditableText().length() > 0) {
                    mOptionalTextView.setVisibility(View.GONE);
                } else {
                    mOptionalTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        mTextCountLimitToast.setText(getString(R.string.text_count_can_input, TEXT_MAX_NUM));
        mLoading = new LoadingDialog(getActivity());
        mLoading.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mPreferencesPage.setRightIconEnable(true);
            }
        });
    }

    public void sendFeedBack(View item) {
        String feedback = mFeedbackContent.getText().toString().trim();

        if (feedback.length() < 6) {
            mFeedbackContent.setBackgroundResource(R.drawable.shape_feedback_input_error_bg);
            ToastUtil.showLongToast(getActivity(), R.string.please_input_over_six);
            return;
        }
        mFeedbackContent.setBackgroundResource(R.drawable.shape_feedback_input_bg);

        String email = mEmail.getText().toString();
        if (!TextUtils.isEmpty(email) && !StringUtil.isEmail(email)) {
            mEmail.setBackgroundResource(R.drawable.shape_feedback_input_error_bg);
            ToastUtil.showLongToast(getActivity(), R.string.email_is_error);
            return;
        }

        mLoading.show();
        item.setEnabled(false);
        InputMethodUtils.hideKeyboard(getActivity());

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.FEEDBACK_EVENTS, AnalyticsSettings
                .ID_CONTENT, feedback);
    }

    @Override
    public void onRightIconClick(View v) {
        sendFeedBack(v);
    }

    @Override
    public void onSecondRightIconClick(View v) {
        //do nothing
    }


    private static class InnerHandler extends Handler {

        private WeakReference<FeedbackFragment> mFeedBackFragmentHolder;

        public InnerHandler(FeedbackFragment feedbackFragment) {
            this.mFeedBackFragmentHolder = new WeakReference<>(feedbackFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            FeedbackFragment feedbackFragment = mFeedBackFragmentHolder.get();
            if (null == feedbackFragment.getActivity() || feedbackFragment.getActivity().isFinishing()) {
                return;
            }

            switch (msg.what) {
                case UPDATE_SUCCESS: {
                    feedbackFragment.mLoading.dismiss();
                    BrowserPreferencesPage activity = (BrowserPreferencesPage) feedbackFragment.getActivity();
                    activity.setRightIconEnable(true);
                    feedbackFragment.mFeedbackContent.setText(null);
                    SharedPreferencesUtils.put(BrowserContract.FEEDBACK_CONTENT, "");
                    ToastUtil.showShortToast(feedbackFragment.getActivity(), R.string.feed_back_sucess);
                    activity.back();
                }
                break;
                case UPDATE_FAIL: {
                    feedbackFragment.mLoading.dismiss();
                    String feedback = feedbackFragment.mFeedbackContent.getText().toString();
                    SharedPreferencesUtils.put(BrowserContract.FEEDBACK_CONTENT, feedback);
                    if (!NetworkUtils.isNetworkAvailable()) {
                        ToastUtil.showShortToast(feedbackFragment.getActivity(), R.string.check_network);
                    } else {
                        ToastUtil.showShortToast(feedbackFragment.getActivity(), R.string.commit_fail);
                    }
                }
                break;
            }
        }
    }

    private Handler mHandler = new InnerHandler(this);


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.feedback_content:
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.FEEDBACK_EVENTS, AnalyticsSettings
                        .ID_CLICK);
                mFeedbackContent.setBackgroundResource(R.drawable.shape_feedback_input_bg);
                break;
            case R.id.email:
                mEmail.setBackgroundResource(R.drawable.shape_feedback_input_bg);
                break;
            case R.id.contact_us_on_facebook:
                ActivityUtils.openUrl(getActivity(), getString(R.string.contact_facebook));
                break;
            default:
                break;
        }
    }


    @Override
    public void onDestroy() {
        mHandler.removeMessages(UPDATE_SUCCESS);
        mHandler.removeMessages(UPDATE_FAIL);
        InputMethodUtils.hideKeyboard(getActivity());
        super.onDestroy();
    }

    @Override
    public void onChooseClick() {
        if (mImageList != null && mImageList.size() <= IMAGE_MAX_NUM) {
            selectPicFromFile();
        }
    }

    @Override
    public void onCloseClick(ImageAdapter.ImageItem v) {
        if (mImageList != null && mImageList.size() <= IMAGE_MAX_NUM + 1) {
            mImageList.remove(v);
            mAdapter.setList(mImageList);
            if (mImageList.size() == 1) {
                mCountImageText.setVisibility(View.GONE);
            } else {
                mCountImageText.setText(getString(R.string.image_count, mImageList.size() - 1, IMAGE_MAX_NUM));
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.getParent().requestDisallowInterceptTouchEvent(true);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                v.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return false;
    }

    private void selectPicFromFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(intent, getString(R.string.upload_image)),
                PICK_FROM_FILE);
    }

}
