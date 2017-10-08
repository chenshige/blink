package com.blink.browser.homepages.navigation;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.blink.browser.R;
import com.blink.browser.adapter.RecommendAdapter;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.homepages.clone.ClonedView;
import com.blink.browser.homepages.clone.ExtendViewCloneable;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.InputMethodUtils;

/**
 * 修改已有导航的View
 */
public class EditNavigationView extends FrameLayout implements View.OnClickListener, View.OnFocusChangeListener {

    private ClonedView mNavigationItemView;
    private FrameLayout mItemContainer;
    private EditText mTitle;
    private EditText mAddress;
    private View mLine1, mLine2;

    private OnNavigationEditListener mOnNavigationEditListener;
    private RecommendAdapter.CommonUrlItemViewHolder mTargetItemViewHolder;

    public EditNavigationView(Context context) {
        super(context);
    }

    public EditNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init(getContext());
    }

    private void init(Context context) {
        if (DisplayUtil.isScreenPortrait(context)) {
            LayoutInflater.from(context).inflate(R.layout.edit_navigation_view_portrait, this);
        } else {
            LayoutInflater.from(context).inflate(R.layout.edit_navigation_view_landscape, this);
        }
        mItemContainer = (FrameLayout) findViewById(R.id.item_container);
        mNavigationItemView = (ClonedView) findViewById(R.id.item_clone);
        mTitle = (EditText) findViewById(R.id.title);
        mAddress = (EditText) findViewById(R.id.address);
        mTitle.setOnFocusChangeListener(this);
        mAddress.setOnFocusChangeListener(this);
        mLine1 = findViewById(R.id.add_link_line1);
        mLine2 = findViewById(R.id.add_link_line2);
        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.edittext_container).setOnClickListener(this);
        setOnClickListener(this);
    }

    public void setOnNavigationEditListener(OnNavigationEditListener onNavigationEditListener) {
        this.mOnNavigationEditListener = onNavigationEditListener;
    }

    public void initNavigationItem(RecommendAdapter.CommonUrlItemViewHolder itemViewHolder) {
        mTargetItemViewHolder = itemViewHolder;
        RecommendUrlEntity bean = itemViewHolder.data;
        ExtendViewCloneable srcItemView = itemViewHolder.mItemView;
        mNavigationItemView.setViewCloneable(srcItemView);
        Rect layout = srcItemView.getLayout();
        FrameLayout.LayoutParams layoutParams = (LayoutParams) mNavigationItemView.getLayoutParams();
        layoutParams.setMargins(layout.left, layout.top, layout.right, layout.bottom);
        mNavigationItemView.setLayoutParams(layoutParams);
        mNavigationItemView.refresh();
        mTitle.setText(bean.getDisplayName());
        mAddress.setText(bean.getUrl());
    }

    @Override
    public void onClick(View view) {
        InputMethodUtils.hideKeyboard(mTitle);
        onKeyBoardHint();
        switch (view.getId()) {
            case R.id.edittext_container:
                //do nothing
                break;
            case R.id.ok:
                saveEdit();
                break;
            case R.id.cancel:
                if (mOnNavigationEditListener != null) {
                    mOnNavigationEditListener.onCancel();
                }
                break;
            default:
                if (mOnNavigationEditListener != null) {
                    mOnNavigationEditListener.onCancel();
                }
        }
    }

    public void saveEdit() {
        String title = mTitle.getText().toString();
        String url = mAddress.getText().toString();
        if (mOnNavigationEditListener != null && mTargetItemViewHolder != null) {
            mOnNavigationEditListener.onEditCompleted(mTargetItemViewHolder, title, url);
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        switch (view.getId()) {
            case R.id.title:
                mLine1.setBackgroundResource(hasFocus ? R.color.add_book_mark_save : R.color.add_book_mark_line);
                break;
            case R.id.address:
                mLine2.setBackgroundResource(hasFocus ? R.color.add_book_mark_save : R.color.add_book_mark_line);
                String url = mAddress.getText().toString();
                if (hasFocus) {
                    if (TextUtils.isEmpty(url)) {
                        mAddress.setText("http://");
                    }
                } else {
                    if (!TextUtils.isEmpty(url) && "http://".equals(url)) {
                        mAddress.setText("");
                    }
                }
                break;
        }
    }


    public void onKeyBoardShow() {
        mNavigationItemView.setVisibility(INVISIBLE);
        scrollTo(0, mItemContainer.getBottom());

    }

    public void onKeyBoardHint() {
        if (!DisplayUtil.isScreenPortrait(getContext())) {
            return;
        }
        mNavigationItemView.setVisibility(VISIBLE);
        scrollTo(0, 0);
    }

    public interface OnNavigationEditListener {

        void onEditCompleted(RecommendAdapter.CommonUrlItemViewHolder targetItemViewHolder, String title, String url);

        void onCancel();

    }

}
