// Copyright 2017 The Blink Browser. All rights reserved.

package com.blink.browser.adblock;

import android.content.Context;
import android.text.TextUtils;

import com.blink.browser.UrlUtils;
import com.blink.browser.network.Network;
import com.blink.browser.network.NetworkServices;
import com.blink.browser.network.UpdateHandler;
import com.blink.browser.util.FileUtils;
import com.blink.browser.util.SharedPreferencesUtils;
import com.blink.browser.util.WebAddress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AdBlockDataUpdator {
    private static final String TAG = AdBlockDataUpdator.class.getSimpleName();

    private static final String ASSETS_ZIP_NAME = "adblock.zip";
    private static final String ASSETS_UNZIP_DIR = "/data/adblock";
    private static final String ADBLOCK_RULE_FILE = "blink_rules.txt";

    private static final String CUSTOM_RULES_FILE_NAME = "custom_rules.txt";
    private static final String DISABLE_JS_AD_HOST_LIST_FILE_NAME = "js_disable_hosts.txt";

    private static final long UPDATE_INTERVAL = 1000 * 60 * 60 * 24; // 24h

    private static final int AD_RULE_DEL = 0;
    private static final int AD_RULE_ADD = 1;

    private List<String> mJsDisableHosts;

    private static AdBlockDataUpdator sInstance = null;

    private String mAdBlockDirPath;
    private File mUnzipAdBlockFile;
    private Context mContext;

    public static AdBlockDataUpdator getInstance() {
        if (sInstance == null) {
            sInstance = new AdBlockDataUpdator();
        }
        return sInstance;
    }

    private AdBlockDataUpdator() {
    }

    public void init(final Context context) {
        if (context == null) return;
        mContext = context;

        mAdBlockDirPath = mContext.getFilesDir() + ASSETS_UNZIP_DIR;
        final File parentFile = new File(mAdBlockDirPath);
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        mUnzipAdBlockFile = new File(parentFile, ADBLOCK_RULE_FILE);
        if (!mUnzipAdBlockFile.exists()) {
            // TODO(jinsong): This case local adblockrules doesn't exit,
            // Whether should update it from server immediately after copy from asset?
            try {
                mUnzipAdBlockFile.createNewFile();
            } catch (IOException e) {
            }
            // TODO(jinsong): Now we don't have adblock.zip default.
            //copyAssetAndUnzip();
        }

        initLocalFiles();
    }

    public String getAdblockDirPath() {
        return mAdBlockDirPath;
    }

    public String getAdBlockRulesPath(String fileName) {
        return mAdBlockDirPath + "/" + fileName;
    }

    public String getCustomRulesFilePath() {
        return mAdBlockDirPath + "/" + CUSTOM_RULES_FILE_NAME;
    }

    public String getDisableJsAdHostsPath() {
        return mAdBlockDirPath + "/" + DISABLE_JS_AD_HOST_LIST_FILE_NAME;
    }

    private void initLocalFiles() {
        FileUtils.createFileIfNotExists(getCustomRulesFilePath());
        FileUtils.createFileIfNotExists(getDisableJsAdHostsPath());
    }

    public void writeCustomRules(List<String> rules) {
        String customRulesFilePath = getCustomRulesFilePath();
        FileUtils.deleteFile(customRulesFilePath);
        FileUtils.writeByLines(rules, customRulesFilePath, false);
    }

    public void appendAdMarkRules(List<String> rules) {
        if (rules == null || rules.size() <= 0) {
            return;
        }
        String adMarkRulesFilePath = getCustomRulesFilePath();
        List<String> adsDataList = AdBlockDataUpdator.getInstance().readCustomRules();
        if (adsDataList != null && !adsDataList.contains(rules.get(0))) {
            FileUtils.writeByLines(rules, adMarkRulesFilePath, true);
        }
    }

    public List<String> readCustomRules() {
        return FileUtils.readByLine(getCustomRulesFilePath());
    }

    public synchronized void writeDisableJsHosts(List<String> host) {
        mJsDisableHosts = host;
        String disableJsAdHostsPath = getDisableJsAdHostsPath();
        FileUtils.deleteFile(disableJsAdHostsPath);
        FileUtils.writeByLines(host, disableJsAdHostsPath, false);
    }

    public void addNewDisableJsHost(String urlToDisableJs) {
        String hostToDisableJs = null;
        try {
            WebAddress address = new WebAddress(urlToDisableJs);
            hostToDisableJs = address.getHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> hostsDisableJs = readDisableJsHosts();
        if (!TextUtils.isEmpty(hostToDisableJs) && !hostsDisableJs.contains(hostToDisableJs)) {
            hostsDisableJs.add(hostToDisableJs);
            String disableJsAdHostsPath = getDisableJsAdHostsPath();
            FileUtils.writeByLine(hostToDisableJs, disableJsAdHostsPath, true);
        }
    }

    public void removeDisableJsHost(String urlToRemove) {
        List<String> hostsDisableJs = readDisableJsHosts();
        List<String> hostsNeedDelete = new ArrayList<>();
        for (String host : hostsDisableJs) {
            if (UrlUtils.checkUrlBelongToHost(urlToRemove, host)) {
                hostsNeedDelete.add(host);
            }
        }
        hostsDisableJs.removeAll(hostsNeedDelete);
        writeDisableJsHosts(hostsDisableJs);
    }

    public synchronized List<String> readDisableJsHosts() {
        if (mJsDisableHosts == null) {
            mJsDisableHosts = FileUtils.readByLine(getDisableJsAdHostsPath());
        }
        return mJsDisableHosts;
    }

    public void updateServices(String updateUrl, String timestamp, long delay) {
        NetworkServices.getInstance().updateServices(
                sInstance.new AdBlockUpdateHandler(mContext, updateUrl, timestamp, delay));
    }

    private class AdBlockUpdateHandler extends UpdateHandler {
        String mUpdateUrl = "";
        String mTimeStamp = "";

        public AdBlockUpdateHandler(Context context, String updateUrl, String timestamp, long delay) {
            super(context);
            mUpdateUrl = updateUrl;
            mTimeStamp = timestamp;
            setDelay(delay);
        }

        @Override
        public boolean checkUpdate() {
            if (TextUtils.isEmpty(mUpdateUrl) || TextUtils.isEmpty(mTimeStamp)) {
                return false;
            }
            long lastUpdate = (long)SharedPreferencesUtils.get(mTimeStamp, 0L);
            return (lastUpdate == 0L || lastUpdate + UPDATE_INTERVAL > System.currentTimeMillis())
                    && super.checkUpdate();
        }

        @Override
        public void doUpdateSuccess(String response) {
            if (TextUtils.isEmpty(response)) return;
            try {
                String[] separated = response.split("\n");
                List<String> rules = new ArrayList<String>();
                for (int i = 0; i < separated.length; i++) {
                    rules.add(separated[i]);
                }
                String filePath = mAdBlockDirPath + "/" + FileUtils.getFileNameFromUrl(mUpdateUrl);
                FileUtils.deleteFile(filePath);
                FileUtils.writeByLines(rules, filePath, false);
                SharedPreferencesUtils.put(mTimeStamp, System.currentTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String doUpdateNow() {
            try {
                return Network.doHttpGet(mContext, mUpdateUrl, getParams(), false);
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private void updateRules(final JSONArray ruleJson) {
        try {
            // The set is for saved.
            HashSet<String> savedSet = new HashSet<String>();
            // The set for deleted.
            HashSet<String> deletedSet = new HashSet<String>();
            // Read JSON data.
            readJSONData(ruleJson, savedSet, deletedSet);
            // Read local data.
            readLocalData(savedSet, deletedSet);

            // Save rules to local.
            saveRules(savedSet);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized void readLocalData(HashSet<String> savedSet, HashSet<String> deletedSet) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(mUnzipAdBlockFile));
            String rule;
            while ((rule = reader.readLine()) != null && !deletedSet.contains(rule)) {
                savedSet.add(rule);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void readJSONData(JSONArray jsonArray, HashSet<String> savedSet, HashSet<String> deletedSet) throws JSONException {
        for (int i = 0; i < jsonArray.length() && jsonArray != null; i++) {
            JSONObject object = (JSONObject) jsonArray.get(i);
            String rule = object.optString("rule");
            int flag = object.optInt("flag");
            if (flag == AD_RULE_ADD) {
                savedSet.add(rule);
            } else if (flag == AD_RULE_DEL) {
                deletedSet.add(rule);
            }
        }
    }

    private synchronized void saveRules(HashSet<String> savedSet) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(mUnzipAdBlockFile, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            Iterator iter = savedSet.iterator();
            while (iter.hasNext()) {
                out.println(iter.next());
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void copyAssetAndUnzip() {
        File zipOutputFile = null;
        try {
            String zipSourceFilePath = mAdBlockDirPath + File.separator + ASSETS_ZIP_NAME;
            zipOutputFile = new File(zipSourceFilePath);
            FileUtils.copyFile(mContext.getAssets().open(ASSETS_ZIP_NAME), new FileOutputStream(zipOutputFile));
            FileUtils.unZipFile(zipSourceFilePath, mAdBlockDirPath);
            FileUtils.deleteFile(zipSourceFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zipOutputFile != null) {
                    zipOutputFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
