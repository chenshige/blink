package com.blink.browser.preferences;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blink.browser.BrowserSettings;
import com.blink.browser.R;
import com.blink.browser.util.BuildUtil;
import com.blink.browser.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

public class BrowserFilePickerFragment extends BasePreferenceFragment implements View.OnClickListener {

    public static final String KEY = "key";

    private static final int TYPE_SDCARD = 0; //  SDCard 目录
    private static final int TYPE_FIRST = 1;//SDCard下的一级目录

    private TextView mCurrentPath;
    private TextView mSelectLocation;
    private TextView mOKButton;
    private RelativeLayout mSelect;
    private RecyclerView mRecyclerView;
    private FilePickerAdapter mAdapter;
    private ArrayList<SDCardInfo> mSDCardList = new ArrayList<>();
    private String mDownLoadPath;
    private String mInterimPath;
    private int mPathIndex = 0;

    private String mKey;

    private onFragmentCallBack mCallBack;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View rootView = inflater.inflate(R.layout.fragment_browser_file_picker, container, false);
        initView(rootView);
        initData();
        return rootView;
    }

    private void initView(View rootView) {
        mCurrentPath = (TextView) rootView.findViewById(R.id.current_path);
        mSelectLocation = (TextView) rootView.findViewById(R.id.select_location);
        mOKButton = (TextView) rootView.findViewById(R.id.ok);
        mSelect = (RelativeLayout) rootView.findViewById(R.id.select);
        mSelect.setVisibility(View.GONE);
        mSelect.setOnClickListener(this);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        mRecyclerView.addItemDecoration(new ItemDivider(getActivity(), R.drawable.recyclerview_divider));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void initData() {
        Bundle arguments = getArguments();
        mKey = arguments.getString(KEY);
        setBrowserActionBarTitle(getString(R.string.pref_download_path_setting_screen_title));
        mInterimPath = BrowserSettings.getInstance().getDownloadPath();
        mCurrentPath.setText(getResources().getString(R.string.default_location, mInterimPath));
        mSelectLocation.setText(R.string.select_location);
        ArrayList<SDCardInfo> arrayList = getSDCardList();
        if (arrayList == null || arrayList.size() == 0) {
            finish();
        }
        mSDCardList.addAll(arrayList);
        mAdapter = new FilePickerAdapter(getActivity(), arrayList);
        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<SDCardInfo> getSDCardList() {
        String[] SDCards = getVolumePaths(getActivity());
        if (SDCards != null && SDCards.length > 0) {
            ArrayList<SDCardInfo> arrayList = new ArrayList<SDCardInfo>();
            for (String SDCard : SDCards) {
                SDCardInfo info = getSDCardInfo(SDCard);
                if (info != null) {
                    arrayList.add(info);
                }
            }
            return arrayList;
        } else {
            return null;
        }
    }

    private void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            setBrowserActionBarTitle(title);
            mOKButton.setText(getString(R.string.select_download_path, title));
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private SDCardInfo getSDCardInfo(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        try {
            String totalSize;
            String freeSize;
            StatFs sf = new StatFs(path);
            Activity activity = getActivity();
            if (Build.VERSION.SDK_INT >= BuildUtil.VERSION_CODES.JELLY_BEAN_MR2) {
                long blockSize = sf.getBlockSizeLong();
                long BlockCount = sf.getBlockCountLong();
                if (BlockCount == 0) {
                    return null;
                }
                totalSize = Formatter.formatFileSize(activity, blockSize * BlockCount);
                freeSize = Formatter.formatFileSize(activity, blockSize * sf.getAvailableBlocksLong());
            } else {
                long blockSize = sf.getBlockSize();
                long BlockCount = sf.getBlockCount();
                if (BlockCount == 0) {
                    return null;
                }
                totalSize = Formatter.formatFileSize(activity, blockSize * BlockCount);
                freeSize = Formatter.formatFileSize(activity, blockSize * sf.getAvailableBlocks());
            }
            SDCardInfo info = new SDCardInfo();
            info.mPath = path;
            info.mTotalSize = totalSize;
            info.mFreeSize = freeSize;
            boolean isPhoneSDCard = Environment.getExternalStorageDirectory().getPath().equals(path);
            info.mIconRid = isPhoneSDCard ? R.drawable.ic_browser_setting_intenal_storage : R.drawable.ic_browser_setting_sdcards;
            return info;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * 获取手机存储卡列表
     *
     * @param activity
     * @return
     */
    public static String[] getVolumePaths(Activity activity) {
        StorageManager storageManager = null;
        Method methodGetPaths = null;
        if (activity != null) {
            storageManager = (StorageManager) activity
                    .getSystemService(Activity.STORAGE_SERVICE);
            try {
                methodGetPaths = storageManager.getClass()
                        .getMethod("getVolumePaths");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (storageManager == null || methodGetPaths == null) {
            return null;
        }

        String[] paths = null;
        try {
            paths = (String[]) methodGetPaths.invoke(storageManager);
        } catch (Exception e) {
        }
        return paths;
    }

    private void showFileList(String path) {
        ArrayList<SDCardInfo> arrayList = getFiles(new File(path));
        mAdapter.setData(arrayList);
        mAdapter.notifyDataSetChanged();
    }


    public void onItemClick(View view, SDCardInfo info) {
        if (info != null) {
            if (mPathIndex == 0
                    && !Environment.getExternalStorageDirectory().getAbsolutePath().equals(info.mPath)) {
                String appoint_path = FileUtils.getDefaultPath(getActivity(), info.mPath);
                if (!TextUtils.isEmpty(appoint_path)) {
                    mDownLoadPath = mInterimPath = appoint_path;
                    mCurrentPath.setText(String.format(getResources().getString(R.string.default_location),
                            mInterimPath));
                    BrowserSettings.getInstance().setDownloadPath(mDownLoadPath);
                }
                return;
            }
            mInterimPath = (mPathIndex == 0 ? "" : mInterimPath + File.separator) + info.mPath;
            showFileList(mInterimPath);
            mPathIndex++;
            if (mPathIndex > TYPE_SDCARD) {
                mCurrentPath.setVisibility(View.GONE);
                mSelect.setVisibility(View.VISIBLE);
                if (mPathIndex == TYPE_FIRST) {
                    setTitle(mInterimPath);
                } else {
                    setTitle(mInterimPath.substring(mInterimPath.lastIndexOf("/") + 1, mInterimPath.length()));
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.actionbar_left:
                onLeftClick();
                break;
            case R.id.select:
                mDownLoadPath = mInterimPath;
                BrowserSettings.getInstance().setDownloadPath(mDownLoadPath);
                back();
                break;
        }
    }

    private void onLeftClick() {
        if (mPathIndex == TYPE_SDCARD) {
            finish();
        } else if (mPathIndex == 1) {
            setTitle(getString(R.string.pref_download_path_setting_screen_title));
            mAdapter.setData(mSDCardList);
            mAdapter.notifyDataSetChanged();
            mInterimPath = null;
            mPathIndex--;
        } else if (mSDCardList != null && mSDCardList.size() > 0) {
            mInterimPath = mInterimPath.substring(0, mInterimPath.lastIndexOf("/"));
            showFileList(mInterimPath);
            mPathIndex--;
            if (mPathIndex == TYPE_FIRST) {
                setTitle(mInterimPath);
            } else {
                setTitle(mInterimPath.substring(mInterimPath.lastIndexOf("/") + 1, mInterimPath.length()));
            }
        }
        if (mPathIndex == TYPE_SDCARD) {
            mCurrentPath.setVisibility(View.VISIBLE);
            mSelect.setVisibility(View.GONE);
        } else {
            mSelect.setVisibility(View.VISIBLE);
        }
    }

    private void back() {
        if (mCallBack != null) {
            mCallBack.onFragmentCallBack(mKey, mDownLoadPath);
        }
        finish();
    }

    private ArrayList<SDCardInfo> getFiles(File root) {

        FileFilter ff = new FileFilter() {
            public boolean accept(File pathname) {
                return !pathname.isHidden();//过滤隐藏文件
            }
        };

        File files[] = root.listFiles(ff);
        ArrayList<SDCardInfo> arrayList = new ArrayList<>();
        ArrayList<String> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    String path = f.getPath();
                    path = path.substring(path.lastIndexOf("/") + 1, path.length());
                    list.add(path);
                }
            }
        }
        Collections.sort(list);

        if (list.size() > 0) {
            for (String path : list) {
                SDCardInfo info = new SDCardInfo();
                info.mPath = path;
                info.mIconRid = R.drawable.ic_browser_setting_folder;
                arrayList.add(info);
            }
        }
        return arrayList;
    }

    public void setOnFragmentCallBack(onFragmentCallBack callBack) {
        mCallBack = callBack;
    }

    private class ItemDivider extends RecyclerView.ItemDecoration {

        private Drawable mDrawable;

        public ItemDivider(Context context, int resId) {
            //在这里我们传入作为Divider的Drawable对象
            mDrawable = context.getResources().getDrawable(resId);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int marginLR = getResources().getDimensionPixelOffset(R.dimen.settings_list_divider_margin);
            final int left = marginLR;
            final int right = parent.getWidth() - parent.getPaddingRight() - marginLR;

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                //以下计算主要用来确定绘制的位置
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + mDrawable.getIntrinsicHeight();
                mDrawable.setBounds(left, top, right, bottom);
                mDrawable.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, int position, RecyclerView parent) {
            outRect.set(0, 0, 0, mDrawable.getIntrinsicWidth());
        }
    }

    private class FilePickerAdapter extends RecyclerView.Adapter<DownLoadPathViewHolder> {

        private ArrayList<SDCardInfo> mArrayList = new ArrayList<>();
        private Context mContext;

        public FilePickerAdapter(Context context, ArrayList<SDCardInfo> arrayList) {
            if (arrayList != null && arrayList.size() > 0) {
                mArrayList.addAll(arrayList);
            }
            this.mContext = context;
        }

        public void setData(ArrayList<SDCardInfo> arrayList) {
            if (arrayList != null) {
                mArrayList.clear();
                mArrayList.addAll(arrayList);
            }
        }

        @Override
        public DownLoadPathViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_picker_item, parent, false);
            DownLoadPathViewHolder holder = new DownLoadPathViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(v, (SDCardInfo) v.getTag());
                }
            });
            return holder;
        }


        @Override
        public void onBindViewHolder(DownLoadPathViewHolder holder, int position) {
            SDCardInfo info = mArrayList.get(position);
            holder.mIcon.setImageResource(info.mIconRid);
            holder.mContent.setText(info.mPath);
            if (TextUtils.isEmpty(info.mTotalSize)) {
                holder.mSize.setText(null);
                holder.mSize.setVisibility(View.GONE);
            } else {
                holder.mSize.setVisibility(View.VISIBLE);
                String desc = mContext.getResources().getString(R.string.sdcard_free, info.mFreeSize, info.mTotalSize);
                holder.mSize.setText(desc);
            }
            holder.itemView.setTag(info);
        }

        @Override
        public int getItemCount() {
            return mArrayList == null ? 0 : mArrayList.size();
        }

    }

    public class DownLoadPathViewHolder extends RecyclerView.ViewHolder {
        private ImageView mIcon;
        private TextView mContent;
        private TextView mSize;

        public DownLoadPathViewHolder(View itemView) {
            super(itemView);
            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mContent = (TextView) itemView.findViewById(R.id.content);
            mSize = (TextView) itemView.findViewById(R.id.size);
        }
    }

    private class SDCardInfo {
        private int mIconRid;
        private String mPath;
        private String mTotalSize;
        private String mFreeSize;
    }
}
