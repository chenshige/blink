package com.blink.browser.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.R;

/**
 * 单选Dialog
 */
public abstract class BrowserRadioDialog extends BrowserDialog implements AdapterView.OnItemClickListener {

    private ListView mListView;
    private CharSequence[] mObjects;
    private int mSelected;

    public BrowserRadioDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public BrowserRadioDialog(Context context, CharSequence[] objects, int defaultValue) {
        this(context, R.style.BrowserDialog);
        if (objects != null && objects.length > 0) {
            mObjects = objects;
        }

        setDefaultValue(defaultValue);
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

    public void setDefaultValue(int defaultValue) {
        mSelected = defaultValue == -1 ? 0 : defaultValue;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelected = position;
        ((DialogAdapter) parent.getAdapter()).notifyDataSetChanged();
        dialogDismiss(position);

        //50ms 延迟关闭Dialog,是让用户看到选中后Radio状态变化
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 50);
    }

    public void dismiss() {
        super.dismiss();
    }

    public abstract void dialogDismiss(int selected);

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
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.radio_dialog_list_item, null);
                viewHolder.mContent = (TextView) convertView.findViewById(R.id.content_item);
                viewHolder.mImageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.mContent.setText((String) getItem(position));
            viewHolder.mImageView.setSelected(mSelected == position);

            return convertView;
        }
    }

    class ViewHolder {
        private TextView mContent;
        private ImageView mImageView;
    }

}
