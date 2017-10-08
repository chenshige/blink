/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.URLUtil;

import com.blink.browser.search.SearchEngine;
import com.blink.browser.search.SearchEngineInfo;
import com.blink.browser.search.SearchEngines;
import com.blink.browser.util.WebAddress;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for Url manipulation
 */
public class UrlUtils {

    static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
            "(?i)" + // switch on case insensitive matching
                    "(" +    // begin group for schema
                    "(?:http|https|file|content):\\/\\/" +
                    "|(?:data|about|javascript):" +
                    "|(?:.*:.*@)" +
                    ")" +
                    "(.*)");

    static final Pattern URL_PATTERN = Pattern.compile("^((https|http|ftp|rtsp|mms)?://)"
            + "+(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?"
            + "(([0-9]{1,3}\\.){3}[0-9]{1,3}"
            + "|"
            + "([0-9a-z_!~*'()-]+\\.)*"
            + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."
            + "[a-z]{2,6})"
            + "(:[0-9]{1,4})?"
            + "((/?)|"
            + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$");

    // Google search
    private final static String QUICKSEARCH_G = "http://www.google.com/m?q=%s";
    private final static String QUERY_PLACE_HOLDER = "%s";

    // Regular expression to strip http:// and optionally
    // the trailing slash
    private static final Pattern STRIP_URL_PATTERN = Pattern.compile("^http://(.*?)/?$");
    public static String sCurrentSearchKey;

    private UrlUtils() { /* cannot be instantiated */ }

    /**
     * Strips the provided url of preceding "http://" and any trailing "/". Does not
     * strip "https://". If the provided string cannot be stripped, the original string
     * is returned.
     * <p>
     * TODO: Put this in TextUtils to be used by other packages doing something similar.
     *
     * @param url a url to strip, like "http://www.google.com/"
     * @return a stripped url like "www.google.com", or the original string if it could
     * not be stripped
     */
    public static String stripUrl(String url) {
        if (url == null) return null;
        Matcher m = STRIP_URL_PATTERN.matcher(url);
        if (m.matches()) {
            return m.group(1);
        } else {
            return url;
        }
    }

    protected static String smartUrlFilter(Uri inUri) {
        if (inUri != null) {
            return smartUrlFilter(inUri.toString());
        }
        return null;
    }

    public static boolean checkUrl(String url) {
        return !TextUtils.isEmpty(url) && URL_PATTERN.matcher(url).matches();
    }

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search.
     * <p>
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @return Original or modified URL
     */
    public static String smartUrlFilter(String url) {
        return smartUrlFilter(url, true);
    }

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search if canBeSearch is true.
     * <p>
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @param canBeSearch If true, will return a search url if it isn't a valid
     *                    URL. If false, invalid URLs will return null
     * @return Original or modified URL
     */
    public static String smartUrlFilter(String url, boolean canBeSearch) {
        String inUrl = url.trim();
        boolean hasSpace = inUrl.indexOf(' ') != -1;
        //inUrl全部小写只是为了前半部分方便匹配 WEB_URL
        String inUrlLowCase = inUrl.toLowerCase();
        // 避免调用URLUtil里framework/net/WebAddress处理一些url中#
        if (Patterns.WEB_URL.matcher(inUrlLowCase).matches() && !ACCEPTED_URI_SCHEMA.matcher(inUrl).matches()) {
            inUrl = "http://" + inUrl;
        }
        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
        if (matcher.matches()) {
            // force scheme to lowercase
            String scheme = matcher.group(1);
            String lcScheme = scheme.toLowerCase();
            if (!lcScheme.equals(scheme)) {
                inUrl = lcScheme + matcher.group(2);
            }
            if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = inUrl.replace(" ", "%20");
            }
            return inUrl;
        }
        if (!hasSpace) {
            if (Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = URLUtil.guessUrl(inUrl);
                Uri uri = Uri.parse(inUrl);
                String host = uri.getHost().toLowerCase();
                inUrl = uri.getScheme().toString() + "://" + host
                        + uri.toString().substring(host.length() + uri.getScheme().length() + 3);
                return inUrl;
            }
        }
        if (canBeSearch) {
            return URLUtil.composeSearchUrl(inUrl,
                    QUICKSEARCH_G, QUERY_PLACE_HOLDER);
        }
        return null;
    }

    public static boolean isSearch(String inUrl) {
        String url = fixUrl(inUrl).trim();
        if (TextUtils.isEmpty(url)) return false;

        String urlLowcase = url.toLowerCase();
        if (Patterns.WEB_URL.matcher(urlLowcase).matches()
                || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(urlLowcase).matches()) {
            return false;
        }
        return true;
    }

    public static String filterBySearchEngine(Context context, String url) {
        SearchEngine searchEngine = BrowserSettings.getInstance()
                .getSearchEngine();
        if (searchEngine == null) return null;
        SearchEngineInfo engineInfo = SearchEngines
                .getInstance(context).getSearchEngineInfo(context, searchEngine.getName());
        if (engineInfo == null) return null;
        return engineInfo.getSearchUriForQuery(url);
    }

    public static String fixUrl(String inUrl) {
        // FIXME: Converting the url to lower case
        // duplicates functionality in smartUrlFilter().
        // However, changing all current callers of fixUrl to
        // call smartUrlFilter in addition may have unwanted
        // consequences, and is deferred for now.
        if (inUrl == null) {
            return "";
        }
        int colon = inUrl.indexOf(':');
        boolean allLower = true;
        for (int index = 0; index < colon; index++) {
            char ch = inUrl.charAt(index);
            if (!Character.isLetter(ch)) {
                break;
            }
            allLower &= Character.isLowerCase(ch);
            if (index == colon - 1 && !allLower) {
                inUrl = inUrl.substring(0, colon).toLowerCase()
                        + inUrl.substring(colon);
            }
        }
        if (inUrl.startsWith("http://") || inUrl.startsWith("https://")) {
            return inUrl;
        }
        if (inUrl.startsWith("http:") ||
                inUrl.startsWith("https:")) {
            if (inUrl.startsWith("http:/") || inUrl.startsWith("https:/")) {
                inUrl = inUrl.replaceFirst("/", "//");
            } else {
                inUrl = inUrl.replaceFirst(":", "://");
            }
        }
        return inUrl;
    }

    // Returns the filtered URL. Cannot return null, but can return an empty string
    /* package */
    static String filteredUrl(String inUrl) {
        if (inUrl == null) {
            return "";
        }
        if (inUrl.startsWith("content:")
                || inUrl.startsWith("browser:")) {
            return "";
        }
        return inUrl;
    }

    public static String getHost(String url) {
        String result = "";
        try {
            // url is null or "", so check it.
            if (!TextUtils.isEmpty(url)) {
                result = new URL(UrlUtils.smartUrlFilter(url, false)).getHost();
            } else {
                return result;
            }
        } catch (MalformedURLException e) {
        }
        return result;
    }

    public static String getSchemePrefix(String spec) {
        int colon = spec.indexOf(':');

        if (colon < 1) {
            return null;
        }

        for (int i = 0; i < colon; i++) {
            char c = spec.charAt(i);
            if (!isValidSchemeChar(i, c)) {
                return null;
            }
        }

        return spec.substring(0, colon).toLowerCase(Locale.US);
    }

    public static boolean isValidSchemeChar(int index, char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return true;
        }
        return index > 0 && ((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.');
    }

    public static boolean isSafeUrl(String url) {
        return !TextUtils.isEmpty(url) && url.startsWith("https");
    }

    private static final String[] IMAGE_TYPES = {".webp", ".bmp", ".dib", ".gif", ".jfif", ".jpe", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".ico"};

    public static boolean checkUrlDataIsImg(String url) {
        for (String imageType : IMAGE_TYPES) {
            if (url.toLowerCase().contains(imageType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkUrlDataIsJs(String url) {
        return url.toLowerCase().endsWith(".js");
    }

    public static boolean checkUrlBelongToHost(String urlString, String host) {
        try {
            WebAddress address = new WebAddress(urlString);
            return address.getHost().endsWith(host);
        } catch (MalformedURLException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }


}
