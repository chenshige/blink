package com.blink.browser.homepages.navigation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.blink.browser.BackgroundHandler;
import com.blink.browser.Browser;
import com.blink.browser.DatabaseManager;
import com.blink.browser.R;
import com.blink.browser.ToolBar;
import com.blink.browser.UrlUtils;
import com.blink.browser.adapter.RecommendAdapter;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.homepages.ScreenRotateable;
import com.blink.browser.util.DisplayUtil;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.InputMethodUtils;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.util.WebIconUtils;
import com.blink.browser.widget.AnimationListener;
import com.blink.browser.widget.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_BOOKMARK_WEBSITE;
import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_HOT_WEBSITE;
import static com.blink.browser.util.DisplayUtil.dip2px;

/**
 * 首页导航View
 */
public class WebNavigationView extends RelativeLayout implements WebNavigationEditable, OnPageChangeListener, ScreenRotateable {

    private static final int RECOMMEND_COLUMN_PORTRAIT = 5;
    private static final int RECOMMEND_COLUMN_LANDSCAPE_OFF_EDIT = 7;
    private static final int NAVIGATION_SCROLL_DISTANCE = dip2px(Browser.getInstance(), 260f);
    private static final int NAVIGATION_MARGIN_TOP_ON_EDIT = dip2px(Browser.getInstance(), 16f);
    private static final int NAVIGATION_MARGIN_TOP_OFF_EDIT = dip2px(Browser.getInstance(), 276f);
    private static final long ICON_TRANSLATE_TOTAL_TIME = 240L;
    private final int[] iconTranslateDelayTimes = {48, 24, 0, 24, 48, 96, 84, 72, 84, 96, 126, 126, 126, 126, 126};
    private static final int KEY_BOARD_SCROLL_TIME = 500;
    private static final int INDEX_OF_INPUT_PAGE = 2;
    private static final int TRANSFER_ON_EDIT_DELAY = 68;
    private static final int NAV_TRANSFER_OFF_EDIT_DELAY = 60;
    private static final int NAV_TRANSFER_OFF_EDIT_TIME = 250;
    private static final int NAV_SHOW_DISMISS_TIME = 300;
    private static final int VIEW_PAGER_UP_SCROLL_TIME = 300;
    private static final int VIEW_PAGER_UP_SCROLL_DELAY_PORTRAIT = 380;
    private static final int VIEW_PAGER_UP_SCROLL_DELAY_LANDSCAPE = 480;
    private static final int VIEW_PAGER_DOWN_SCROLL_TIME = 250;
    private static final int KEYBOARD_NOTIFY_DELAY = 100;

    private FragmentActivity mActivity;
    private LayoutInflater mInflater;
    private ToolBar mToolBar;
    private TextView mBottomButton;
    private WebNavigationListener mWebNavigationListener;
    private RelativeLayout mRootView;
    private FrameLayout mNavContainerOffEdit;
    private FrameLayout mNavContainerOnEdit;
    private RecyclerView mNavRecycler;
    private int mNavColumnOnLandscapeEdit;
    private RecommendAdapter mRecommendAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private FrameLayout mViewPagerLayout;
    private LinearLayout mViewPagerContainer;
    private ViewPager mViewPager;
    private EditNavigationView mEditNavigationView;
    private Scroller mScroller;
    private ArrayList<Fragment> mPageList = new ArrayList<>();
    private AddFromBookmarkPage mAddFromBookmarkPage;
    private AddFromHistoryPage mAddFromHistoryPage;
    private AddNewNavigationPage mAddNewNavigationPage;
    private DatabaseManager mDbManager = DatabaseManager.getInstance();
    private int currWindowBottom = 0;
    private int mNavColumn;


