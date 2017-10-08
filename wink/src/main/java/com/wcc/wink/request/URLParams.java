package com.wcc.wink.request;

import java.io.File;

public class URLParams<E> {

    public interface URLParamsCreator<T> {
        URLParams<T> createUrlParams(T entity);
    }

    String url;
    File target;
    E entity;
}
