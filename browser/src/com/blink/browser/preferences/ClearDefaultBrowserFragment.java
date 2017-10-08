package com.blink.browser.preferences;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.R;
import com.blink.browser.util.DefaultBrowserSetUtils;
import com.blink.browser.util.ToastUtil;

/**
 * Page to set default browser
 */
public class ClearDefaultBrowserFragment extends BasePreferenceFragment implements View.OnClickListener {

    private static final String KEY_DEFAULT_BROWSER_INFO = "defaultBrowserInfo";
    private static final String KEY_DEFAULT_BROWSER_PKG_NAME = "defaultBrowserPkgName";

    private ImageView mAppIcon;
    private TextView mAppName;

    private Handler mHandler = new Handler();

    public static ClearDefaultBrowserFragment create(ResolveInfo defaultBrowserInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DEFAULT_BROWSER_INFO, defaultBrowserInfo);
        bundle.putString(KEY_DEFAULT_BROWSER_PKG_NAME, defaultBrowserInfo.activityInfo.packageName);
        ClearDefaultBrowserFragment clearDefaultBrowserFragment = new ClearDefaultBrowserFragment();
        clearDefaultBrowserFragment.setArguments(bundle);
        return clearDefaultBrowserFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_clear_default_browser, container, false);
        setBrowserActionBarTitle(getString(R.string.clear_default_browser));
        findView(view);
        bindingEventListener(view);
        initView(getArguments());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DefaultBrowserSetUtils.isThereNoDefaultBrowser(getActivity())) {
            onClearSuccess();
        }
    }

    private void onClearSuccess() {
        ToastUtil.showShortToast(getActivity(), R.string.clear_data_success);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 800);//finish after 800 ms.
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear:
                Bundle arguments = getArguments();
                String defaultBrowserPkgName = arguments.getString(KEY_DEFAULT_BROWSER_PKG_NAME);
                DefaultBrowserSetUtils.openAppInfoSettingView(getActivity(), defaultBrowserPkgName);
                break;
            case R.id.actionbar_left:
                finish();
                break;
            default:
                //do nothing.
        }
    }

    private void findView(View view) {
        mAppIcon = (ImageView) view.findViewById(R.id.app_icon);
        mAppName = (TextView) view.findViewById(R.id.app_name);
    }

    private void bindingEventListener(View view) {
        TextView btnOK = (TextView) view.findViewById(R.id.clear);
        btnOK.setOnClickListener(this);
    }


    private void initView(Bundle intent) {
        Parcelable parcelable = intent.getParcelable(KEY_DEFAULT_BROWSER_INFO);
        ResolveInfo defaultBrowserInfo = null;
        if (parcelable instanceof ResolveInfo) {
            defaultBrowserInfo = ((ResolveInfo) parcelable);
        }
        if (defaultBrowserInfo != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            Drawable appIcon = defaultBrowserInfo.loadIcon(packageManager);
            mAppIcon.setImageDrawable(appIcon);
            CharSequence appName = defaultBrowserInfo.loadLabel(packageManager);
            mAppName.setText(appName);
        } else {
            ToastUtil.showShortToast(getActivity(), R.string.unknown_error);
            finish();
        }
    }


}
