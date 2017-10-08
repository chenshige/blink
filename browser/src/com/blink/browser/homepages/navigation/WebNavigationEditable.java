package com.blink.browser.homepages.navigation;

import com.blink.browser.bean.RecommendUrlEntity;

public interface WebNavigationEditable {

    boolean addNewNavigation(String title, String url, boolean needCheck);

    void onFinishAddNewNavigation();

    boolean modifyNavigation(int position, RecommendUrlEntity entity, String newTitle, String newUrl);

    boolean deleteNavigation(int position);

    int doUrlContained(String url);

    boolean isEdit();

}
