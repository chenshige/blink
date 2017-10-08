// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.R;

import java.util.Map;

/**
 * 多选Dialog
 */
public abstract class BrowserMultiselectDialog extends BrowserDialog
        implements AdapterView.OnItemClickListener {

    private ListView mListView;
    private CharSequence[] mObjects;
    private Map<Integer, String> mSelected = new ArrayMap<>();
    private int mLayoutId;

    public BrowserMultiselectDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public BrowserMultiselectDialog(Context context, CharSequence[] objects) {
        this(context, R.style.BrowserDialog);
        if (objects != null && objects.length > 0) {
            mObjects = objects;
        }
    }

    /**
     * @param context
     * @param layoutId item 布局的id
     * @param objects  内容列表
     */
    public BrowserMultiselectDialog(Context context, int layoutId, CharSequence[] objects) {
        this(context, R.style.BrowserDialog);
        if (objects != null && objects.length > 0) {
            mObjects = objects;
        }

        mLayoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        mListView = new ListView(getContext());
        setBrowserContentView(mListView);

        mListView.setDividerHeight(0);
        mListView.setPadding(0, (int) getContext().getResources().getDimension(R.dimen.dialog_content_padding_top), 0, 0);
        mListView.setOnItemClickListener(this);
        DialogAdapter adapter = new DialogAdapter();
        mListView.setAdapter(adapter);
    }

    public void setValue(Map<Integer, String> selected) {
        if (selected != null && selected.size() > 0) {
            mSelected.clear();
            mSelected.putAll(selected);
        }
    }

    public void setValue(int selected) {
        if (selected >= 0) {
            mSelected.put(selected, selected + "");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!mSelected.containsKey(position)) {
            mSelected.put(position, position + "");
        } else {
            mSelected.remove(position);
        }
        ((DialogAdapter) parent.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onPositiveButtonClick() {
        super.onPositiveButtonClick();
        dialogDismiss(mSelected);
    }

    public abstract void dialogDismiss(Map<Integer, String> selected);

    public class DialogAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mObjects == null ? 0 : mObjects.length;
        }

        @Override
        public Object getItem(int position) {
            return mObjects[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(mLayoutId == 0 ? R.layout.multiselect_dialog_list_item : mLayoutId, null);
                viewHolder.mContent = (TextView) convertView.findViewById(R.id.content_item);
                viewHolder.mImageView = (ImageView) convertView.findViewById(R.id.image);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.mContent.setText((String) getItem(position));
            viewHolder.mImageView.setSelected(mSelected.containsKey(position));

            return convertView;
        }

    }

    class ViewHolder {
        private TextView mContent;
        private ImageView mImageView;
    }
}
