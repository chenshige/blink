// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blink.browser.R;

import java.util.ArrayList;
import java.util.List;

/**
 * this is select language adapter
 */
public class SelectLanguageAdapter extends RecyclerView.Adapter<SelectLanguageAdapter.SelectLanguageViewHolder> implements View.OnClickListener {
    private Context mContext;
    private translateListener mOnClickListener;
    private List<String> mList;
    private List<String> mCounutry;

    public interface translateListener {
        void translateListener(String language, String mCountry);
    }

    public SelectLanguageAdapter(Context context) {
        mContext = context;
        getDefaultLanguage();
    }

    @Override
    public SelectLanguageAdapter.SelectLanguageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SelectLanguageViewHolder holder = new SelectLanguageViewHolder(LayoutInflater.from(mContext).inflate(R.layout.view_language_list, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(SelectLanguageAdapter.SelectLanguageViewHolder holder, int position) {
        holder.mLanguage.setText(mList.get(position));
        holder.itemView.setOnClickListener(this);
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setOnclickListener(translateListener clickListener) {
        mOnClickListener = clickListener;
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListener != null) {
            mOnClickListener.translateListener(mList.get((int) view.getTag()), mCounutry.get((int) view.getTag()));
        }
    }

    class SelectLanguageViewHolder extends RecyclerView.ViewHolder {
        TextView mLanguage;

        public SelectLanguageViewHolder(View view) {
            super(view);
            mLanguage = (TextView) view.findViewById(R.id.language);
        }
    }

    private void getDefaultLanguage() {
        mList = new ArrayList();
        mCounutry = new ArrayList();
        String[] languageList = mContext.getResources().getStringArray(R.array.language);
        for (int i = 0; i < languageList.length; i++) {
            if (i % 2 == 0) {
                mList.add(languageList[i]);
            } else if (i % 2 == 1) {
                mCounutry.add(languageList[i]);
            }
        }
    }
}
