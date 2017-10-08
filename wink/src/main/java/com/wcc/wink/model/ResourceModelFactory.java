package com.wcc.wink.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/** 资源对象工厂类，负责创建资源持久化对象
 * Created by wenbiao.xie on 2016/6/16.
 */
public interface ResourceModelFactory<K, E> {
    /**
     * 数据库创建时回调，这里需要明确的是并不是数据库创建时回调该方法，而是涉及到对应的资源对象
     * 需要进行创建相关表时才回调，下面的onUpgrade方法也是如此
     * @param db 数据库对象
     */
    void onCreate(SQLiteDatabase db);

    /**
     * 数据库升级时回调，解释同上
     * @param db 数据库对象
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);


    /**
     * 数据库打开时回调，目前暂未引入数据库中，留待后续版本扩展
     * @param db 数据库对象
     */
    void onOpen(SQLiteDatabase db);

    /**
     * 创建资源持久化控制对象，用于下载时持久化相应对象
     *
     * 一般来说，资源实体对于下载本身是透明的，它的内容如何持久化需要对应的持久化控制对象才能进行
     * 对于扩展引入的实体需要在{@link GenericModelFactory} 中注册相应的工厂类
     *
     * @param context 上下文对象
     * @param factories 通用资源持久化仓库对象
     * @return 返回创建成功的持久化控制对象
     */
    ModelDao<K, E> build(Context context, GenericModelFactory factories);

    /**
     * 销毁该工厂对象
     * 当该工厂对象要被取代时，将被调用。目前暂未引入流程中，留待后续版本扩展
     */
    void teardown();
}
