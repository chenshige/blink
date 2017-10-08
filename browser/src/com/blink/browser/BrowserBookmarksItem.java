/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blink.browser;

import android.graphics.Bitmap;

public class BrowserBookmarksItem {
    public String url;
    public String title;
    public Bitmap thumbnail;
    public boolean hasThumbnail;
    public Bitmap favicon;
    public boolean isFolder;
    public String id; //数据库中的id，update用
    public boolean isSelected = false; //是否被选中
    public long date; //时间。显示到天
    public long time; //创建时间，毫秒
}
