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
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blink.browser.util.Logger;
import com.blink.browser.widget.BrowserDialog;

import java.lang.reflect.Method;


/**
 * Displays page info
 *
 */
public class PageDialogsHandler {

    private Context mContext;
    private Controller mController;
    private boolean mPageInfoFromShowSSLCertificateOnError;
    private String mUrlCertificateOnError;
    private Tab mPageInfoView;
    private BrowserDialog mPageInfoDialog;

    // as SSLCertificateOnError has different style for landscape / portrait,
    // we have to re-open it when configuration changed
    private BrowserDialog mSSLCertificateOnErrorDialog;
    private WebView mSSLCertificateOnErrorView;
    private SslErrorHandler mSSLCertificateOnErrorHandler;
    private SslError mSSLCertificateOnErrorError;

    // as SSLCertificate has different style for landscape / portrait, we
    // have to re-open it when configuration changed
    private BrowserDialog mSSLCertificateDialog;
    private Tab mSSLCertificateView;
    private HttpAuthenticationDialog mHttpAuthenticationDialog;

    private static Method mInflateCertificateViewMehtod;

    public PageDialogsHandler(Context context, Controller controller) {
        mContext = context;
        mController = controller;

        try {
            Class<?> clazz = getClass().getClassLoader().loadClass("android.net.http.SslCertificate");
            mInflateCertificateViewMehtod = clazz.getDeclaredMethod("inflateCertificateView", Context.class);
        } catch (Exception e) {
            //throw new RuntimeException("Invalid reflect", e);
            Logger.error("PageDialogsHanlder", "inflateCertificateView can not reflect");
        }
    }

    public void onConfigurationChanged(Configuration config) {
        if (mPageInfoDialog != null) {
            mPageInfoDialog.dismiss();
            showPageInfo(mPageInfoView,
                         mPageInfoFromShowSSLCertificateOnError,
                         mUrlCertificateOnError);
        }
        if (mSSLCertificateDialog != null) {
            mSSLCertificateDialog.dismiss();
            showSSLCertificate(mSSLCertificateView);
        }
        if (mSSLCertificateOnErrorDialog != null) {
            mSSLCertificateOnErrorDialog.dismiss();
            showSSLCertificateOnError(mSSLCertificateOnErrorView,
                                      mSSLCertificateOnErrorHandler,
                                      mSSLCertificateOnErrorError);
        }
        if (mHttpAuthenticationDialog != null) {
            mHttpAuthenticationDialog.reshow();
        }
    }

    /**
     * Displays an http-authentication dialog.
     */
    void showHttpAuthentication(final Tab tab, final HttpAuthHandler handler, String host, String realm) {
        mHttpAuthenticationDialog = new HttpAuthenticationDialog(mContext, host, realm);
        mHttpAuthenticationDialog.setOkListener(new HttpAuthenticationDialog.OkListener() {
            public void onOk(String host, String realm, String username, String password) {
                setHttpAuthUsernamePassword(host, realm, username, password);
                handler.proceed(username, password);
                mHttpAuthenticationDialog = null;
            }
        });
        mHttpAuthenticationDialog.setCancelListener(new HttpAuthenticationDialog.CancelListener() {
            public void onCancel() {
                handler.cancel();
                mController.onUpdatedSecurityState(tab);
                mHttpAuthenticationDialog = null;
            }
        });
        mHttpAuthenticationDialog.show();
    }

    /**
     * Set HTTP authentication password.
     *
     * @param host The host for the password
     * @param realm The realm for the password
     * @param username The username for the password. If it is null, it means
     *            password can't be saved.
     * @param password The password
     */
    public void setHttpAuthUsernamePassword(String host, String realm,
                                            String username,
                                            String password) {
        WebView w = mController.getCurrentTopWebView();
        if (w != null) {
            w.setHttpAuthUsernamePassword(host, realm, username, password);
        }
    }

