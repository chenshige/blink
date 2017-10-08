// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;


public class ViewHolder {
    private SparseArray<View> mViews;
    private View converterView;

    public ViewHolder(Context context, ViewGroup parent, int layoutId, int position) {
        this.mViews = new SparseArray<View>();
        converterView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        converterView.setTag(this);
    }

    public static ViewHolder get(Context context, View convertView, ViewGroup parent, int layoutId, int postion) {
        if (convertView == null) {
            return new ViewHolder(context, parent, layoutId, postion);
        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            return viewHolder;
        }

    }

    public <T extends View> T getView(int viewId) {
        View view = mViews.get(viewId);
        if (view == null) {
            view = converterView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }

    public View getConverterView() {
        return converterView;
    }

    public ViewHolder setText(int viewId, String text){
        TextView tv = getView(viewId);
        tv.setText(text);
        return this;
    }

    public ViewHolder setCheckBox(int itemCheckBoxId, boolean checked){
        CheckBox cb = getView(itemCheckBoxId);
        cb.setChecked(checked);
        return this;

    }

    /**
     * @param viewId
     * @param listener
     * @return
     */
    public ViewHolder setOnClickListener(int viewId, View.OnClickListener listener){
        View imgV = getView(viewId);
        imgV.setOnClickListener(listener);
        return this;
    }

    public ViewHolder setVisibility(int viewId, int visibility){
        View view = getView(viewId);
        view.setVisibility(visibility);
        return this;
    }

}
