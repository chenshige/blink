package com.blink.browser.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blink.browser.R;
import com.blink.browser.provider.BrowserContract;
import com.blink.browser.util.DefaultBrowserSetUtils;

public class SetDefaultBrowserFragment extends BasePreferenceFragment implements View.OnClickListener {


    public static SetDefaultBrowserFragment create() {
        return new SetDefaultBrowserFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_set_default_browser, container, false);
        setBrowserActionBarTitle(getString(R.string.set_default_browser));
        view.findViewById(R.id.to_set_default_browser).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.to_set_default_browser){
            DefaultBrowserSetUtils.openOneUrlToSetDefaultBrowser(getActivity(), BrowserContract.EMPTY_WEB_URL);
        }
    }
}