    /**
     * Displays a page-info dialog.
     * @param tab The tab to show info about
     * @param fromShowSSLCertificateOnError The flag that indicates whether
     * this dialog was opened from the SSL-certificate-on-error dialog or
     * not. This is important, since we need to know whether to return to
     * the parent dialog or simply dismiss.
     * @param urlCertificateOnError The URL that invokes SSLCertificateError.
     * Null when fromShowSSLCertificateOnError is false.
     */
    void showPageInfo(final Tab tab,
            final boolean fromShowSSLCertificateOnError,
            final String urlCertificateOnError) {
        if (tab == null) return;
        final LayoutInflater factory = LayoutInflater.from(mContext);

        final View pageInfoView = factory.inflate(R.layout.page_info, null);

        final WebView view = tab.getWebView();

        String url = fromShowSSLCertificateOnError ? urlCertificateOnError : tab.getUrl();
        String title = tab.getTitle();

        if (url == null) {
            url = "";
        }
        if (title == null) {
            title = "";
        }

        ((TextView) pageInfoView.findViewById(R.id.address)).setText(url);
        ((TextView) pageInfoView.findViewById(R.id.title)).setText(title);

        mPageInfoView = tab;
        mPageInfoFromShowSSLCertificateOnError = fromShowSSLCertificateOnError;
        mUrlCertificateOnError = urlCertificateOnError;

        BrowserDialog dialog =new BrowserDialog(mContext){
            @Override
            public void onPositiveButtonClick() {
                super.onPositiveButtonClick();
                mPageInfoDialog = null;
                mPageInfoView = null;

                // if we came here from the SSL error dialog
                if (fromShowSSLCertificateOnError) {
                    // go back to the SSL error dialog
                    showSSLCertificateOnError(
                            mSSLCertificateOnErrorView,
                            mSSLCertificateOnErrorHandler,
                            mSSLCertificateOnErrorError);
                }
            }

            @Override
            public void onNeutralButtonClick() {
                super.onNeutralButtonClick();
                mPageInfoDialog = null;
                mPageInfoView = null;

                // if we came here from the SSL error dialog
                if (fromShowSSLCertificateOnError) {
                    // go back to the SSL error dialog
                    showSSLCertificateOnError(
                            mSSLCertificateOnErrorView,
                            mSSLCertificateOnErrorHandler,
                            mSSLCertificateOnErrorError);
                } else {
                    // otherwise, display the top-most certificate from
                    // the chain
                    showSSLCertificate(tab);
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                mPageInfoDialog = null;
                mPageInfoView = null;

                // if we came here from the SSL error dialog
                if (fromShowSSLCertificateOnError) {
                    // go back to the SSL error dialog
                    showSSLCertificateOnError(
                            mSSLCertificateOnErrorView,
                            mSSLCertificateOnErrorHandler,
                            mSSLCertificateOnErrorError);
                }
            }
        }.setBrowserTitle(R.string.page_info)
                .setBrowserContentView(pageInfoView)
                .setBrowserPositiveButton(R.string.ok);


        // if we have a main top-level page SSL certificate set or a certificate
        // error
        if (fromShowSSLCertificateOnError ||
                (view != null && view.getCertificate() != null)) {
            // add a 'View Certificate' button
            dialog.setBrowserNeutralButton(R.string.view_certificate);
        }
        mPageInfoDialog = dialog;
        dialog.show();
    }

    /**
     * Displays the main top-level page SSL certificate dialog
     * (accessible from the Page-Info dialog).
     * @param tab The tab to show certificate for.
     */
    private void showSSLCertificate(final Tab tab) {

        SslCertificate cert = tab.getWebView().getCertificate();
        if (cert == null) {
            return;
        }

        mSSLCertificateView = tab;
        mSSLCertificateDialog = createSslCertificateDialog(cert, tab.getSslCertificateError());
        mSSLCertificateDialog.setBrowserPositiveButton(R.string.ok).
                        setBrowserPositiveButtonListener(
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mSSLCertificateDialog = null;
                                mSSLCertificateView = null;

                                showPageInfo(tab, false, null);
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                mSSLCertificateDialog = null;
                                mSSLCertificateView = null;

                                showPageInfo(tab, false, null);
                            }
                        });
        mSSLCertificateDialog.show();
    }

    /**
     * Displays the SSL error certificate dialog.
     * @param view The target web-view.
     * @param handler The SSL error handler responsible for cancelling the
     * connection that resulted in an SSL error or proceeding per user request.
     * @param error The SSL error object.
     */
    void showSSLCertificateOnError(
            final WebView view, final SslErrorHandler handler,
            final SslError error) {

        SslCertificate cert = error.getCertificate();
        if (cert == null) {
            return;
        }

        mSSLCertificateOnErrorHandler = handler;
        mSSLCertificateOnErrorView = view;
        mSSLCertificateOnErrorError = error;
        mSSLCertificateOnErrorDialog = createSslCertificateDialog(cert, error);
        mSSLCertificateOnErrorDialog.setBrowserPositiveButton(R.string.ok)
                .setBrowserPositiveButtonListener(
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mSSLCertificateOnErrorDialog = null;
                                mSSLCertificateOnErrorView = null;
                                mSSLCertificateOnErrorHandler = null;
                                mSSLCertificateOnErrorError = null;

                                ((BrowserWebView) view).getWebViewClient().
                                        onReceivedSslError(view, handler, error);
                            }
                        })
                 .setBrowserNeutralButton(R.string.page_info_view)
                .setBrowserNeutralButtonListener(
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mSSLCertificateOnErrorDialog = null;

                                // do not clear the dialog state: we will
                                // need to show the dialog again once the
                                // user is done exploring the page-info details

                                showPageInfo(mController.getTabControl()
                                        .getTabFromView(view),
                                        true,
                                        error.getUrl());
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                mSSLCertificateOnErrorDialog = null;
                                mSSLCertificateOnErrorView = null;
                                mSSLCertificateOnErrorHandler = null;
                                mSSLCertificateOnErrorError = null;

                                ((BrowserWebView) view).getWebViewClient().
                                        onReceivedSslError(view, handler, error);
                            }
                        });
        mSSLCertificateOnErrorDialog.show();
    }

    /*
     * Creates an AlertDialog to display the given certificate. If error is
     * null, text is added to state that the certificae is valid and the icon
     * is set accordingly. If error is non-null, it must relate to the supplied
     * certificate. In this case, error is used to add text describing the
     * problems with the certificate and a different icon is used.
     */
    private BrowserDialog createSslCertificateDialog(SslCertificate certificate,
                                            SslError error) {
        View certificateView = null;//certificate.inflateCertificateView(mContext);
        try {
        	certificateView = (View)mInflateCertificateViewMehtod.invoke(certificate, mContext);
        } catch (Exception e) {
        	throw new RuntimeException("Invalid reflect", e);
        }
        if (certificateView == null) {
        	return null;
        }
        final LinearLayout placeholder =
                (LinearLayout)certificateView.findViewById(Resources.getSystem().getIdentifier("placeholder", "id", "android"));

        LayoutInflater factory = LayoutInflater.from(mContext);
        int iconId;

        if (error == null) {
            iconId = R.drawable.ic_dialog_browser_certificate_secure;
            LinearLayout table = (LinearLayout)factory.inflate(R.layout.ssl_success, placeholder);
            TextView successString = (TextView)table.findViewById(R.id.success);
            successString.setText(R.string.ssl_certificate_is_valid);
        } else {
            iconId = R.drawable.ic_dialog_browser_certificate_partially_secure;
            if (error.hasError(SslError.SSL_UNTRUSTED)) {
                addError(factory, placeholder, R.string.ssl_untrusted);
            }
            if (error.hasError(SslError.SSL_IDMISMATCH)) {
                addError(factory, placeholder, R.string.ssl_mismatch);
            }
            if (error.hasError(SslError.SSL_EXPIRED)) {
                addError(factory, placeholder, R.string.ssl_expired);
            }
            if (error.hasError(SslError.SSL_NOTYETVALID)) {
                addError(factory, placeholder, R.string.ssl_not_yet_valid);
            }
            if (error.hasError(SslError.SSL_DATE_INVALID)) {
                addError(factory, placeholder, R.string.ssl_date_invalid);
            }
            if (error.hasError(SslError.SSL_INVALID)) {
                addError(factory, placeholder, R.string.ssl_invalid);
            }
            // The SslError should always have at least one type of error and we
            // should explicitly handle every type of error it supports. We
            // therefore expect the condition below to never be hit. We use it
            // as as safety net in case a new error type is added to SslError
            // without the logic above being updated accordingly.
            if (placeholder.getChildCount() == 0) {
                addError(factory, placeholder, R.string.ssl_unknown);
            }
        }

        return new BrowserDialog(mContext)
                .setBrowserTitle(R.string.ssl_certificate)
                .setBrowserContentView(certificateView);
    }

    private void addError(LayoutInflater inflater, LinearLayout parent, int error) {
        TextView textView = (TextView) inflater.inflate(R.layout.ssl_warning,
                parent, false);
        textView.setText(error);
        parent.addView(textView);
    }
}
