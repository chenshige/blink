// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adblock;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.blink.browser.R;
import com.blink.browser.adblock.AdBlockDataUpdator;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.Logger;
import com.blink.browser.util.ToastUtil;
import com.blink.browser.util.WebAddress;

import java.util.ArrayList;
import java.util.List;

public class AdsFilter {
    public static final String PERFIX_CLASS_NAME = ".";
    public static final String PERFIX_ID = "#";
    public static final String PERFIX_STYLE = "style=";

    public static final String ADS_PATH_FILTER_CLASSNAME = "(function () {\n" +
            "   \"use strict\";\n" +
            "return document.querySelectorAll('IMG[src$=\"PATH\"]')[0].parentNode.parentNode.parentNode.className\n" +
            "}());";
    public static final String ADS_PATH_FILTER_ID = "(function () {\n" +
            "   \"use strict\";\n" +
            "return document.querySelectorAll('IMG[src$=\"PATH\"]')[0].parentNode.parentNode.parentNode.id\n" +
            "}());";

    public static final String ADS_PATH_FILTER_STYLE = "(function () {\n" +
            "   \"use strict\";\n" +
            "return document.querySelectorAll('IMG[src$=\"PATH\"]')[0].parentNode.parentNode.parentNode.getAttribute('style')\n" +
            "}());";

    public static final String ADS_PATH_FILTER = "(function () {\n" +
            "   \"use strict\";\n" +
            "   var screenHeight = screen.height;\n" +
            "   function evaluateNode(node) {\n" +
            "       if (node != null && node.tagName != 'BODY' " +
            "               && node.childNode == 1" +
            "               && node.offsetHeight < screenHeight / 3 * 2){\n" +
            "           return true;\n" +
            "       }\n" +
            "       return false;\n" +
            "   }\n" +
            "\n\n" +
            "   function getRule(currentNode) {\n" +
            "       className = currentNode.className;\n" +
            "       if (className != null && className != \"\") {\n" +
            "           return '.' + className;\n" +
            "       } else {\n" +
            "           id = currentNode.id;\n" +
            "           if (id != null && id != \"\") {\n" +
            "               return '#' + id;\n" +
            "           } else {\n" +
            "               style = currentNode.getAttribute('style');\n" +
            "               return 'style=' + '\"' + style + '\"';\n" +
            "           }\n" +
            "       }\n" +
            "   }\n" +
            "\n\n" +

            "   var className, id, style, rule;\n" +
            "   var currentNode = document.querySelectorAll('IMG[src$=\"PATH\"]')[0];\n" +
            "   if (currentNode == null || currentNode.tagName == 'BODY') {\n" +
            "       return '';\n" +
            "   }\n" +
            "   currentNode = currentNode.parentNode.parentNode;\n" +
            "   var parentNode = currentNode.parentNode;\n" +
            "   while (parentNode != null && parentNode.tagName != 'BODY' && evaluateNode(parentNode)) {\n" +
            "       currentNode = currentNode.parentNode;\n" +
            "       parentNode = currentNode.parentNode;\n" +
            "   }\n"+
            "   if (currentNode != null && (currentNode.tagName == 'BODY' || currentNode.offsetHeight > screenHeight / 3 * 2)){\n" +
            "       currentNode = document.querySelectorAll('IMG[src$=\"PATH\"]')[0].parentNode;\n" +
            "   }\n" +
            "   if (currentNode != null && currentNode.tagName != 'BODY') {\n" +
            "       rule = getRule(currentNode);\n" +
            "       while (rule == \"style=null\") {\n" +
            "           parentNode = currentNode.parentNode;\n" +
            "           if (parentNode != null && parentNode.tagName != 'BODY' && parentNode.offsetHeight < screenHeight / 3 * 2) {\n" +
            "               currentNode = parentNode;\n" +
            "               rule = getRule(currentNode);\n" +
            "           } else {\n" +
            "               break;\n" +
            "           }\n" +
            "       }\n" +
            "   }\n" +
            "   if (rule != \"style=null\" && currentNode.tagName != 'BODY'){\n" +
            "       currentNode.style.display = 'none';\n" +
            "   }\n" +
            "   return rule;\n" +
            "}());\n";


    public static String getAdsFilterRule(final Context context, final String src, final WebView webView) {
        if (webView == null || !(webView instanceof WebView) || TextUtils.isEmpty(src)) {
            return null;
        }
        String srcPath = null;
        try {
            srcPath = new WebAddress(src).getPath();
        } catch (Exception e) {
            return null;
        }
        String rule = null;
        String id_filter = ADS_PATH_FILTER.replaceAll("PATH", srcPath);
        Logger.error("ZH", "filter=" + id_filter);
        if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(id_filter, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Logger.error("ZH", "onReceiveValue-->s="+ s);
                    s = handleRules(s,webView);
                    if (TextUtils.isEmpty(s)) {
                        reportAd(context, webView.getUrl(), src, null);
                        ToastUtil.showLongToast(context.getApplicationContext(), R.string.mark_image_ad_fail);
                        return;
                    }
                    List<String> temp = new ArrayList<String>();
                    temp.add(s);
                    AdBlockDataUpdator.getInstance().appendAdMarkRules(temp);
                    reportAd(context, webView.getUrl(), src, s);
                }
            });
        }

        return rule;
    }


    /*
#1 class or id
#2 webview.geturl -> webaddress -> gethost()
#3 if id != null:
        host###id
   else class !=- null:
    #1 replace of ' ' by . --> class2
    #2 host##.class2
   else
      #1 fetch style content
      #2 host##TagName[style="style content"]
*/

    private static String handleRules(String rule, WebView webView) {
        if (webView == null || TextUtils.isEmpty(rule)) {
            return null;
        }
        String url = webView.getUrl();
        String host = null;
        try {
            host = new WebAddress(url).getHost();
        } catch (Exception e) {
        }
        if (TextUtils.isEmpty(host)) {
            return null;
        }
        rule = rule.replaceAll("\"", "");
        if (rule.startsWith(PERFIX_ID)) {
            return host + "##" + rule;
        } else if (rule.startsWith(PERFIX_CLASS_NAME)) {
            rule = rule.replaceAll(" ", ".");
            return host + "##" + rule;
        } else if (rule.startsWith(PERFIX_STYLE)) {
            return host + "##" + "TagName[style=\"" + rule + "\"]";
        }
        return null;
    }

    private static void reportAd(Context context, String url, String src, String rule) {
    }
}
