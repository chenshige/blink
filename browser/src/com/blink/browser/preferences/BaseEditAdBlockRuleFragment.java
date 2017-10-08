package com.blink.browser.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.widget.BrowserDialog;

import java.util.List;

/**
 * 自定义广告过滤规则
 */

@SuppressLint("ValidFragment")
public abstract class BaseEditAdBlockRuleFragment extends BasePreferenceFragment implements BrowserPreferencesPage.OnRightIconClickListener {

    private static final float DELETE_ICON_DISABLE_ALPHA = 0.5f;
    private static final float DELETE_ICON_ENABLE_ALPHA = 1f;

    private int mTitleRes;

    protected abstract List<String> readRules(Context context);

    protected abstract void writeRules(Context context, List<String> rules);

    protected abstract View createView(LayoutInflater inflater, ViewGroup container);

    protected abstract ListView getListView();

    protected abstract void addData(List<String> dataList, String newData);

    protected abstract void onDataListChange(List<String> dataList);

    protected abstract String getDialogInputHint();

    protected abstract String getInputFilteredPrompt();

    protected abstract boolean checkInputLineCanSave(String line);

    protected abstract boolean checkInputLineIsIllegal(String line);

    private AdBlockRuleAdapter mRuleAdapter;

    private List<String> mDataList;

    private BrowserPreferencesPage mPreferencesPage;

    public BaseEditAdBlockRuleFragment(int titleRes) {
        this.mTitleRes = titleRes;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View root = createView(inflater, container);
        init();
        return root;
    }

    private void init() {
        mPreferencesPage = (BrowserPreferencesPage) getActivity();
        mPreferencesPage.setOnRightIconClickListener(this);
        initTitle();
        initList();
        refreshDeleteIcon();
    }

    private void refreshDeleteIcon() {
        ImageView rightActionbarIcon = mPreferencesPage.getRightActionbarIcon();
        if (mDataList.size() == 0) {
            rightActionbarIcon.setEnabled(false);
            rightActionbarIcon.setAlpha(DELETE_ICON_DISABLE_ALPHA);
        } else {
            rightActionbarIcon.setEnabled(true);
            rightActionbarIcon.setAlpha(DELETE_ICON_ENABLE_ALPHA);
        }
    }

    private interface OnClickInputDialogOk {
        void onClickOk(String input);
    }

    protected BrowserDialog createInputDialog(@StringRes int title, String hint, String defaultText, final OnClickInputDialogOk onClickInputDialogOk) {
        BrowserDialog inputDialog = new BrowserDialog(mPreferencesPage);
        View dialogContent = View.inflate(mPreferencesPage, R.layout.dialog_input_rule, null);
        final EditText editText = (EditText) dialogContent.findViewById(R.id.input);
        final View inputLine = dialogContent.findViewById(R.id.input_line);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputLine.setBackgroundResource(R.color.add_book_mark_save);
            }
        });
        if (!TextUtils.isEmpty(defaultText)) {
            editText.setText(defaultText);
        }
        if (!TextUtils.isEmpty(hint))
            editText.setHint(hint);
        inputDialog.setBrowserContentView(dialogContent);
        inputDialog.setBrowserTitle(getString(title))
                .setBrowserNegativeButton(getText(R.string.cancel).toString())
                .setBrowserPositiveButton(getText(R.string.ok).toString())
                .setBrowserPositiveButtonListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString();
                        if (onClickInputDialogOk != null) {
                            onClickInputDialogOk.onClickOk(input);
                        }
                    }
                });
        return inputDialog;
    }

    private void initTitle() {
        setBrowserActionBarTitle(getString(mTitleRes));
        mPreferencesPage.setRightIcon(R.drawable.ic_browser_bookmark_menu_delete);
        mPreferencesPage.setSecondRightIcon(R.drawable.ic_browser_toolbar_close02);
    }

    private void initList() {
        Context context = getActivity();
        ListView list = getListView();
        mDataList = readRules(context);
        mRuleAdapter = new AdBlockRuleAdapter(context);
        onDataListChange(mDataList);
        list.setAdapter(mRuleAdapter);
    }

    private void onDataChange() {
        onDataListChange(mDataList);
        refreshDeleteIcon();
        mRuleAdapter.notifyDataSetChanged();
        writeRules(getActivity(), mDataList);
    }

    private void writeToList(String input) {
        String[] rulesArray = input.trim().split("[\\n ]");
        boolean hasIllegalLine = false;
        for (String rule : rulesArray) {
            if (!TextUtils.isEmpty(rule) && !mDataList.contains(rule) && checkInputLineCanSave(rule)) {
                addData(mDataList, rule);
            }

            if (checkInputLineIsIllegal(rule)) {
                hasIllegalLine = true;
            }
        }
        if (hasIllegalLine) {
            ToastUtil.showShortToastByString(getActivity(), getInputFilteredPrompt());
        }
    }

    @Override
    public void onRightIconClick(View v) {
        deleteAll();
    }

    public void deleteAll() {
        BrowserDialog dialog = new BrowserDialog(mPreferencesPage);
        dialog.setBrowserTitle(getString(R.string.confirm_deletion_all));
        dialog.setBrowserPositiveButton(R.string.ok)
                .setBrowserPositiveButtonListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDataList.clear();
                        onDataChange();
                    }
                }).show();
    }

    @Override
    public void onSecondRightIconClick(View v) {
        createInputDialog(R.string.add, getDialogInputHint(), null, new OnClickInputDialogOk() {
            @Override
            public void onClickOk(String input) {
                writeToList(input);
                onDataChange();
            }
        }).show();
    }


    private class AdBlockRuleAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        AdBlockRuleAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mDataList == null ? 0 : mDataList.size();
        }

        @Override
        public String getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void remove(String data) {
            mDataList.remove(data);
            onDataChange();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
            } else {
                viewHolder = createAdBlockRuleItemView(parent);
            }
            viewHolder.setData(position, getItem(position));
            return viewHolder.mItemView;
        }

        private ViewHolder createAdBlockRuleItemView(ViewGroup parent) {
            View itemView = mInflater.inflate(R.layout.item_rule, parent, false);
            return new ViewHolder(itemView);
        }
    }

    private class ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private View mItemView;
        private TextView mRuleTextView;
        private String mRule;
        private int mPosition;

        public ViewHolder(View itemView) {
            mItemView = itemView;
            mRuleTextView = (TextView) itemView.findViewById(R.id.rule_text);
            itemView.setTag(this);
        }

        public void setData(int position, String rule) {
            this.mPosition = position;
            this.mRule = rule;
            mRuleTextView.setText(mRule);
            mItemView.setOnClickListener(this);
            mItemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            createInputDialog(R.string.modify, null, mRule, new OnClickInputDialogOk() {
                @Override
                public void onClickOk(String input) {
                    mDataList.set(mPosition, input);
                    mRuleAdapter.notifyDataSetChanged();
                }
            }).show();
        }

        @Override
        public boolean onLongClick(View v) {
            BrowserDialog dialog = new BrowserDialog(mPreferencesPage);
            dialog.setBrowserTitle(getText(R.string.confirm_deletion).toString());
            dialog.setBrowserPositiveButton(R.string.ok)
                    .setBrowserPositiveButtonListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mRuleAdapter.remove(mRule);
                        }
                    }).show();
            return true;
        }
    }
}
