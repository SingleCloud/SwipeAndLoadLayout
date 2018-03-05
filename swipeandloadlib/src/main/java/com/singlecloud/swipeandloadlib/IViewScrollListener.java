package com.singlecloud.swipeandloadlib;

public interface IViewScrollListener {

    /**
     * The threshold for the refresh or load
     */
    float getThreshold();

    /**
     * The view change into visible.
     */
    void onStart();

    /**
     * on refreshing/loading
     */
    void onExecuting();

    /**
     * If release view will perform a refresh operation .
     */
    void onThreshold(boolean willBeExecuted);

}
