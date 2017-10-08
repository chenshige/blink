// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StartPageActivity extends Activity implements Runnable{

    private static final int LOADING_TIME = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.start_page_in, R.anim.start_page_out);
        setContentView(R.layout.activity_start);
        BackgroundHandler.getMainHandler().postDelayed(this, LOADING_TIME);
    }

    @Override
    public void run() {
        Intent intent = getIntent();
        intent.setClass(this, BrowserActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.start_page_in, R.anim.start_page_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BackgroundHandler.getMainHandler().removeCallbacks(this);
    }
}
