// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.blink.browser.R;

import java.util.List;


public class ImageAdapter {
    public static final int IMAGE_NORMAL = 0;
    public static final int IMAGE_ADD = 1;

    ViewGroup mParent;
    LayoutInflater mInflater;
    private Context mContext;
    private OnImageClickListener mOnImageClickListener;

    public ImageAdapter(ViewGroup parent, OnImageClickListener onImageClickListener) {
        mContext = parent.getContext();
        mInflater = LayoutInflater.from(mContext);
        mParent = parent;
        mOnImageClickListener = onImageClickListener;
    }

    public void clear() {
        mParent.removeAllViews();
    }

    public void addView(final ImageItem imageItem, ViewGroup parent, int position) {
        View convertView;
        ImageHolder holder = new ImageHolder();
        convertView = mInflater.inflate(R.layout.imagechoose, parent, false);
        holder.mRoot = convertView;
        holder.mChoose = (ImageView) convertView.findViewById(R.id.choose);
        holder.mClose = (ImageView) convertView.findViewById(R.id.close);
        holder.mAddIcon = (ImageView) convertView.findViewById(R.id.icon_add);
        holder.mClose.setVisibility(View.GONE);
        int type = imageItem.getType();
        switch (type) {
            case IMAGE_NORMAL:
                holder.mChoose.setImageBitmap(imageItem.getBitmap());
                holder.mClose.setVisibility(View.VISIBLE);
                holder.mAddIcon.setVisibility(View.GONE);
                holder.mClose.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnImageClickListener != null) {
                            mOnImageClickListener.onCloseClick(imageItem);
                        }
                    }
                });
                break;
            case IMAGE_ADD:
                if (position != 3) {
                    convertView.setVisibility(View.VISIBLE);
                    holder.mAddIcon.setVisibility(View.VISIBLE);
                    holder.mChoose.setImageResource(R.drawable.shape_bg_add_image);
                    holder.mRoot.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mOnImageClickListener != null) {
                                mOnImageClickListener.onChooseClick();
                            }
                        }
                    });
                } else {
                    convertView.setVisibility(View.GONE);
                }
                break;

        }

        parent.addView(convertView);
    }

    public void setList(List<ImageItem> list) {
        clear();
        for (int i = 0; i < list.size(); i++) {
            addView(list.get(i), mParent, i);
        }
    }

    public void addList(List<ImageItem> list) {
        for (int i = 0; i < list.size(); i++) {
            addView(list.get(i), mParent, i);
        }
    }

    private static class ImageHolder {
        public ImageView mChoose;
        public ImageView mClose;
        public ImageView mAddIcon;
        public View mRoot;
    }

    public interface OnImageClickListener {
        void onChooseClick();

        void onCloseClick(ImageItem v);
    }

    public static class ImageItem {
        private int mType;
        private Bitmap mBitmap;

        public ImageItem(int type, Bitmap bitmap) {
            this.mType = type;
            this.mBitmap = bitmap;
        }

        public int getType() {
            return mType;
        }

        public void setType(int type) {
            this.mType = type;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public void setBitmap(Bitmap mBitmap) {
            this.mBitmap = mBitmap;
        }
    }

}
