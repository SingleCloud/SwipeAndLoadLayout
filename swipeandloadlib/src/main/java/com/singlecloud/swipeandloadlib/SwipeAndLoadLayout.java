package com.singlecloud.swipeandloadlib;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

public class SwipeAndLoadLayout extends RelativeLayout {

    private static final String TAG = "#Genlan#";

    private View mHeaderView;
    private IViewScrollListener mHeaderListener;
    private View mFooterView;
    private IViewScrollListener mFooterListener;
    private View mTarget;

    private int mAnimationTime = 500;

    private boolean mIsRefreshEnable, mIsLoadMoreEnable;
    private boolean mIsRefreshing, mIsLoadingMore;
    private boolean mIsDraggingHeader, mIsDraggingFooter;

    private OnRefreshListener mOnRefreshListener;
    private OnLoadMoreListener mOnLoadMoreListener;

    private float mLastPressedY;
    private float mInitialPressedX, mInitialPressedY;

    private int mDragDistance;
    private int mMaxDragHeight;

    /**
     * the default height of header/footer
     */
    private int mHeaderOffset, mFooterOffset;

    /**
     * true  ---- do refresh
     * false ---- do load more
     */
    private boolean mIsRefresh;

    public SwipeAndLoadLayout(Context context) {
        this(context, null);
    }

