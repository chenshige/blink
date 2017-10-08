// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.UrlUtils;
import com.blink.browser.bean.InputWordBean;

import java.util.ArrayList;
import java.util.List;

/**
 * this is input word adapter
 */
public class InputWordAdapter extends RecyclerView.Adapter<InputWordAdapter.InputWordViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<InputWordBean> mList = new ArrayList<>();
    private InputWordListener mOnClickListener;

    public interface InputWordListener {
        void openUrl(String url);

        void deleteWord(int id);
    }

    public InputWordAdapter(Context context) {
        mContext = context;
    }

    @Override
    public InputWordAdapter.InputWordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        InputWordViewHolder holder = new InputWordViewHolder(LayoutInflater.from(mContext).inflate(R.layout.view_input_url, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(InputWordAdapter.InputWordViewHolder holder, int position) {
        holder.mInputWordDeleteIcon.setOnClickListener(this);
        holder.itemView.setOnClickListener(this);
        holder.mInputMark.setImageResource(UrlUtils.isSearch(mList.get(position).getmInputWord()) ? R.drawable.ic_browser_search_icon : R.drawable.ic_browser_search_url_default_icon);
        holder.mInputWordContent.setText(mList.get(position).getmInputWord());
        holder.itemView.setTag(position);
        holder.mInputWordDeleteIcon.setTag(position);

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setOnclickListener(InputWordListener clickListener) {
        mOnClickListener = clickListener;
    }

    public void setData(List<InputWordBean> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.hot_word_delete_icon:
                mOnClickListener.deleteWord(mList.get((int) view.getTag()).getmIndex());
                break;
            case R.id.hot_word_layout:
                mOnClickListener.openUrl(mList.get((int) view.getTag()).getmInputWord().trim());
                break;
        }

    }

    class InputWordViewHolder extends RecyclerView.ViewHolder {
        ImageView mInputWordDeleteIcon;
        TextView mInputWordContent;
        RelativeLayout mLayout;
        ImageView mInputMark;

        public InputWordViewHolder(View view) {
            super(view);
            mInputWordDeleteIcon = (ImageView) view.findViewById(R.id.hot_word_delete_icon);
            mInputWordContent = (TextView) view.findViewById(R.id.hot_word_content);
            mInputMark = (ImageView) view.findViewById(R.id.url_search_input_icon);
            mLayout = (RelativeLayout) view.findViewById(R.id.hot_word_layout);
        }
    }
}
