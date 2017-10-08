package com.blink.browser.homepages;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.blink.browser.R;

public class MultiSelectView extends FrameLayout {

    private ImageView mDeleteSelected;
    private ImageView mClose;
    private View mSelectAll;
    private ImageView imageView;


    public MultiSelectView(Context context) {
        super(context);
        initView();
    }

    public MultiSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MultiSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        View.inflate(getContext(), R.layout.view_recommend_select, this);
        mDeleteSelected = (ImageView) findViewById(R.id.delete_all_selected);
        mClose = (ImageView) findViewById(R.id.window_close);
        mSelectAll = findViewById(R.id.layout_select_all);
        imageView = (ImageView) mSelectAll.findViewById(R.id.recommend_select);

    }

    public void registListener(View.OnClickListener listener) {
        mDeleteSelected.setOnClickListener(listener);
        mClose.setOnClickListener(listener);
        mSelectAll.setOnClickListener(listener);
    }

    public void reset() {
        imageView.setImageResource(R.drawable.ic_browser_delete_apps_check_box_normal);
    }

    public void setSelected(boolean selected) {
        if (selected)
            imageView.setImageResource(R.drawable.ic_browser_delete_apps_check_box_selected);
        else
            imageView.setImageResource(R.drawable.ic_browser_delete_apps_check_box_normal);
    }

}