    public SwipeAndLoadLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeAndLoadLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeAndLoadLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mIsRefreshEnable = true;
        mIsLoadMoreEnable = true;
        mIsRefreshing = false;
        mIsLoadingMore = false;
        mIsDraggingHeader = false;
        mIsDraggingFooter = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ensureTarget();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mTarget == null)
            ensureTarget();
        if (mTarget == null)
            return;
        super.onLayout(changed, l, t, r, b);
    }

    public void onRefreshCompleted() {
        mIsRefreshing = false;
        ViewWrapper wrapper = new ViewWrapper(mHeaderView);
        ObjectAnimator anim = ObjectAnimator.ofInt(wrapper, "marginTop", 0, mHeaderOffset);
        anim.setDuration(mAnimationTime);
        anim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mHeaderView.setVisibility(GONE);
            }
        });
        anim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastPressedY = mInitialPressedY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsRefresh) {
                    if (mHeaderView.getTop() <= 0) {
                        mHeaderView.setVisibility(VISIBLE);
                        LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
                        mDragDistance = Math.min((int) (mLastPressedY - mInitialPressedY), mMaxDragHeight);
                        params.topMargin = (mHeaderOffset + mDragDistance);
                        mHeaderView.setLayoutParams(params);
                        mHeaderListener.onThreshold(mHeaderView.getTop() >= 0);
                    } else {
                        float temp = (Math.abs(mHeaderOffset) + (mLastPressedY - mInitialPressedY - Math.abs(mHeaderOffset)));
                        float height = temp > mMaxDragHeight ?mMaxDragHeight:temp;
                        mHeaderView.getLayoutParams().height = (int) height;
                        mHeaderView.requestLayout();
                    }
                } else {
                    mFooterView.setVisibility(VISIBLE);
                    LayoutParams params = (LayoutParams) mFooterView.getLayoutParams();
                    mDragDistance = (int) Math.min(mLastPressedY - mInitialPressedY, 0 - mMaxDragHeight);
                    params.bottomMargin = (mFooterOffset + Math.abs(mDragDistance));
                    mFooterView.setLayoutParams(params);
                    mFooterListener.onThreshold(mFooterView.getBottom() >= 0);
                }
                mLastPressedY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (mIsRefresh) {
                    mIsDraggingHeader = false;
                    if (mHeaderView.getTop() >= 0) {
                        ViewWrapper wrapper = new ViewWrapper(mHeaderView);
                        ObjectAnimator animHeight = ObjectAnimator.ofInt(wrapper, "height", ((LayoutParams) mHeaderView.getLayoutParams()).height, Math.abs(mHeaderOffset));
                        ObjectAnimator animMargin = ObjectAnimator.ofInt(wrapper, "marginTop", ((LayoutParams) mHeaderView.getLayoutParams()).topMargin, 0);
                        AnimatorSet set = new AnimatorSet();
                        set.play(animHeight).with(animMargin);
                        set.setDuration(mAnimationTime);
                        set.addListener(new SimpleAnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator animation, boolean isReverse) {
                                mHeaderListener.onExecuting();
                                mIsRefreshing = true;
                                if (mOnRefreshListener != null)
                                    mOnRefreshListener.onRefresh();
                            }
                        });
                        set.start();
                    } else {
                        if (mHeaderView.getBottom() >= 0) {
                            ViewWrapper wrapper = new ViewWrapper(mHeaderView);
                            ObjectAnimator anim = ObjectAnimator.ofInt(wrapper, "marginTop", mHeaderView.getTop(), mHeaderOffset);
                            anim.setDuration(mAnimationTime);
                            anim.addListener(new SimpleAnimatorListener() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mHeaderView.setVisibility(GONE);
                                }
                            });
                            anim.start();
                        } else {
                            ((LayoutParams) mHeaderView.getLayoutParams()).topMargin = mHeaderOffset;
                            mHeaderView.setVisibility(GONE);
                        }
                    }
                } else {
                    mIsDraggingFooter = false;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialPressedX = ev.getX();
                mLastPressedY = mInitialPressedY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsRefreshing) return super.onInterceptTouchEvent(ev);
                if (mIsLoadingMore) return super.onInterceptTouchEvent(ev);
                final float yInitDiff = ev.getY() - mInitialPressedY;
                final float xInitDiff = ev.getX() - mInitialPressedX;
                boolean moved = Math.abs(yInitDiff) > Math.abs(xInitDiff) && Math.abs(yInitDiff) > 0;
                boolean canRefresh = mIsRefreshEnable && yInitDiff > 0 && moved && !canAnyChildScrollDown(mTarget);
                boolean canLoadMore = mIsLoadMoreEnable && yInitDiff < 0 && moved && !canAnyChildScrollUp(mTarget);
                if (canRefresh) {
                    if (mHeaderView == null) return super.onInterceptTouchEvent(ev);
                    mHeaderListener.onStart();
                    mIsDraggingHeader = true;
                    mIsRefresh = true;
                    return true;
                }
                if (canLoadMore) {
                    if (mFooterView == null) return super.onInterceptTouchEvent(ev);
                    mFooterListener.onStart();
                    mIsDraggingFooter = true;
                    mIsRefresh = false;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDraggingHeader || mIsDraggingFooter) return true;
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.mOnLoadMoreListener = onLoadMoreListener;
    }

    public void setHeaderView(View view) {
        if (view instanceof IViewScrollListener) {
            this.mHeaderListener = (IViewScrollListener) view;
        }
        if (mHeaderListener == null)
            throw new IllegalArgumentException("the refresh view must be implements IViewScrollListener");
        if (mHeaderView != null && mHeaderView != view) removeView(mHeaderView);
        this.mHeaderView = view;
        LayoutParams paramsHeader = (LayoutParams) view.getLayoutParams();
        if (paramsHeader == null)
            paramsHeader = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mHeaderListener.getThreshold());
        paramsHeader.alignWithParent = true;
        int width = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mHeaderView.measure(width, height);
        height = mHeaderView.getMeasuredHeight();
        mHeaderOffset = 0 - height;
        paramsHeader.topMargin = mHeaderOffset;
        paramsHeader.addRule(ALIGN_PARENT_TOP);
        mHeaderView.setId(R.id.refresh_header);
        this.mHeaderView.setLayoutParams(paramsHeader);
        this.mHeaderView.setVisibility(GONE);
        addView(mHeaderView);
        mMaxDragHeight = (int) (Math.abs(height) * 1.618);
    }

    public void setHeaderView(View view, IViewScrollListener listener) {
        this.mHeaderListener = listener;
        setHeaderView(view);
    }

    public void setFooterView(View view) {
        if (view instanceof IViewScrollListener) {
            this.mFooterListener = (IViewScrollListener) view;
        }
        if (mFooterListener == null)
            throw new IllegalArgumentException("the refresh view must be implements IViewScrollListener");
        if (mFooterView != null && mFooterView != view) removeView(mFooterView);
        this.mFooterView = view;
        LayoutParams paramsFooter = (LayoutParams) view.getLayoutParams();
        if (paramsFooter == null)
            paramsFooter = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mHeaderListener.getThreshold());
        paramsFooter.alignWithParent = true;
        int width = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mHeaderView.measure(width, height);
        height = mHeaderView.getMeasuredHeight();
        mFooterOffset = 0 - height;
        paramsFooter.bottomMargin = mFooterOffset;
        paramsFooter.addRule(ALIGN_PARENT_BOTTOM);
        this.mFooterView.setLayoutParams(paramsFooter);
        this.mFooterView.setVisibility(GONE);
        addView(mFooterView);
        mMaxDragHeight = (int) (Math.abs(height) * 1.618);
    }

    public void setFooterView(View view, IViewScrollListener listener) {
        this.mFooterListener = listener;
        setFooterView(view);
    }

    public void setRefreshEnable(boolean refreshEnable) {
        this.mIsRefreshEnable = refreshEnable;
    }

    public void setLoadMoreEnable(boolean loadMoreEnable) {
        this.mIsLoadMoreEnable = loadMoreEnable;
    }

    public boolean isLoadMoreEnable() {
        return mIsLoadMoreEnable;
    }

    public boolean isRefreshEnable() {
        return mIsRefreshEnable;
    }

    public boolean isLoadingMore() {
        return mIsLoadingMore;
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    public void loadMoreCompleted() {
        this.mIsLoadingMore = false;
    }

    private void ensureTarget() {
        if ((mHeaderView != null && mFooterView != null && getChildCount() > 3) ||
                (mHeaderView == null && mFooterView != null && getChildCount() > 2) ||
                (mHeaderView != null && mFooterView == null && getChildCount() > 2) ||
                (mHeaderView == null && mFooterView == null && getChildCount() > 1)) {
            throw new IllegalArgumentException("Child view must be one,you can contain it for a view group;");
        }
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mHeaderView) && !child.equals(mFooterView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    private boolean canAnyChildScrollUp(View view) {
        if (view instanceof ListView || !(view instanceof ViewGroup)) {
            return view.canScrollVertically(1);
        } else {
            if (((ViewGroup) view).getChildCount() == 0) return false;
            int k = 1;
            boolean canScroll;
            while (k < ((ViewGroup) view).getChildCount()) {
                canScroll = canAnyChildScrollUp(((ViewGroup) view).getChildAt(k));
                if (canScroll) return true;
                k++;
            }
            return false;
        }
    }

    private boolean canAnyChildScrollDown(View view) {
        if (view instanceof ListView || !(view instanceof ViewGroup)) {
            return view.canScrollVertically(-1);
        } else {
            if (((ViewGroup) view).getChildCount() == 0) return false;
            int k = 1;
            boolean canScroll = false;
            while (k < ((ViewGroup) view).getChildCount()) {
                canScroll = canAnyChildScrollDown(((ViewGroup) view).getChildAt(k));
                k++;
            }
            return canScroll;
        }
    }

    private abstract class SimpleAnimatorListener implements Animator.AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation, boolean isReverse) {

        }

        @Override
        public void onAnimationEnd(Animator animation, boolean isReverse) {

        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

    private static class ViewWrapper {
        private WeakReference<View> mWeak;

        private ViewWrapper(View view) {
            mWeak = new WeakReference<>(view);
        }

        private void setMarginTop(int marginTop) {
            if (mWeak == null) return;
            ((LayoutParams) mWeak.get().getLayoutParams()).topMargin = marginTop;
            mWeak.get().requestLayout();
        }

        private void setMarginBottom(int marginBottom) {
            if (mWeak == null) return;
            ((LayoutParams) mWeak.get().getLayoutParams()).bottomMargin = marginBottom;
            mWeak.get().requestLayout();
        }

        private void setHeight(int height) {
            if (mWeak == null) return;
            ((LayoutParams) mWeak.get().getLayoutParams()).height = height;
            mWeak.get().requestLayout();
        }
    }
}