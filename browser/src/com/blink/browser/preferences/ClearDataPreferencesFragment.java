package com.blink.browser.preferences;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;

import java.util.ArrayList;
import java.util.List;

/**
 * clear data preferences
 */
public class ClearDataPreferencesFragment extends BasePreferenceFragment {
    private static final int DATA_SIZE = 6;

    private ClearDataAdapter mClearDataAdapter;
    private Button mClearAllButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View root = super.onCreateView(inflater, container, bundle);
        init();
        return root;
    }

    private void init() {
        final List<Data> dataList = new ArrayList<>(DATA_SIZE);
        String[] clearDataList = getResources().getStringArray(R.array.pref_clear_data_choices);
        String[] clearOptionList = getResources().getStringArray(R.array.pref_clear_data_values);
        String[] clearDescriptionList = getResources().getStringArray(R.array.pref_clear_data_descriptions);
        for (int i = 0; i < clearDataList.length; i++) {
            String dataName = clearDataList[i];
            String clearOption = clearOptionList[i];
            String clearDescription = clearDescriptionList[i];
            dataList.add(new Data(dataName, clearDescription, clearOption));
        }
        ListView list = getList();
        if (list == null) {
            return;
        }
        mClearDataAdapter = new ClearDataAdapter(getActivity(), dataList);
        list.setBackgroundResource(R.color.settings_background);
        list.setAdapter(mClearDataAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowserActionBarTitle(getText(R.string.pref_privacy_clear_data).toString());
    }

    private class ClearDataAdapter extends BaseAdapter {

        private static final int TYPE_CLEAR_ITEM = 0;
        private static final int TYPE_CLEAR_BUTTON = 1;
        private LayoutInflater mInflater;
        private List<Data> mDataList;

        public ClearDataAdapter(Context context, List<Data> dataList) {
            mInflater = LayoutInflater.from(context);
            this.mDataList = dataList;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < mDataList.size()) {
                return TYPE_CLEAR_ITEM;
            } else {
                return TYPE_CLEAR_BUTTON;
            }
        }

        @Override
        public int getCount() {
            return mDataList == null ? 0 : mDataList.size() + 1;
        }

        @Override
        public Data getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            switch (getItemViewType(position)) {
                case TYPE_CLEAR_ITEM:
                    return createClearDataItemView(position, parent);
                case TYPE_CLEAR_BUTTON:
                    return createListViewFoot(parent);
                default:
                    return createClearDataItemView(position, parent);
            }
        }

        private View createClearDataItemView(int position, ViewGroup parent) {
            View itemView = mInflater.inflate(R.layout.item_clear_data, parent, false);
            ViewHolder viewHolder = new ViewHolder(itemView);
            viewHolder.setData(getItem(position));
            //不是最后一个，就显示分割线
            viewHolder.showDivider(position != (mDataList.size() - 1));
            return viewHolder.mItemView;
        }

        private View createListViewFoot(ViewGroup parent) {
            View foot = LayoutInflater.from(getActivity()).inflate(R.layout.clear_button, parent, false);
            mClearAllButton = (Button) foot.findViewById(R.id.clear_all_button);
            if(isClearAllAlready()){
                mClearAllButton.setText(R.string.clear_all_already);
                mClearAllButton.setEnabled(false);
            }
            mClearAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BrowserSettings.getInstance().clearAllData();
                    for (Data data : mDataList) {
                        data.mIsCleared = true;
                    }
                    mClearDataAdapter.notifyDataSetChanged();
                    BrowserAnalytics.trackEvent(BrowserAnalytics.Event.CLEARDATA_EVENTS, AnalyticsSettings
                            .ID_CLEARALL);
                }
            });
            return foot;
        }

        public boolean isClearAllAlready() {
            for (Data data : mDataList) {
                if (!data.mIsCleared) {
                    return false;
                }
            }
            return true;
        }
    }

    private class ViewHolder implements View.OnClickListener {
        private View mItemView;
        private TextView mNameTextView;
        private TextView mDescriptionTextView;
        private TextView mClearTextView;
        private View mDivider;
        private Data mData;

        public ViewHolder(View itemView) {
            mItemView = itemView;
            mNameTextView = (TextView) itemView.findViewById(R.id.data_name);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.data_description);
            mClearTextView = (TextView) itemView.findViewById(R.id.clear_text);
            mDivider = itemView.findViewById(R.id.divider);
            mClearTextView.setOnClickListener(this);
        }

        private void onCleared() {
            mClearTextView.setText(getText(R.string.already_cleared));
            mClearTextView.setEnabled(false);
        }

        public void setData(Data data) {
            this.mData = data;
            mNameTextView.setText(data.mName);
            mDescriptionTextView.setText(data.mDescription);
            if (data.mIsCleared) {
                onCleared();
            }
        }

        @Override
        public void onClick(View v) {
            BrowserSettings.getInstance().clearData(mData.mClearOption);
            mData.mIsCleared = true;
            onCleared();
            if (mClearAllButton != null && mClearDataAdapter.isClearAllAlready()) {
                mClearAllButton.setEnabled(false);
                mClearAllButton.setText(R.string.clear_all_already);
            }
        }

        public void showDivider(boolean isShow) {
            mDivider.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    private static class Data {

        private String mName;
        private String mDescription;
        private String mClearOption;
        private boolean mIsCleared = false;

        public Data(String name, String description, String clearOption) {
            this.mName = name;
            this.mDescription = description;
            this.mClearOption = clearOption;
        }

    }

}
