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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.FileChooserParams;

import com.blink.browser.util.ToastUtil;

import java.io.File;

//import android.support.v4.content.FileProvider;

/**
 * Handle the file upload. This does not support selecting multiple files yet.
 */
public class UploadHandler {
    private final static String IMAGE_MIME_TYPE = "image/*";
    private final static String VIDEO_MIME_TYPE = "video/*";
    private final static String AUDIO_MIME_TYPE = "audio/*";

    private final static String FILE_PROVIDER_AUTHORITY = "com.android.browser-classic.file";

    /*
     * The Object used to inform the WebView of the file to upload.
     */
    private ValueCallback<Uri[]> mUploadMessage;
    private ValueCallback<Uri> mOldUploadMessage;

    private boolean mHandled;
    private Controller mController;
    private FileChooserParams mParams;
    private Uri mCapturedMedia;

    public UploadHandler(Controller controller) {
        mController = controller;
    }

    boolean handled() {
        return mHandled;
    }

    void onResult(int resultCode, Intent intent) {
        Uri[] uris;
        // As the media capture is always supported, we can't use
        // FileChooserParams.parseResult().
        uris = parseResult(resultCode, intent);
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(uris);
        }
        if (mOldUploadMessage != null) {
            if (uris != null) {
                mOldUploadMessage.onReceiveValue(uris[0]);
            } else {
                mOldUploadMessage.onReceiveValue(null);
            }
        }
        mHandled = true;
    }

    void openFileChooser(ValueCallback<Uri[]> callback, FileChooserParams fileChooserParams) {

        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }

        mUploadMessage = callback;
        mParams = fileChooserParams;
        Intent[] captureIntents = createCaptureIntent(mParams.getAcceptTypes());
        assert (captureIntents != null && captureIntents.length > 0);
        Intent intent = null;
        // Go to the media capture directly if capture is specified, this is the
        // preferred way.
        if (fileChooserParams.isCaptureEnabled() && captureIntents.length == 1) {
            intent = captureIntents[0];
        } else {
            intent = new Intent(Intent.ACTION_CHOOSER);
            intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, captureIntents);
            intent.putExtra(Intent.EXTRA_INTENT, fileChooserParams.createIntent());
        }
        startActivity(intent);
    }

    void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }

        mOldUploadMessage = uploadMsg;
        String[] acceptTypes = new String[1];
        acceptTypes[0] = acceptType;
        Intent[] captureIntents = createCaptureIntent(acceptTypes);
        assert (captureIntents != null && captureIntents.length > 0);
        Intent intent = null;
        // Go to the media capture directly if capture is specified, this is the
        // preferred way.
        if (!TextUtils.isEmpty(capture) && captureIntents.length == 1) {
            intent = captureIntents[0];
        } else {
            intent = new Intent(Intent.ACTION_CHOOSER);
            intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, captureIntents);
            String mimeType = "*/*";
            if (acceptType != null && !acceptType.trim().isEmpty()) {
                mimeType = acceptType.split(";")[0];
            }

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(mimeType);
            intent.putExtra(Intent.EXTRA_INTENT, i);
        }
        startActivity(intent);
    }

    private Uri[] parseResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return null;
        }
        Uri result = intent == null || resultCode != Activity.RESULT_OK ? null
                : intent.getData();

        // As we ask the camera to save the result of the user taking
        // a picture, the camera application does not return anything other
        // than RESULT_OK. So we need to check whether the file we expected
        // was written to disk in the in the case that we
        // did not get an intent returned but did get a RESULT_OK. If it was,
        // we assume that this result has came back from the camera.
        if (result == null && intent == null && resultCode == Activity.RESULT_OK
                && mCapturedMedia != null) {
            result = mCapturedMedia;
        }

        Uri[] uris = null;
        if (result != null) {
            uris = new Uri[1];
            uris[0] = result;
        }
        return uris;
    }

    private void startActivity(Intent intent) {
        try {
            mController.getActivity().startActivityForResult(intent, Controller.FILE_SELECTED);
        } catch (ActivityNotFoundException e) {
            // No installed app was able to handle the intent that
            // we sent, so file upload is effectively disabled.
            ToastUtil.showLongToast(mController.getActivity(), R.string.uploads_disabled);
        }
    }

    private Intent[] createCaptureIntent(String[] acceptStrings) {
        String mimeType = "*/*";
        String[] acceptTypes = acceptStrings;
        if (acceptTypes != null && acceptTypes.length > 0) {
            mimeType = acceptTypes[0];
        }
        Intent[] intents;
        if (mimeType.equals(IMAGE_MIME_TYPE)) {
            intents = new Intent[1];
            intents[0] = createCameraIntent(createTempFileContentUri(".jpg"));
        } else if (mimeType.equals(VIDEO_MIME_TYPE)) {
            intents = new Intent[1];
            intents[0] = createCamcorderIntent();
        } else if (mimeType.equals(AUDIO_MIME_TYPE)) {
            intents = new Intent[1];
            intents[0] = createSoundRecorderIntent();
        } else {
            intents = new Intent[3];
            intents[0] = createCameraIntent(createTempFileContentUri(".jpg"));
            intents[1] = createCamcorderIntent();
            intents[2] = createSoundRecorderIntent();
        }
        return intents;
    }

    private Uri createTempFileContentUri(String suffix) {
        try {
/*            File mediaPath = new File(mController.getActivity().getFilesDir(), "captured_media");
            if (!mediaPath.exists() && !mediaPath.mkdir()) {
                throw new RuntimeException("Folder cannot be created.");
            }
            File mediaFile = File.createTempFile(
                    String.valueOf(System.currentTimeMillis()), suffix, mediaPath);
            return FileProvider.getUriForFile(mController.getActivity(),
                    FILE_PROVIDER_AUTHORITY, mediaFile);*/

            File externalDataDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM);
            File cameraDataDir = new File(externalDataDir.getAbsolutePath() +
                    File.separator + "captured_media");
            cameraDataDir.mkdirs();
            String mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator +
                    System.currentTimeMillis() + "." + suffix;
            return Uri.fromFile(new File(mCameraFilePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Intent createCameraIntent(Uri contentUri) {
        if (contentUri == null) throw new IllegalArgumentException();
        mCapturedMedia = contentUri;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedMedia);
        intent.setClipData(ClipData.newUri(mController.getActivity().getContentResolver(),
                FILE_PROVIDER_AUTHORITY, mCapturedMedia));
        return intent;
    }

    private Intent createCamcorderIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    private Intent createSoundRecorderIntent() {
        return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    }
}
