package com.blink.browser.download;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.Logger;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.ToastUtil;
import com.wcc.wink.Wink;
import com.wcc.wink.request.DownloadInfo;

import java.io.File;
import java.util.List;

public class DownloadRenamePage extends Activity implements View.OnClickListener {

    private Bundle mMap;
    private long mReferance;
    private DownloadInfo mDownloadItem;
    private EditText mEdit;
    private String mFilePath;
    private String mFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_rename);
        mMap = getIntent().getExtras();
        if (mMap != null) {
            mReferance = mMap.getLong(BrowserContract.DOWNLOAD_REFERANCE, 0);
            mDownloadItem = getDownloadItem(mReferance);
        }
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BrowserSettings.getInstance().getShowStatusBar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams
                    .FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void initView() {
        mEdit = (EditText) findViewById(R.id.filename);
        if (mDownloadItem != null) {
            mFilePath = mDownloadItem.getLocalFilePath();
            mFileName = mDownloadItem.getTitle();
            mEdit.setText(mFileName);
            if (mFileName.lastIndexOf(".") > 0) {
                mEdit.setSelection(mFileName.lastIndexOf("."));
            }
        }

        InputMethodUtils.showKeyboard(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.layout_custom_actionbar);
        View actionbarView = actionBar.getCustomView();
        actionbarView.findViewById(R.id.actionbar_left_icon).setOnClickListener(this);
        TextView actionBarTitle = (TextView) actionbarView.findViewById(R.id.actionbar_title);
        actionBarTitle.setText(R.string.rename);
        ImageView actionBarRightIcon = (ImageView) actionbarView.findViewById(R.id.actionbar_right_icon);
        actionBarRightIcon.setVisibility(View.VISIBLE);
        actionBarRightIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (R.id.actionbar_right_icon == v.getId()) {
            updateDownload();
        } else if (R.id.actionbar_left_icon == v.getId()) {
            finish();
        }
    }

    private void updateDownload() {
        if (mDownloadItem == null) return;

        if (!TextUtils.isEmpty(mFilePath)) {
            String newName = mEdit.getText().toString();
            if (TextUtils.isEmpty(newName)) {
                ToastUtil.showShortToast(this, R.string.filename_empty);
                return;
            }
            String newPath = mFilePath.replace(mFileName, newName);
            Logger.e(newPath);
            Logger.e(mFileName);
            if (FileUtils.fileIsExists(newPath)) {
                ToastUtil.showShortToast(this, R.string.file_exist);
                return;
            }
            if (!mFilePath.equals(newPath)) {
                File file = new File(mFilePath);
                boolean ret = file.renameTo(new File(newPath));
                if (ret) {
                    mDownloadItem.setTitle(newName);
                    mDownloadItem.setLocalFilePath(newPath);
                    Wink.get().updateDownloadInfo(mDownloadItem);
                    ToastUtil.showShortToast(this, R.string.rename_success);
                    finish();
                } else {
                    ToastUtil.showShortToast(this, R.string.rename_failed);
                }
            } else {
                finish();
            }
        }
    }

    private DownloadInfo getDownloadItem(long id) {
        List<DownloadInfo> list = Wink.get().getDownloadedResources();

        if (list != null && list.size() > 0) {
            for (DownloadInfo info :
                    list) {
                if (info.getId() == id) {
                    return info;
                }
            }
        }
        return null;
    }
}
