package com.blink.browser.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.adblock.AdBlockDataUpdator;
import com.blink.browser.util.StringUtil;
import com.blink.browser.util.WebAddress;

import java.util.Collections;
import java.util.List;

/**
 * 纯净模式
 */

public class EditJsDisableHostsFragment extends BaseEditAdBlockRuleFragment {

    private ListView mListView;

    private ImageView mNullDataIcon;

    private TextView mNullDataText;

    public EditJsDisableHostsFragment() {
        super(R.string.static_webpage);
    }

    @Override
    public List<String> readRules(Context context) {
        return AdBlockDataUpdator.getInstance().readDisableJsHosts();
    }

    @Override
    public void writeRules(Context context, List<String> rules) {
        AdBlockDataUpdator.getInstance().writeDisableJsHosts(rules);
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        View rootView = inflater.inflate(R.layout.js_disable_host_list, container, false);
        mListView = (ListView) rootView.findViewById(R.id.list);
        mNullDataIcon = (ImageView) rootView.findViewById(R.id.null_domains_prompt);
        mNullDataText = (TextView) rootView.findViewById(R.id.null_domains_prompt_text);
        return rootView;
    }

    @Override
    public ListView getListView() {
        return mListView;
    }

    @Override
    public void addData(List<String> dataList, String newData) {
        dataList.add(newData);
    }

    @Override
    public void onDataListChange(List<String> dataList) {
        int size = dataList.size();
        if (size == 0) {
            mNullDataIcon.setVisibility(View.VISIBLE);
            mNullDataText.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            Collections.sort(dataList);
            mNullDataIcon.setVisibility(View.GONE);
            mNullDataText.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public String getDialogInputHint() {
        return getString(R.string.example_host);
    }

    @Override
    protected String getInputFilteredPrompt() {
        return getString(R.string.js_disable_host_error_prompt);
    }

    @Override
    public boolean checkInputLineCanSave(String line) {
        if (!StringUtil.checkAsciiText(line)) return false;
        try {
            WebAddress address = new WebAddress(line);
            String host = address.getHost();
            if (TextUtils.isEmpty(host) || !host.contains(".") || host.endsWith(".")) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkInputLineIsIllegal(String line) {
        return !checkInputLineCanSave(line);
    }

}
