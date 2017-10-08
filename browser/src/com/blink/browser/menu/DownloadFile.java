package com.blink.browser.menu;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.blink.browser.R;

public class DownloadFile extends RelativeLayout {
    private Point mTarget;
    private int mFrame = 60;
    private ImageView mFile;
    private View mMenu;

    public DownloadFile(Context context) {
        super(context);
    }

    public DownloadFile(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    private void initView() {
        mFile = (ImageView) findViewById(R.id.file_icon);
        mMenu = findViewById(R.id.menu_toast);
    }

    public void setTarget(Point mTarget) {
        this.mTarget = new Point(mTarget.x - 36, mTarget.y - 36);
    }

    public void moveAnimation() {
        setVisibility(VISIBLE);
        mFile.setVisibility(View.VISIBLE);
        mMenu.setVisibility(View.GONE);

        float xs[] = new float[mFrame + 1];
        float ys[] = new float[mFrame + 1];

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        Point startP = new Point(dm.widthPixels / 2, dm.heightPixels / 2);

        calculateShowXY(startP, mTarget, xs, ys);

        ObjectAnimator animatorX = ObjectAnimator.ofFloat(this, "x", xs);
        animatorX.setDuration(1000);

        ObjectAnimator animatorY = ObjectAnimator.ofFloat(this, "y", ys);
        animatorY.setDuration(1000);

        animatorX.start();
        animatorY.start();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFile.setVisibility(View.GONE);
                mMenu.setVisibility(View.VISIBLE);

                ObjectAnimator animatorX = ObjectAnimator.ofFloat(mMenu, "scaleX", 1, 0);
                animatorX.setDuration(500);
                ObjectAnimator animatorY = ObjectAnimator.ofFloat(mMenu, "scaleY", 1, 0);
                animatorY.setDuration(500);

                ObjectAnimator alpha = ObjectAnimator.ofFloat(mMenu, "alpha", 1, 0);
                alpha.setDuration(500);

                animatorX.start();
                animatorY.start();
                alpha.start();


            }
        }, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setVisibility(View.GONE);
            }
        }, 1500);
    }

    public void calculateShowXY(Point startPosition, Point endPosition, float[] xs, float[] ys) {
        float x1 = startPosition.x;
        float y1 = startPosition.y;
        float x2 = endPosition.x;
        float y2 = endPosition.y;
        float p = 1.0f / mFrame;
        float xOffset = x2 - x1;
        float yOffset = y2 - y1;
        float x3, y3, a, b, c;


        x3 = (x1 + x2) / 2.0f;
        y3 = Math.min(y1, y2) * 3.0f / 4;
        a = (y1 * (x2 - x3) + y2 * (x3 - x1) + y3 * (x1 - x2)) / (x1 * x1 * (x2 - x3) + x2 * x2 * (x3 - x1) + x3 * x3
                * (x1 - x2));
        b = (y1 - y2) / (x1 - x2) - a * (x1 + x2);
        c = y1 - (x1 * x1) * a - x1 * b;
        for (int i = 0; i <= mFrame; i++) {
            if (i == mFrame + 1) break;
            float offset = i * p;
            xs[i] = x1 + offset * xOffset;
            ys[i] = a * xs[i] * xs[i] + b * xs[i] + c;
        }
    }
}
