package com.blink.browser.view.switchsearchengine;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.blink.browser.handler.BrowserHandler;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineItemAnimator extends SimpleItemAnimator {

    private List<RecyclerView.ViewHolder> mAnimationAddViewHolders = new ArrayList<>();
    private List<RecyclerView.ViewHolder> mAnimationRemoveViewHolders = new ArrayList<>();
    private OnAnimationEnd mAnimationEnd;

    //需要执行动画时会系统会调用，用户无需手动调用
    @Override
    public void runPendingAnimations() {
        if (!mAnimationAddViewHolders.isEmpty()) {
            addAnimaStart();
        } else if (!mAnimationRemoveViewHolders.isEmpty()) {
            removeAnimaStart();
        }
    }

    //remove时系统会调用，返回值表示是否需要执行动画
    @Override
    public boolean animateRemove(RecyclerView.ViewHolder viewHolder) {
        return mAnimationRemoveViewHolders.add(viewHolder);
    }

    //viewholder添加时系统会调用
    @Override
    public boolean animateAdd(RecyclerView.ViewHolder viewHolder) {
        return mAnimationAddViewHolders.add(viewHolder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder viewHolder, int fromX, int fromY, int toX, int toY) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        return false;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void endAnimations() {

    }

    @Override
    public boolean isRunning() {
        return !(mAnimationAddViewHolders.isEmpty() && mAnimationRemoveViewHolders.isEmpty());
    }

    private void addAnimaStart(){
        for (int i = 0; i < mAnimationAddViewHolders.size(); i++) {
            final RecyclerView.ViewHolder viewHolder = mAnimationAddViewHolders.get(i);
            final View target = viewHolder.itemView;
            final AnimatorSet animator = new AnimatorSet();
            animator.playTogether(
                    ObjectAnimator.ofFloat(target, "scaleX", 0, 1.05f, 1.0f),
                    ObjectAnimator.ofFloat(target, "scaleY", 0, 1.05f, 1.0f)
            );

            animator.setTarget(target);
            animator.setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());

            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimationAddViewHolders.remove(viewHolder);
                    if (!isRunning() && mAnimationAddViewHolders.size() == 0) {
                        dispatchAnimationsFinished();
                        if (mAnimationEnd != null) {
                            mAnimationEnd.onAddItemAnimationEnd();
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });


            BrowserHandler.getInstance().handlerPostDelayed(new Runnable() {
                @Override
                public void run() {
                    target.setVisibility(View.VISIBLE);
                    animator.start();
                }
            }, viewHolder.getPosition() * 20);

        }
    }

    private void removeAnimaStart(){
        for (int i = 0; i < mAnimationRemoveViewHolders.size(); i++) {
            final RecyclerView.ViewHolder viewHolder = mAnimationRemoveViewHolders.get(i);
            final View target = viewHolder.itemView;
            target.setVisibility(View.INVISIBLE);
            final AnimatorSet animator = new AnimatorSet();

            animator.playTogether(
                    ObjectAnimator.ofFloat(target, "scaleX", 1.0f, 1.05f, 0.0f),
                    ObjectAnimator.ofFloat(target, "scaleY", 1.0f, 1.05f, 0.0f)
            );

            animator.setTarget(target);
            animator.setDuration(50);
            animator.setInterpolator(new AccelerateInterpolator());
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimationRemoveViewHolders.remove(viewHolder);
                    if (!isRunning()) {
                        dispatchAnimationsFinished();
                        if (mAnimationEnd != null) {
                            mAnimationEnd.onRemoveItemAnimationEnd();
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            animator.start();
        }
    }

    public void setOnAnimationEnd(OnAnimationEnd onAnimationEnd) {
        mAnimationEnd = onAnimationEnd;
    }

    interface OnAnimationEnd {
        void onAddItemAnimationEnd();

        void onRemoveItemAnimationEnd();
    }

}
