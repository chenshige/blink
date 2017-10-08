package com.wcc.wink.loader;

/**
 * Created by wenbiao.xie on 2016/6/14.
 */
public interface UrlFetcher<T> {

    int load(T data, boolean force) throws Exception;

    /**
     * Returns a string uniquely identifying the data that this fetcher will fetch including the specific size.
     *
     * <p>
     *     A hash of the bytes of the data that will be fetched is the ideal id but since that is in many cases
     *     impractical, urls, file paths, and uris are normally sufficient.
     * </p>
     *
     * <p>
     *     Note - this method will be run on the main thread so it should not perform blocking operations and should
     *     finish quickly.
     * </p>
     */
    String getId();

    /**
     * A method that will be called when a load is no longer relevant and has been cancelled. This method does not need
     * to guarantee that any in process loads do not finish. It also may be called before a load starts or after it
     * finishes.
     *
     * <p>
     *  The best way to use this method is to cancel any loads that have not yet started, but allow those that are in
     *  process to finish since its we typically will want to display the same resource in a different view in
     *  the near future.
     * </p>
     *
     * <p>
     *     Note - this method will be run on the main thread so it should not perform blocking operations and should
     *     finish quickly.
     * </p>
     */
    void cancel();
}
