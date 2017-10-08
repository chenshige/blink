package com.blink.browser.view.switchsearchengine;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.blink.browser.BaseUi;
import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.handler.BrowserHandler;
import com.blink.browser.search.SearchEngineInfo;
import com.blink.browser.search.SearchEnginePreference;
import com.blink.browser.search.SearchEngines;
import com.blink.browser.util.DisplayUtil;

import java.util.ArrayList;
import java.util.List;

public class SelectSearchEngine extends RelativeLayout implements SearchEngineItemAnimator.OnAnimationEnd, View
        .OnClickListener {

    private static final int ENTER_TIME_BLACK_BG = 300;
    private static final int OUT_TIME_BLACK_BG = 300;
    private static final int ENTER_TIME_SUSPENDED = 200;
    private static final int OUT_TIME_SUSPENDED = 200;
    private static final int ENTER_CANCEL_BTN_TIME = 300;
    private static final int ROW_ITEM_SIZE = 3;

    private View mClickView;
    private ImageView mBgView;
    private SearchEngineSuspanededView mSuspendedImageView;
    private ImageView mCancel;
    private RecyclerView mRecyclerView;
    private SearchEngineAdapter mAdapter;

    private List<SearchEngineInfo> mSearchEngineList = new ArrayList<>();
    private boolean mIsItemShow = false;
    private int[] mClickViewLocation;
    private int mClickHeight = 0;
    private int mClickWidth = 0;
    private boolean mIsCancel = false;
    private BaseUi mUi;
    private int mActionBarH = 0;
    private boolean mIsRunAnima = false;
    private int mItemPaddingHorizontal;
    private int mItemPaddingVertical;
    private boolean mIsChangeSearchEngine = false;

    public SelectSearchEngine(Context context, BaseUi ui, View view) {
        super(context);
        mClickView = view;
        mUi = ui;
        initView();
        initData();
    }

    public void initView() {
        View.inflate(getContext(), R.layout.select_searchengine_view, this);
        mBgView = (ImageView) findViewById(R.id.anima_image);
        mCancel = (ImageView) findViewById(R.id.cancel_searchengine);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mCancel.setOnClickListener(this);
        if (BrowserSettings.getInstance().getShowStatusBar()) {
            TypedValue heightValue = new TypedValue();
            getContext().getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, heightValue, true);
            mActionBarH = DisplayUtil.getStatusBarHeight(getContext());

        }
    }

    public void initData() {
        List<SearchEngineInfo> list = SearchEngines.getInstance(getContext()).getSearchEngineInfos();
        if (list == null || list.size() == 0) {
            mUi.dimissSearchEngine();
            return;
        }
        mSearchEngineList.clear();
        mSearchEngineList.addAll(list);
        SearchEngineInfo defaultEngineInfo = SearchEngines.getInstance(getContext()).
                getSearchEngineInfo(getContext(), BrowserSettings.getInstance().getSearchEngineName());
        if (defaultEngineInfo == null || mSearchEngineList == null || mSearchEngineList.size() == 0) {
            mUi.dimissSearchEngine();
            return;
        }

        final int screenWidth = DisplayUtil.getScreenWidth(getContext());
        final int screenHeight = DisplayUtil.getScreenHeight(getContext());
        int itemImageH = getResources().getDimensionPixelOffset(R.dimen.select_searchengine_item_image_wh) +
                getResources().getDimensionPixelOffset(R.dimen.select_searchengine_item_image_margin) * 2;

        final boolean isLandscape = screenWidth > screenHeight;
        //横屏下recyclerView据两边的边距
        int recyclerViewMarginH = screenWidth / 8;
        int recyclerViewWidth = isLandscape ? screenWidth - recyclerViewMarginH * 2 : screenWidth;
        mItemPaddingHorizontal = (recyclerViewWidth / ROW_ITEM_SIZE - itemImageH) / 2;
        mItemPaddingVertical = isLandscape ? mItemPaddingHorizontal / 2 : mItemPaddingHorizontal;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mRecyclerView.getLayoutParams();
        int height = (recyclerViewWidth / ROW_ITEM_SIZE - mItemPaddingHorizontal * 2
                + mItemPaddingVertical * 2) * (mSearchEngineList.size() / ROW_ITEM_SIZE + (mSearchEngineList.size()
                % ROW_ITEM_SIZE > 0 ? 1 : 0));
        if (height > screenHeight * 4 / 5) {
            height = screenHeight * 4 / 5;
        }
        params.height = height;
        params.topMargin = isLandscape ? (screenHeight - params.height) / 2 : (screenHeight - params.height) * 2 / 5;
        params.leftMargin = isLandscape ? recyclerViewMarginH : 0;
        params.rightMargin = isLandscape ? recyclerViewMarginH : 0;

        mRecyclerView.setLayoutParams(params);

        for (int i = 0; i < mSearchEngineList.size(); i++) {
            SearchEngineInfo info = mSearchEngineList.get(i);
            if (defaultEngineInfo.getName() != null
                    && info.getName() != null
                    && defaultEngineInfo.getName().equals(info.getName())) {
                mSearchEngineList.remove(info);
            }
        }

        mAdapter = new SearchEngineAdapter(defaultEngineInfo);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setVisibility(View.VISIBLE);
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), ROW_ITEM_SIZE);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mRecyclerView.setOnClickListener(this);
        setOnClickListener(this);
        SearchEngineItemAnimator animator = new SearchEngineItemAnimator();
        mRecyclerView.setItemAnimator(animator);
        animator.setOnAnimationEnd(this);
        setImageViewLocation();
        mIsRunAnima = true;
        //view加载完成时回调
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // TODO Auto-generated method stub
                mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startAnimation(isLandscape ? screenWidth : screenHeight);
            }
        });
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initData();
    }

    /**
     * 设置悬浮框，黑色背景起点的位置的位置
     */
    public void setImageViewLocation() {
        if (mClickView == null) {
            return;
        }
        mClickViewLocation = new int[2];
        mClickView.getLocationOnScreen(mClickViewLocation);
        mClickViewLocation = new int[]{mClickViewLocation[0], mClickViewLocation[1] - mActionBarH};

        mClickHeight = mClickView.getHeight();
        mClickWidth = mClickView.getWidth();

        FrameLayout.LayoutParams animaParams = (FrameLayout.LayoutParams) mBgView.getLayoutParams();
        animaParams.leftMargin = mClickViewLocation[0] + mClickWidth / 2;
        animaParams.topMargin = mClickViewLocation[1] + mClickHeight / 2;
        mBgView.setLayoutParams(animaParams);

        float scale = (float) mClickWidth / getResources().getDimensionPixelOffset(R.dimen
                .select_searchengine_item_image_wh);
        float margin = getResources().getDimensionPixelOffset(R.dimen.select_searchengine_item_image_margin) * scale;

        mClickHeight += margin * 2;
        mClickWidth += margin * 2;

        mSuspendedImageView = new SearchEngineSuspanededView(getContext(), mClickView, false);
        mSuspendedImageView.setImage(BrowserSettings.getInstance().getSearchEngineName());
        mSuspendedImageView.setImageViewMargins((int) margin);
        mSuspendedImageView.setBackgroundResource(R.drawable.searchengine_rounded_border_bg);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mClickWidth, mClickHeight);
        layoutParams.topMargin = mClickViewLocation[1] - (int) margin;
        layoutParams.leftMargin = mClickViewLocation[0] - (int) margin;
        addView(mSuspendedImageView, layoutParams);

    }

    /**
     * 悬浮框进入动画
     */
    private void startSuspendedAnimation() {
        if (mSuspendedImageView == null) {
            return;
        }
        ImageView moveImageView = mSuspendedImageView.getImageView();
        if (moveImageView == null) {
            return;
        }

        if (mRecyclerView.getChildCount() == 0) {
            return;
        }

        View v = mRecyclerView.getChildAt(0);
        final SearchEngineAdapter.ViewHolder viewHolder = (SearchEngineAdapter.ViewHolder) mRecyclerView
                .getChildViewHolder(v);
        LinearLayout itemLayout = viewHolder.getItemLayout();
        int[] location = new int[2];
        itemLayout.getLocationOnScreen(location);

        float scale = (float) mClickView.getWidth() / getResources().getDimensionPixelOffset(R.dimen
                .select_searchengine_item_image_wh);
        int margin = (int) (getResources().getDimensionPixelOffset(R.dimen.select_searchengine_item_image_margin) *
                scale);

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator x = ObjectAnimator.ofFloat(mSuspendedImageView, "x", mClickViewLocation[0] - margin,
                location[0] + (itemLayout.getWidth() - mSuspendedImageView.getWidth()) / 2);
        ObjectAnimator y = ObjectAnimator.ofFloat(mSuspendedImageView, "y", mClickViewLocation[1] - margin,
                location[1] + (itemLayout.getHeight() - mSuspendedImageView.getHeight()) / 2 - mActionBarH);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mSuspendedImageView, "scaleX", 1, itemLayout.getWidth() /
                (float) mSuspendedImageView.getWidth());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mSuspendedImageView, "scaleY", 1, itemLayout.getHeight() /
                (float) mSuspendedImageView.getHeight());

        animatorSet.playTogether(x, y, scaleX, scaleY);
        animatorSet.setDuration(ENTER_TIME_SUSPENDED);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();

        BrowserHandler.getInstance().handlerPostDelayed(new Runnable() {
            @Override
            public void run() {
                viewHolder.itemView.setVisibility(View.VISIBLE);
                mSuspendedImageView.setVisibility(View.GONE);
                mIsItemShow = true;
            }
        }, 200);

        mAdapter.addData(mSearchEngineList);
        mAdapter.notifyItemRangeInserted(mAdapter.getItemCount(), mSearchEngineList.size());
        showCancelView();
    }

    /**
     * 背景扩散动画
     *
     * @param animation_size
     */
    private void startAnimation(float animation_size) {

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mBgView, "scaleX", 1, animation_size);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mBgView, "scaleY", 1, animation_size);
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(ENTER_TIME_BLACK_BG);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                startSuspendedAnimation();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animatorSet.start();
    }

    /**
     * 底部取消按钮进入动画
     */
    private void showCancelView() {
        int[] location = new int[2];
        mCancel.getLocationOnScreen(location);
        mCancel.setVisibility(View.VISIBLE);
        ObjectAnimator y = ObjectAnimator.ofFloat(mCancel, "y", DisplayUtil.getScreenHeight(getContext()),
                location[1] - mActionBarH);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(y);
        animatorSet.setDuration(ENTER_CANCEL_BTN_TIME);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    /**
     * 底部取消按钮退出
     */
    private void dismissCancelView() {
        mIsRunAnima = true;
        mCancel.setVisibility(View.GONE);
    }

    public void onBackKey() {
        if (mIsRunAnima) {
            return;
        }
        mIsCancel = true;
        mAdapter.removeAll();
        dismissCancelView();
    }

    /**
     * 黑色北京淡出动画
     */
    private void dismiss() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mBgView, "alpha", 1f, 0.0f);
        animatorSet.playTogether(alpha);
        animatorSet.setDuration(OUT_TIME_BLACK_BG);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mUi.dimissSearchEngine();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animatorSet.start();
    }

    public void onItemClick(View item) {
        SearchEngineAdapter.ViewHolder viewHolder = (SearchEngineAdapter.ViewHolder) item.getTag();
        if (viewHolder == null) {
            return;
        }
        SearchEngineInfo info = viewHolder.getInfo();
        if (info == null || TextUtils.isEmpty(info.getName())) {
            onBackKey();
            return;
        }


        int[] location = new int[2];
        item.getLocationOnScreen(location);
        if (mSuspendedImageView != null) {
            removeView(mSuspendedImageView);
            mSuspendedImageView = null;
        }

        if (!BrowserSettings.getInstance().getSearchEngineName().equals(info.getName())) {
            mIsChangeSearchEngine = true;
            BrowserSettings.getInstance().setSearchEngineName(info.getName());
        }

        mSuspendedImageView = new SearchEngineSuspanededView(getContext(), item, true);
        mSuspendedImageView.setImage(info.getName());
        mSuspendedImageView.setBackgroundResource(R.drawable.searchengine_item_bg);
        LayoutParams layoutParams = new LayoutParams(item.getWidth(), item.getHeight());
        layoutParams.topMargin = location[1] - mActionBarH;
        layoutParams.leftMargin = location[0];
        addView(mSuspendedImageView, layoutParams);

        mAdapter.removeAll();
        dismissCancelView();

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.SEARCHENGINESWITCH_EVENTS, AnalyticsSettings
                .ID_STATUS, info.getName());
    }

    /**
     * item 添加动画结束通知
     */
    @Override
    public void onAddItemAnimationEnd() {
        mIsRunAnima = false;
    }

    /**
     * item 移除动画结束通知，以及选中退出动画
     */
    @Override
    public void onRemoveItemAnimationEnd() {
        if (mIsCancel) {
            dismiss();
        } else {
            int[] location = new int[2];
            mSuspendedImageView.getLocationOnScreen(location);

            int mClickHeight = mClickView.getHeight();
            int mClickWidth = mClickView.getWidth();

            float scale = (float) mClickWidth / getResources().getDimensionPixelOffset(R.dimen
                    .select_searchengine_item_image_wh);
            int margin = (int) (getResources().getDimensionPixelOffset(R.dimen.select_searchengine_item_image_margin)
                    * scale);

            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator x = ObjectAnimator.ofFloat(mSuspendedImageView, "x", location[0],
                    mClickViewLocation[0] - (mSuspendedImageView.getWidth() - mClickWidth) / 2);
            ObjectAnimator y = ObjectAnimator.ofFloat(mSuspendedImageView, "y", location[1] - mActionBarH,
                    mClickViewLocation[1] - (mSuspendedImageView.getHeight() - mClickHeight) / 2);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mSuspendedImageView, "scaleX", 1,
                    (mClickWidth + margin * 2) / (float) mSuspendedImageView.getWidth());
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mSuspendedImageView, "scaleY", 1,
                    (mClickHeight + margin * 2) / (float) mSuspendedImageView.getHeight());
            ObjectAnimator alpha = ObjectAnimator.ofFloat(mBgView, "alpha", 1f, 0.0f);

            x.setInterpolator(new DecelerateInterpolator());
            y.setInterpolator(new AccelerateInterpolator());
            scaleX.setInterpolator(new DecelerateInterpolator());
            scaleY.setInterpolator(new DecelerateInterpolator());
            alpha.setInterpolator(new DecelerateInterpolator());
            animatorSet.playTogether(x, y, scaleX, scaleY, alpha);

            animatorSet.setDuration(OUT_TIME_SUSPENDED);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (mIsChangeSearchEngine) {
                        mUi.changeSearchEngine();
                    }
                    mUi.dimissSearchEngine();
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            animatorSet.start();
        }
    }


    @Override
    public void onClick(View view) {
        onBackKey();
    }


    public class SearchEngineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<SearchEngineInfo> dataList = new ArrayList<>();

        public SearchEngineAdapter(SearchEngineInfo info) {
            if (info != null) {
                this.dataList.clear();
                this.dataList.add(info);
            }
        }

        public void addData(List<SearchEngineInfo> list) {
            if (dataList != null && dataList.size() > 0) {
                this.dataList.addAll(list);
                int remainder = dataList.size() % ROW_ITEM_SIZE;
                if (remainder > 0) {
                    for (int i = 0; i < ROW_ITEM_SIZE - remainder; i++) {
                        SearchEngineInfo info = new SearchEngineInfo();
                        dataList.add(info);
                    }
                }
            }
        }

        public void removeAll() {
            int l = dataList.size();
            dataList.clear();
            notifyItemRangeRemoved(0, l);
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                    .select_search_engine_item, null));

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            SearchEngineInfo info = dataList.get(position);
            ViewHolder holder = (ViewHolder) viewHolder;
            LinearLayout itemLayout = ((ViewHolder) viewHolder).getItemLayout();
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
            params.setMargins(mItemPaddingHorizontal, mItemPaddingVertical, mItemPaddingHorizontal, mItemPaddingVertical);
            itemLayout.setLayoutParams(params);
            itemLayout.setTag(viewHolder);
            itemLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsRunAnima) {
                        return;
                    }
                    onItemClick(view);
                }
            });

            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackKey();
                }
            });

            if (info == null) {
                return;
            }
            holder.setInfo(info);
            ImageView imageView = holder.getImageView();
            String name = info.getName();
            if (TextUtils.isEmpty(name)) {
                viewHolder.itemView.setVisibility(View.INVISIBLE);
                itemLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
                imageView.setVisibility(View.INVISIBLE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                SearchEnginePreference.setSearchEngineIcon(getContext(), imageView, info.getName());
            }
            viewHolder.itemView.setVisibility(mIsItemShow ? View.VISIBLE : View.INVISIBLE);

        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView mImageView;
            private LinearLayout itemLayout;
            private SearchEngineInfo info;

            public ViewHolder(View itemView) {
                super(itemView);
                itemLayout = (LinearLayout) itemView.findViewById(R.id.item_layout);
                mImageView = (ImageView) itemView.findViewById(R.id.item_icon);
            }

            public ImageView getImageView() {
                return mImageView;
            }

            public LinearLayout getItemLayout() {
                return itemLayout;
            }

            public void setInfo(SearchEngineInfo info) {
                this.info = info;
            }

            public SearchEngineInfo getInfo() {
                return info;
            }

        }
    }
}