    private ViewTreeObserver.OnGlobalLayoutListener mGlobalListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (!DisplayUtil.isScreenPortrait(getContext())) {
                return;
            }
            if (!mRecommendAdapter.isEdit()) return;
            Rect r = new Rect();
            getWindowVisibleDisplayFrame(r);
            int visitHeight = getBottom() - r.bottom;
            if (currWindowBottom == visitHeight) return;
            currWindowBottom = visitHeight;
            if (visitHeight <= 0) {
                onKeyBoardHint();
            } else {
                onKeyBoardShow();
            }
        }
    };

    public WebNavigationView(FragmentActivity activity, ToolBar toolBar, TextView bottomButton) {
        super(activity);
        this.mActivity = activity;
        mScroller = new Scroller(activity);
        mInflater = LayoutInflater.from(activity);
        createTouchHelper();
        createNavAdapter(activity);
        createPages();
        init(activity);
        createViewPager(activity);
        addViewPagerToLayout();
        this.mToolBar = toolBar;
        this.mBottomButton = bottomButton;
        mBottomButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackKey();
            }
        });
        getViewTreeObserver().addOnGlobalLayoutListener(mGlobalListener);
    }

    public void setWebNavigationListener(WebNavigationListener webNavigationListener) {
        this.mWebNavigationListener = webNavigationListener;
    }

    private void createTouchHelper() {
        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                    final int swipeFlags = 0;
                    return makeMovementFlags(dragFlags, swipeFlags);
                } else {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                    return makeMovementFlags(dragFlags, swipeFlags);
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return mRecommendAdapter.getItemType(viewHolder.getAdapterPosition()) == mRecommendAdapter.getItemType(target.getAdapterPosition()) &&
                        mRecommendAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (viewHolder == null) {
                    mRecommendAdapter.fireSortChangeIfNeed();
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return !isEdit();
            }

        });
    }

    private void createNavAdapter(final Context context) {
        mRecommendAdapter = new RecommendAdapter(context, mItemTouchHelper);
        mRecommendAdapter.registerListener(new RecommendAdapter.AdapterItemListener() {

            @Override
            public void openUrl(RecommendAdapter.CommonUrlItemViewHolder holder) {
                String url = holder.data.getUrl();
                if (TextUtils.isEmpty(url)) return;
                trackEvent(holder);
                mWebNavigationListener.openUrl(url);
            }

            @Override
            public void editNavigation(RecommendAdapter.CommonUrlItemViewHolder viewHolder) {
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QLMORE_EVENTS, AnalyticsSettings.ID_MODIFY);
                mEditNavigationView.setVisibility(VISIBLE);
                mEditNavigationView.initNavigationItem(viewHolder);
                setViewPagerVisible(false);
                refreshBottomButton();
            }

            @Override
            public void onClickAdd(RecommendAdapter.CommonUrlItemViewHolder viewHolder) {
                transferOnEditStatus(DisplayUtil.isScreenPortrait(getContext()));
            }

            @Override
            public void onDataSetChange() {
                mAddFromBookmarkPage.onDataSetChange();
                mAddFromHistoryPage.onDataSetChange();
                mAddNewNavigationPage.onNavListFullNotify(mRecommendAdapter.isFull());
                mNavRecycler.setLayoutManager(new GridLayoutManager(context, mNavColumn) {
                    @Override
                    public boolean canScrollVertically() {
                        return !DisplayUtil.isScreenPortrait(context) && canNavScroll();
                    }
                });
            }

            @Override
            public void onDeleteMode() {
                refreshBottomButton();
                if (mNavContainerOffEdit != null) {
                    mNavContainerOffEdit.setClipChildren(false);
                }
            }

            @Override
            public void offDeleteMode() {
                refreshBottomButton();
                if (mNavContainerOffEdit != null) {
                    mNavContainerOffEdit.setClipChildren(true);
                }
            }

        });
    }

    private void createPages() {
        mPageList.clear();
        mAddFromBookmarkPage = new AddFromBookmarkPage(this);
        mAddFromHistoryPage = new AddFromHistoryPage(this);
        mAddNewNavigationPage = new AddNewNavigationPage(this);
        mAddNewNavigationPage.setOnFocusChangeListener(new AddNewNavigationPage.OnFocusChangeListener() {
            @Override
            public void onFocusChange() {
                BackgroundHandler.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGlobalListener.onGlobalLayout();
                    }
                }, KEYBOARD_NOTIFY_DELAY);
            }
        });
        mPageList.add(mAddFromBookmarkPage);
        mPageList.add(mAddFromHistoryPage);
        mPageList.add(mAddNewNavigationPage);
    }

    private void init(FragmentActivity activity) {
        if (mViewPagerLayout != null) {
            mViewPagerLayout.removeView(mViewPagerContainer);
        }
        removeAllViews();
        boolean isScreenPortrait = DisplayUtil.isScreenPortrait(activity);
        initRootView(isScreenPortrait);
        initRecommendView(activity, isScreenPortrait);
        initEditNavigationView();
    }

    private void initRootView(boolean isScreenPortrait) {
        if (isScreenPortrait) {
            mRootView = (RelativeLayout) mInflater.inflate(R.layout.main_navigation_portrait, this, false);
            mNavContainerOffEdit = null;
            mNavContainerOnEdit = null;
        } else {
            mRootView = (RelativeLayout) mInflater.inflate(R.layout.main_navigation_landscape, this, false);
            mNavContainerOffEdit = (FrameLayout) mRootView.findViewById(R.id.nav_container_off_edit);
            mNavContainerOnEdit = (FrameLayout) mRootView.findViewById(R.id.nav_container_on_edit);
        }
        addView(mRootView);
    }

    private void initRecommendView(final Context context, boolean isScreenPortrait) {
        boolean isEdit = isEdit();
        if (isScreenPortrait) {
            mNavRecycler = (RecyclerView) findViewById(R.id.recommend_url_recycler);
            mNavColumn = RECOMMEND_COLUMN_PORTRAIT;
            mNavRecycler.setLayoutManager(new GridLayoutManager(context, RECOMMEND_COLUMN_PORTRAIT) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            });
            layoutNavigationPortrait(isEdit);
        } else {
            mNavRecycler = (RecyclerView) mInflater.inflate(R.layout.nav_recycler_view_landscape, mNavContainerOffEdit, false);
            moveNavRecyclerIfNeed(isEdit);
        }
        mNavRecycler.setAdapter(mRecommendAdapter);
        mItemTouchHelper.attachToRecyclerView(mNavRecycler);
        mRecommendAdapter.refreshInputUrlAndRecommend();
    }

    private void moveNavRecyclerIfNeed(boolean edit) {
        if (DisplayUtil.isScreenPortrait(mActivity)) {
            throw new IllegalStateException("cannot call this method when screen portrait");
        }
        if (edit) {
            putNavToContainerOnEdit();
        } else {
            putNavToContainerOffEdit();
        }
        mNavRecycler.setLayoutManager(new GridLayoutManager(mActivity, mNavColumn) {
            @Override
            public boolean canScrollVertically() {
                return !DisplayUtil.isScreenPortrait(mActivity) && canNavScroll();
            }
        });
    }

    private void putNavToContainerOffEdit() {
        FrameLayout parent = (FrameLayout) mNavRecycler.getParent();
        if (parent == mNavContainerOffEdit) {
            mNavContainerOffEdit.setVisibility(VISIBLE);
            mNavContainerOnEdit.setVisibility(GONE);
            return;
        }
        if (parent == mNavContainerOnEdit) {
            parent.removeView(mNavRecycler);
        }
        mNavContainerOffEdit.addView(mNavRecycler);
        mNavColumn = RECOMMEND_COLUMN_LANDSCAPE_OFF_EDIT;
        mNavContainerOffEdit.setVisibility(VISIBLE);
        mNavContainerOnEdit.setVisibility(GONE);
    }

    private void putNavToContainerOnEdit() {
        FrameLayout parent = (FrameLayout) mNavRecycler.getParent();
        if (parent == mNavContainerOnEdit) {
            mNavContainerOnEdit.setVisibility(VISIBLE);
            mNavContainerOffEdit.setVisibility(GONE);
            return;
        }
        if (parent == mNavContainerOffEdit) {
            parent.removeView(mNavRecycler);
        }
        mNavContainerOnEdit.addView(mNavRecycler);
        final Context context = getContext();
        mNavColumn = calcNavColumnOnLandscapeEdit(context);
        setNavContainerWidthOnLandscapeEdit(context, mNavColumn);
        mNavContainerOnEdit.setVisibility(VISIBLE);
        mNavContainerOffEdit.setVisibility(GONE);
    }

    //计算横屏时编辑模式下一行放几个icon
    private int calcNavColumnOnLandscapeEdit(Context context) {
        int screenHeight = DisplayUtil.getScreenWidth(context);
        int viewPagerWidth = DisplayUtil.getDimenPxValue(context, R.dimen.nav_view_pager_landscape_width);
        int navMargin = DisplayUtil.getDimenPxValue(context, R.dimen.nav_margin_left_on_edit);
        int freeSpace = screenHeight - navMargin * 2 - viewPagerWidth;
        int itemSize = DisplayUtil.getDimenPxValue(context, R.dimen.nav_item_size);
        mNavColumnOnLandscapeEdit = freeSpace / itemSize;
        if (mNavColumnOnLandscapeEdit < 1) {
            mNavColumnOnLandscapeEdit = 1;//列数不可小于1
        }
        return mNavColumnOnLandscapeEdit;
    }

    private boolean canNavScroll() {
        int visibleCount = mRecommendAdapter.getVisibleCount();
        int rows = visibleCount / mNavColumn;
        if (visibleCount % mNavColumn != 0) {
            rows++;
        }
        int itemSize = DisplayUtil.getDimenPxValue(mActivity, R.dimen.nav_item_size);
        return itemSize * rows > mNavRecycler.getHeight();
    }

    private void setNavContainerWidthOnLandscapeEdit(Context context, int navColumn) {
        LayoutParams navContainerParams = (LayoutParams) mNavContainerOnEdit.getLayoutParams();
        navContainerParams.width = navColumn * DisplayUtil.getDimenPxValue(context, R.dimen.nav_item_size);
        mNavContainerOnEdit.setLayoutParams(navContainerParams);
    }

    private void createViewPager(FragmentActivity activity) {
        mViewPagerLayout = (FrameLayout) findViewById(R.id.view_pager_layout);
        mViewPagerContainer = (LinearLayout) mInflater.inflate(R.layout.main_nav_edit_view_pager, mViewPagerLayout, false);
        mViewPager = (ViewPager) mViewPagerContainer.findViewById(R.id.viewpager_combo);
        BasePagerAdapter adapter = new BasePagerAdapter(activity.getSupportFragmentManager(), mPageList);
        mViewPager.setAdapter(adapter);
        PagerSlidingTabStrip mIndicator = (PagerSlidingTabStrip) mViewPagerContainer.findViewById(R.id.tab_page_indicator_combo);
        mIndicator.setViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(this);
    }

    private void addViewPagerToLayout() {
        if (mViewPagerContainer.getParent() != null) {
            throw new IllegalStateException("this method must called once after init root view");
        }
        mViewPagerLayout = (FrameLayout) findViewById(R.id.view_pager_layout);
        mViewPagerLayout.addView(mViewPagerContainer);
        setViewPagerVisible(isEdit());
    }

    private void initEditNavigationView() {
        mEditNavigationView = (EditNavigationView) findViewById(R.id.edit_navigation);
        mEditNavigationView.setOnNavigationEditListener(new EditNavigationView.OnNavigationEditListener() {
            @Override
            public void onEditCompleted(RecommendAdapter.CommonUrlItemViewHolder targetItemViewHolder, String title, String url) {
                if (modifyNavigation(targetItemViewHolder.position, targetItemViewHolder.data, title, url)) {
                    mEditNavigationView.setVisibility(GONE);
                    setViewPagerVisible(true);
                    onKeyBoardHint();
                    refreshBottomButton();
                }
            }

            @Override
            public void onCancel() {
                mEditNavigationView.setVisibility(GONE);
                setViewPagerVisible(true);
                onKeyBoardHint();
                refreshBottomButton();
            }
        });
    }

    private void refreshBottomButton() {
        if (mRecommendAdapter.isDeleteMode() && !mRecommendAdapter.isEdit()) {
            mBottomButton.setVisibility(VISIBLE);
            mBottomButton.setText(R.string.done);
        } else if (mRecommendAdapter.isEdit()) {
            if (mEditNavigationView.getVisibility() == VISIBLE) {
                mBottomButton.setVisibility(GONE);
            } else if (mRecommendAdapter.isDeleteMode()) {
                mBottomButton.setVisibility(VISIBLE);
                mBottomButton.setText(R.string.done);
            } else {
                mBottomButton.setVisibility(VISIBLE);
                mBottomButton.setText(R.string.done);
            }
        } else {
            mBottomButton.setVisibility(GONE);
        }
        if (!mToolBar.isShowToolBar()) {
            mToolBar.updateToolBarVisibility(true, true);
        } else {
            mToolBar.updateToolBarVisibility();
        }
    }

    private void setViewPagerVisible(boolean show) {
        if (show) {
            mViewPagerLayout.setVisibility(VISIBLE);
        } else {
            mViewPagerLayout.setVisibility(INVISIBLE);
        }
    }

    private int distanceScrollWhenKeyBoardShow() {
        return mViewPagerLayout.getTop() - dip2px(getContext(), 10);
    }

    private void onKeyBoardShow() {
        if (mEditNavigationView.getVisibility() == VISIBLE) {
            mEditNavigationView.onKeyBoardShow();
        } else {
            mRootView.scrollTo(0, distanceScrollWhenKeyBoardShow());
        }
        mAddNewNavigationPage.onKeyBoardShow();
    }

    private void onKeyBoardHint() {
        mAddNewNavigationPage.onKeyBoardHint();
        if (mEditNavigationView.getVisibility() == VISIBLE) {
            mEditNavigationView.onKeyBoardHint();
        }
        if (DisplayUtil.isScreenPortrait(getContext())) {
            mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), KEY_BOARD_SCROLL_TIME);
        }
    }

    private void transferOnEditStatus(boolean isPortrait) {
        if (mWebNavigationListener != null) {
            mWebNavigationListener.onTransferOnEditStatus();
        }
        mRecommendAdapter.setIsEdit(true);
        refreshBottomButton();
        if (isPortrait) {
            BackgroundHandler.getMainHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    navIconsScrollUpPortrait();
                }
            }, TRANSFER_ON_EDIT_DELAY);
        } else {
            navIconsScrollUpLandscape();
        }
        viewPagerScrollUp(isPortrait);
    }

    private void navIconsScrollUpPortrait() {
        int itemCount = mRecommendAdapter.getItemCount();
        final ArrayList<View> targetViews = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            RecommendAdapter.CommonUrlItemViewHolder holder = mRecommendAdapter.getViewHolderByPosition(i);
            if (holder == null) {
                break;
            }
            TranslateAnimation animation = startOneNavIconScrollAnim(holder.mItemView, iconTranslateDelayTimes[i]);
            targetViews.add(holder.mItemView);
            if (i == itemCount - 1) {
                animation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        for (View targetView : targetViews) {
                            targetView.clearAnimation();
                        }
                        layoutNavigationPortrait(true);
                    }
                });
            }
        }
    }

    private void navIconsScrollUpLandscape() {
        AlphaAnimation dismissAnim = new AlphaAnimation(1f, 0f);
        dismissAnim.setDuration(NAV_SHOW_DISMISS_TIME);
        final AlphaAnimation showAnim = new AlphaAnimation(0f, 1f);
        showAnim.setDuration(NAV_SHOW_DISMISS_TIME);
        dismissAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                moveNavRecyclerIfNeed(true);
                mNavContainerOnEdit.startAnimation(showAnim);
            }
        });
        mNavContainerOffEdit.startAnimation(dismissAnim);
    }

    public TranslateAnimation startOneNavIconScrollAnim(final View itemView, long delayTime) {
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -NAVIGATION_SCROLL_DISTANCE);
        animation.setDuration(ICON_TRANSLATE_TOTAL_TIME);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setFillAfter(true);
        animation.setStartOffset(delayTime);
        itemView.startAnimation(animation);
        return animation;
    }

    private void viewPagerScrollUp(boolean isPortrait) {
        Animation viewPagerAnim = AnimationUtils.loadAnimation(getContext(), R.anim.nav_view_pager_show);
        viewPagerAnim.setDuration(VIEW_PAGER_UP_SCROLL_TIME);
        if (isPortrait) {
            viewPagerAnim.setStartOffset(VIEW_PAGER_UP_SCROLL_DELAY_PORTRAIT);
        } else {
            viewPagerAnim.setStartOffset(VIEW_PAGER_UP_SCROLL_DELAY_LANDSCAPE);
        }
        viewPagerAnim.setInterpolator(new OvershootInterpolator(1.2f));
        viewPagerAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                setViewPagerVisible(true);
            }
        });
        mViewPagerLayout.startAnimation(viewPagerAnim);
    }


    private void transferOffEditStatus() {
        //viewPager 下落
        viewPagerScrollDown();
        mAddNewNavigationPage.finishEdit();
        //导航图标下落 同时背景变白
        mBottomButton.setVisibility(GONE);
        if (DisplayUtil.isScreenPortrait(getContext())) {
            mRecommendAdapter.setIsEdit(false);
            navIconsScrollDownPortrait();
        } else {
            navIconsScrollDownLandscape();
        }
        //图标下落完成同时 logo 搜索框渐显并下落
        mWebNavigationListener.onTransferOffEditStatus();
    }

    private void navIconsScrollDownPortrait() {
        TranslateAnimation navIconsAnim = new TranslateAnimation(0f, 0f, 0f, NAVIGATION_SCROLL_DISTANCE);
        navIconsAnim.setDuration(NAV_TRANSFER_OFF_EDIT_TIME);
        navIconsAnim.setStartOffset(NAV_TRANSFER_OFF_EDIT_DELAY);
        navIconsAnim.setInterpolator(new AccelerateInterpolator());
        navIconsAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(final Animation animation) {
                mNavRecycler.clearAnimation();
                layoutNavigationPortrait(false);
                refreshBottomButton();
            }
        });
        mNavRecycler.startAnimation(navIconsAnim);
    }

    private void navIconsScrollDownLandscape() {
        AlphaAnimation dismissAnim = new AlphaAnimation(1f, 0f);
        dismissAnim.setDuration(NAV_SHOW_DISMISS_TIME);
        final AlphaAnimation showAnim = new AlphaAnimation(0f, 1f);
        showAnim.setDuration(NAV_SHOW_DISMISS_TIME);
        dismissAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mRecommendAdapter.setIsEdit(false);
                refreshBottomButton();
                moveNavRecyclerIfNeed(false);
                mNavContainerOffEdit.startAnimation(showAnim);
            }
        });
        mNavContainerOnEdit.startAnimation(dismissAnim);
    }

    private void viewPagerScrollDown() {
        Animation viewPagerAnim = AnimationUtils.loadAnimation(getContext(), R.anim.nav_view_pager_dismiss);
        viewPagerAnim.setDuration(VIEW_PAGER_DOWN_SCROLL_TIME);
        viewPagerAnim.setInterpolator(new AccelerateInterpolator());
        viewPagerAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                setViewPagerVisible(false);
                mViewPager.setCurrentItem(0, false);
            }
        });
        mViewPagerLayout.startAnimation(viewPagerAnim);
    }

    private void layoutNavigationPortrait(boolean isEdit) {
        MarginLayoutParams layoutParams = (MarginLayoutParams) mNavRecycler.getLayoutParams();
        if (isEdit) {
            layoutParams.topMargin = NAVIGATION_MARGIN_TOP_ON_EDIT;
        } else {
            layoutParams.topMargin = NAVIGATION_MARGIN_TOP_OFF_EDIT;
        }
        mNavRecycler.setLayoutParams(layoutParams);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();
            scroll(currY);
            postInvalidate();
        }
        super.computeScroll();
    }

    private void scroll(int currY) {
        mRootView.scrollTo(0, currY);
    }

    public void onResume() {
        if (mRecommendAdapter.isDeleteMode()) {
            mRecommendAdapter.setDeleteMode(false);
        }
    }

    public boolean isWebEditShowing() {
        return isEdit();
    }

    public boolean onBackKey() {
        if (mEditNavigationView.getVisibility() == VISIBLE) {
            mEditNavigationView.setVisibility(GONE);
            setViewPagerVisible(true);
            refreshBottomButton();
            return true;
        }
        if (mRecommendAdapter.isDeleteMode()) {
            mRecommendAdapter.setDeleteMode(false);
            refreshBottomButton();
            return true;
        }
        if (mRecommendAdapter.isEdit()) {
            transferOffEditStatus();
            return true;
        }
        refreshBottomButton();
        return false;
    }

    private void trackEvent(RecommendAdapter.CommonUrlItemViewHolder holder) {
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QUICKLINK_EVENTS, Integer
                .toString(holder.position + 1), holder.data.getDisplayName());

        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QUICKLINK_EVENTS, AnalyticsSettings.ID_QUICKLINK);
    }

    @Override
    public int doUrlContained(String url) {
        if (url == null) return -1;
        List<RecommendUrlEntity> data = mRecommendAdapter.getData();
        for (int i = 0; i < data.size(); i++) {
            RecommendUrlEntity entity = data.get(i);
            if (TextUtils.equals(entity.getUrl(), url)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isEdit() {
        return mRecommendAdapter != null && mRecommendAdapter.isEdit();
    }

    private boolean checkNavigationInput(String title, String url) {
        if (TextUtils.isEmpty(title)) {
            ToastUtil.showShortToast(mActivity, R.string.right_recommend_add_link_not_title);
            return true;
        }

        if (TextUtils.isEmpty(url)) {
            ToastUtil.showShortToast(mActivity, R.string.right_recommend_add_link_not_url);
            return true;
        }
        if (!UrlUtils.checkUrl(url)) {
            ToastUtil.showShortToast(mActivity, R.string.right_recommend_add_link_url_format);
            return true;
        }
        return false;
    }

    @Override
    public boolean addNewNavigation(String title, String url, boolean needCheck) {
        if (needCheck && checkNavigationInput(title, url)) return false;
        if (mRecommendAdapter.isFull()) {
            ToastUtil.showShortToast(getContext(), R.string.add_up_to_15);
            return false;
        }
        final RecommendUrlEntity info = new RecommendUrlEntity();
        info.setDisplayName(title);
        info.setUrl(url);
        List<RecommendUrlEntity> list = mDbManager.findByArgs(RecommendUrlEntity.class,
                RecommendUrlEntity.Column.URL + " = ? AND " +
                        RecommendUrlEntity.Column.DISPLAY_NAME + " = ? ", new String[]{url, title});
        if (list == null || list.size() == 0) {
            setNavIcon(info, url);
            DatabaseManager.getInstance().insert(info);
            return true;
        } else {
            ToastUtil.showShortToast(mActivity, R.string.right_recommend_add_link_repeat);
            return false;
        }
    }

    private void setNavIcon(final RecommendUrlEntity info, String url) {
        Bitmap iconBmp = WebIconUtils.getWebIconFromLocalDb(mActivity, url);
        byte[] webIconLocal = ImageUtils.bitmapToBytes(iconBmp);
        if (webIconLocal != null && webIconLocal.length != 0) {
            info.setWeight(WEIGHT_BOOKMARK_WEBSITE);
        }
        info.setImageIcon(webIconLocal);
    }

    @Override
    public void onFinishAddNewNavigation() {
        mViewPager.setCurrentItem(0, false);
    }

    @Override
    public boolean modifyNavigation(int position, RecommendUrlEntity entity, String newTitle, String newUrl) {
        if (checkNavigationInput(newTitle, newUrl)) return false;
        entity.setDisplayName(newTitle);
        entity.setUrl(newUrl);
        setNavIcon(entity, newUrl);
        return mDbManager.updateBy(entity) >= 1;
    }

    @Override
    public boolean deleteNavigation(int position) {
        RecommendUrlEntity entity = mRecommendAdapter.getData(position);
        return mDbManager.deleteById(RecommendUrlEntity.class, entity.getId()) >= 1;
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position != INDEX_OF_INPUT_PAGE) {
            InputMethodUtils.hideKeyboard(mActivity);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onScreenRotate(boolean isPortrait) {
        init(mActivity);
        addViewPagerToLayout();
        mAddNewNavigationPage.onScreenRotate(isPortrait);
    }

    public void onIncognito(boolean incognito) {

        mRecommendAdapter.setIncognito(incognito);

    }

    public interface WebNavigationListener {

        void openUrl(String url);

        void onTransferOnEditStatus();

        void onTransferOffEditStatus();

    }

    static class BasePagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        private int[] mIconArray;
        private int[] mIconSelectArray;
        private ArrayList<Fragment> mPageList;

        public BasePagerAdapter(FragmentManager fm, ArrayList<Fragment> pageList) {
            super(fm);
            mIconArray = new int[]{R.drawable.ic_browser_tab_bookmark, R.drawable.ic_browser_tab_history, R.drawable.ic_browser_tab_user_defined};
            mIconSelectArray = new int[]{R.drawable.ic_browser_tab_bookmark_selected, R.drawable.ic_browser_tab_history_selected,
                    R.drawable.ic_browser_tab_user_defined_selected};
            mPageList = pageList;
        }

        @Override
        public Fragment getItem(int position) {
            return mPageList.get(position);
        }

        @Override
        public int getCount() {
            return mPageList.size();
        }

        @Override
        public int getPageIconResId(int position) {
            return mIconArray[position];
        }

        @Override
        public int getPageSelectIconResId(int position) {
            return mIconSelectArray[position];
        }
    }

}
