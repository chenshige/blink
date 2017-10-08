// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.blink.browser.BackgroundHandler;
import com.blink.browser.DatabaseManager;
import com.blink.browser.R;
import com.blink.browser.analytics.AnalyticsSettings;
import com.blink.browser.analytics.BrowserAnalytics;
import com.blink.browser.bean.RecommendUrlEntity;
import com.blink.browser.homepages.clone.CloneableRelativeLayout;
import com.blink.browser.util.ColorUtils;
import com.blink.browser.util.ImageUtils;
import com.blink.browser.util.RecommendUrlUtil;
import com.blink.browser.view.RoundImageView;
import com.blink.browser.widget.AnimationListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.OnLongClickListener;
import static android.view.View.VISIBLE;
import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_BOOKMARK_WEBSITE;
import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_HOT_WEBSITE;
import static com.blink.browser.bean.RecommendUrlEntity.WEIGHT_RECOMMEND_WEBSITE;

public class RecommendAdapter extends RecyclerView.Adapter<RecommendAdapter.CommonUrlItemViewHolder>
        implements DatabaseManager.DataChangeListener<RecommendUrlEntity> {

    private static final String LOGTAG = "BrowserRecommendAdapter";
    private static final int ANIMATION_TIME = 300;
    private static final int TITLE_ANIMATION_DELAY = 100;
    private static final int RECOVERY_DELAY = 300;
    private static final float START_VALUE = 0f;
    private static final float MIDDLE_VALUE = 0.5f;
    private static final float END_VALUE = 1f;

    private static final int TYPE_NAV = 1;
    private static final int TYPE_ADD = 2;
    private static final int TYPE_SPACE_GRAY = 3;
    private static final int TYPE_SPACE_HIDE = 4;

    private Handler mHandler = new Handler();
    private Context mContext;
    private ItemTouchHelper mItemTouchHelper;
    private List<RecommendUrlEntity> mCommonUrlBeans;
    private SparseArray<CommonUrlItemViewHolder> mViewHolderSet = new SparseArray<>();
    private AdapterItemListener mAdapterItemListener;
    private boolean mIsEdit = false;
    private boolean mIsDeleteMode = false;
    private int mMaxCount;
    private boolean mIsMove;
    private boolean mIncognito;


    public RecommendAdapter(Context context, ItemTouchHelper itemTouchHelper) {
        mCommonUrlBeans = new ArrayList<>();
        mContext = context;
        mMaxCount = context.getResources().getInteger(R.integer.recommend_item_max_count);
        mItemTouchHelper = itemTouchHelper;
        RecommendUrlUtil.addContentObserver(this);
    }


    public void setIsEdit(boolean mIsEdit) {
        this.mIsEdit = mIsEdit;
        this.notifyDataSetChanged();
    }

    public boolean isEdit() {
        return mIsEdit;
    }

    public boolean isIncognito() {
        return mIncognito;
    }

    public void setIncognito(boolean incognito) {
        this.mIncognito = incognito;
        this.notifyDataSetChanged();
    }

    public void registerListener(AdapterItemListener adapterItemListener) {
        this.mAdapterItemListener = adapterItemListener;
    }

    public void setDeleteMode(boolean deleteMode) {
        if (deleteMode == mIsDeleteMode) return;
        this.mIsDeleteMode = deleteMode;
        notifyDataSetChanged();
        if (mAdapterItemListener == null) return;
        if (mIsDeleteMode) {
            mAdapterItemListener.onDeleteMode();
        } else {
            mAdapterItemListener.offDeleteMode();
        }
    }

    public boolean isDeleteMode() {
        return mIsDeleteMode;
    }

    public boolean isFull() {
        return mCommonUrlBeans.size() == mMaxCount;
    }

    public List<RecommendUrlEntity> getData() {
        return mCommonUrlBeans;
    }

    public RecommendUrlEntity getData(int position) {
        return mCommonUrlBeans.get(position);
    }

    private void replaceData(List<RecommendUrlEntity> list) {
        mCommonUrlBeans.clear();
        if (list != null) {
            BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QUICKLINK_EVENTS, AnalyticsSettings.ID_AMOUNT, String
                    .valueOf(list.size()));

            if (list.size() <= mMaxCount) {
                mCommonUrlBeans.addAll(list);
            } else {
                for (int i = 0; i < mMaxCount; i++) {
                    mCommonUrlBeans.add(list.get(i));
                }
            }
        }
        notifyDataSetChanged();
    }

    private void insert(RecommendUrlEntity entity) {
        int insertPosition = mCommonUrlBeans.size();
        if (insertPosition < mMaxCount) {
            mCommonUrlBeans.add(insertPosition, entity);
            startInsertAnimation(insertPosition);
        }
    }

    private void modify(int position) {
        if (position < 0 || position >= mCommonUrlBeans.size()) {
            return;
        }
        notifyItemChanged(position);
        if (mAdapterItemListener != null) {
            mAdapterItemListener.onDataSetChange();
        }
    }

    private void delete(int position) {
        int navCount = mCommonUrlBeans.size();
        if (position < 0 || position >= navCount) {
            return;
        }
        mCommonUrlBeans.remove(position);
        startDeleteAnimation(position);
    }

    private int getPositionByEntity(RecommendUrlEntity entity) {
        return mCommonUrlBeans.indexOf(entity);
    }

    public CommonUrlItemViewHolder getViewHolderByPosition(int position) {
        return mViewHolderSet.get(position);
    }

    @Override
    public void onInsertToDB(RecommendUrlEntity entity) {
        if (!isDeleteMode()) insert(entity);
    }

    @Override
    public void onUpdateToDB(RecommendUrlEntity entity) {
        if (!isDeleteMode()) modify(getPositionByEntity(entity));
    }

    @Override
    public void onDeleteToDB(RecommendUrlEntity entity) {
        if (!isDeleteMode()) delete(getPositionByEntity(entity));
    }

    private void startInsertAnimation(final int position) {
        final CommonUrlItemViewHolder viewHolder = mViewHolderSet.get(position);
        if (viewHolder == null) return;
        onBindNavigationViewHolder(viewHolder, position);
        ScaleAnimation scaleAnimation = new ScaleAnimation(START_VALUE, END_VALUE, START_VALUE, END_VALUE,
                Animation.RELATIVE_TO_SELF, MIDDLE_VALUE, Animation.RELATIVE_TO_SELF, MIDDLE_VALUE);
        scaleAnimation.setDuration(ANIMATION_TIME);
        scaleAnimation.setStartOffset(TITLE_ANIMATION_DELAY);
        scaleAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                viewHolder.mItemIcon.clearAnimation();
                if (mAdapterItemListener != null) {
                    mAdapterItemListener.onDataSetChange();
                }
                notifyDataSetChanged();
            }
        });
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                float output;
                //分段函数 x <= 0.8时 y = 1.5x  x>0.8 时 y = 2 - x;
                if (input <= 0.8f)
                    output = 1.5f * input;
                else
                    output = 2f - input;
                viewHolder.mItemIcon.setAlpha(output);
                return output;
            }
        });
        viewHolder.mItemIcon.startAnimation(scaleAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(ANIMATION_TIME);
        alphaAnimation.setFillAfter(true);
        viewHolder.mItemTitle.startAnimation(alphaAnimation);
    }

    private void startDeleteAnimation(final int position) {
        final CommonUrlItemViewHolder viewHolder = mViewHolderSet.get(position);
        if (viewHolder == null) return;
        //变化范围从0到1,参考点为中心点
        final ScaleAnimation scaleAnimation = new ScaleAnimation(START_VALUE, END_VALUE, START_VALUE, END_VALUE,
                Animation.RELATIVE_TO_SELF, MIDDLE_VALUE, Animation.RELATIVE_TO_SELF, MIDDLE_VALUE);
        scaleAnimation.setDuration(ANIMATION_TIME);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                float output;
                //分段函数 x >= 0.2时 y = 1.5 - 1.5x  x < 0.2时 y = x + 1;
                if (input >= 0.2f)
                    output = 1.5f - (input * 1.5f);
                else
                    output = input + 1f;
                viewHolder.mItemIcon.setAlpha(output);
                return output;
            }
        });
        viewHolder.mItemIcon.startAnimation(scaleAnimation);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
        alphaAnimation.setDuration(ANIMATION_TIME);
        alphaAnimation.setStartOffset(TITLE_ANIMATION_DELAY);
        alphaAnimation.setFillAfter(true);
        viewHolder.mItemTitle.startAnimation(alphaAnimation);
        alphaAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                int lastIndex = mCommonUrlBeans.size();
                if (position != lastIndex) {
                    notifyItemMoved(position, lastIndex);
                }
                BackgroundHandler.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.mItemIcon.setAlpha(1f);
                        viewHolder.mItemIcon.clearAnimation();
                        notifyDataSetChanged();
                        if (mAdapterItemListener != null) {
                            mAdapterItemListener.onDataSetChange();
                        }
                    }
                }, RECOVERY_DELAY);
                if (isEdit()) {
                    onBindHideSpaceViewHolder(viewHolder);
                } else {
                    onBindGraySpaceViewHolder(viewHolder);
                }
            }
        });
    }

    @Override
    public CommonUrlItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.homepage_recommend_item, parent, false);
        return new CommonUrlItemViewHolder(view);
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        int size = mCommonUrlBeans.size();
        if (fromPosition >= size || toPosition >= size) return false;
        BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QUICKLINK_EVENTS, AnalyticsSettings.ID_MOVE);
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mCommonUrlBeans, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mCommonUrlBeans, i, i - 1);
            }
        }
        mIsMove = true;
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public void fireSortChangeIfNeed() {
        if (!mIsMove) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int count = mCommonUrlBeans.size();
                int size = count;
                for (int i = 0; i < count; i++) {
                    RecommendUrlEntity entity = mCommonUrlBeans.get(i);
                    size--;
                    entity.setOrd(size);
                    entity.updateToDb();
                }
                mIsMove = false;
            }
        });
    }

    @Override
    public void onBindViewHolder(final CommonUrlItemViewHolder holder, int position) {
        mViewHolderSet.put(position, holder);
        switch (getItemType(position)) {
            case TYPE_NAV:
                onBindNavigationViewHolder(holder, position);
                break;
            case TYPE_SPACE_GRAY:
                onBindGraySpaceViewHolder(holder);
                break;
            case TYPE_ADD:
                onBindAddViewHolder(holder);
                break;
            case TYPE_SPACE_HIDE:
                onBindHideSpaceViewHolder(holder);
                break;
            default:
                //do nothing
        }
    }

    public int getItemType(int position) {
        int navSize = mCommonUrlBeans.size();
        if (position < navSize) {
            return TYPE_NAV;
        } else if (!isEdit()) {
            if (!isDeleteMode() && position == navSize)
                return TYPE_ADD;
            else
                return TYPE_SPACE_HIDE;
        } else {
            return TYPE_SPACE_GRAY;
        }
    }

    private void onBindNavigationViewHolder(final CommonUrlItemViewHolder holder, final int position) {
        holder.mItemIcon.setAlpha(1f);
        holder.mItemIcon.clearAnimation();
        final RecommendUrlEntity bean = mCommonUrlBeans.get(position);
        holder.mItemTitle.setText(bean.getDisplayName());
        holder.position = position;
        holder.mItemIcon.setVisibility(VISIBLE);
        holder.data = bean;
        if (bean.getWeight() <= WEIGHT_RECOMMEND_WEBSITE) {
            int resID = holder.mItemTitle.getContext().getResources().getIdentifier(bean.getImageUrl(),
                    "drawable", holder.mItemTitle.getContext().getPackageName());
            holder.mItemIcon.setImageResource(resID);
        } else {
            if (bean.getImageIcon() != null && bean.getImageIcon().length > 0) {
                Bitmap iconBmp = ImageUtils.getBitmap(bean.getImageIcon(), null);
                if (iconBmp != null) {
                    if (bean.getWeight() == WEIGHT_HOT_WEBSITE || bean.getWeight() == WEIGHT_BOOKMARK_WEBSITE) {
                        holder.mItemIcon.setImageBitmap(iconBmp);
                    } else {
                        holder.mItemIcon.setIcon(bean.getUrl(), iconBmp);
                    }
                } else {
                    holder.mItemIcon.setDefaultIconByUrl(bean.getUrl());
                }
            } else {
                holder.mItemIcon.setDefaultIconByUrl(bean.getUrl());
            }
        }
        if (mIsEdit || mIncognito) {
            holder.mClose.setImageResource(R.drawable.ic_browser_delete_homepage_light);
            holder.mItemTitle.setTextColor(ColorUtils.getColor(R.color.white));
        } else {
            holder.mClose.setImageResource(R.drawable.ic_browser_delete_navigation_dark);
            holder.mItemTitle.setTextColor(ColorUtils.getColor(R.color.grid_common_text_color));
        }
        holder.mClose.setTag(bean);
        holder.mClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RecommendUrlEntity entity = (RecommendUrlEntity) v.getTag();
                int position = findPositionByBean(entity);
                if (position != -1) {
                    DatabaseManager.getInstance().deleteById(RecommendUrlEntity.class, entity.getId());
                    delete(position);
                    v.setVisibility(GONE);
                }
                if (mCommonUrlBeans.size() == 0) {
                    setDeleteMode(false);
                }
                BrowserAnalytics.trackEvent(BrowserAnalytics.Event.QUICKLINK_EVENTS, AnalyticsSettings.ID_DELETE);
            }
        });
        holder.mItemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapterItemListener != null && !mIsDeleteMode) {
                    if (!mIsEdit) {
                        mAdapterItemListener.openUrl(holder);
                    } else {
                        if (holder.position < mCommonUrlBeans.size()) {
                            mAdapterItemListener.editNavigation(holder);
                        }
                    }
                }
            }
        });
        //长按编辑
        holder.mItemView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isEdit() && position < mCommonUrlBeans.size()) setDeleteMode(true);
                return false;
            }
        });
        holder.mItemIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isDeleteMode() && MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper.startDrag(holder);
                }
                return false;
            }
        });
        if (mIsDeleteMode) {
            holder.mClose.setVisibility(VISIBLE);
        } else {
            holder.mClose.setVisibility(GONE);
        }
    }

    private void onBindGraySpaceViewHolder(CommonUrlItemViewHolder holder) {
        holder.mItemIcon.setVisibility(VISIBLE);
        holder.mClose.setVisibility(GONE);
        holder.mItemTitle.setText("");
        holder.mItemIcon.setImageResource(R.drawable.navigation_place_icon);
        holder.mItemView.setOnClickListener(null);
    }

    private void onBindHideSpaceViewHolder(CommonUrlItemViewHolder holder) {
        holder.mItemIcon.setVisibility(INVISIBLE);
        holder.mClose.setVisibility(GONE);
        holder.mItemTitle.setText("");
        holder.mItemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setDeleteMode(false);
                if (mAdapterItemListener != null) {
                    mAdapterItemListener.offDeleteMode();
                }
            }
        });
    }

    private void onBindAddViewHolder(final CommonUrlItemViewHolder holder) {
        holder.mItemIcon.setVisibility(VISIBLE);
        holder.mItemIcon.setImageResource(R.drawable.ic_browser_recommend_add);
        holder.mClose.setVisibility(GONE);
        holder.mItemTitle.setText("");
        holder.mItemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAdapterItemListener != null && !mIsDeleteMode)
                    mAdapterItemListener.onClickAdd(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMaxCount;
    }

    public int getVisibleCount() {
        if (isEdit()) {
            return mMaxCount;
        }
        int dataSize = mCommonUrlBeans.size();
        if (mIsDeleteMode) {
            return dataSize;
        } else {
            return dataSize + 1;
        }
    }


    public void refreshInputUrlAndRecommend() {
        new AsyncTask<Void, Void, Void>() {
            List<RecommendUrlEntity> recommendUrls = null;

            @Override
            protected Void doInBackground(Void... vs) {
                recommendUrls = RecommendUrlUtil.getLocalRecommendInfos();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                replaceData(recommendUrls);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class CommonUrlItemViewHolder extends RecyclerView.ViewHolder {

        private TextView mItemTitle;
        private RoundImageView mItemIcon;
        private ImageView mClose;
        public CloneableRelativeLayout mItemView;
        public int position;
        public RecommendUrlEntity data;

        CommonUrlItemViewHolder(View itemView) {
            super(itemView);
            mItemView = (CloneableRelativeLayout) itemView;
            mItemIcon = (RoundImageView) itemView.findViewById(R.id.recommend_item_icon);
            mItemTitle = (TextView) itemView.findViewById(R.id.recommend_item_title);
            mClose = (ImageView) itemView.findViewById(R.id.recommend_item_close);
            mItemIcon.setRoundBg(ColorUtils.getColor(R.color.transparent));
        }
    }

    private int findPositionByBean(RecommendUrlEntity entity) {
        int size = mCommonUrlBeans.size();
        for (int i = 0; i < size; i++) {
            if (mCommonUrlBeans.get(i) == entity) {
                return i;
            }
        }
        return -1;
    }

    public interface AdapterItemListener {

        void openUrl(CommonUrlItemViewHolder viewHolder);

        void editNavigation(CommonUrlItemViewHolder viewHolder);

        void onClickAdd(CommonUrlItemViewHolder viewHolder);

        void onDataSetChange();

        void onDeleteMode();

        void offDeleteMode();

    }

}
