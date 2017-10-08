package com.wcc.wink.model;

import java.util.Collection;
import java.util.List;

/**
 * Created by wenbiao.xie on 2016/6/16.
 */
public interface ModelDao<K, E> {
    /**
     * 初始化数据访问对象
     * @throws Exception 可能会抛出IOException与数据库相关的异常
     */
    void init() throws Exception;

    /**
     * 获取存储中所有的资源对象列表
     * @return 返回数据库存储的所有对象
     */
    Collection<E> all();

    /**
     * 根据指定关键字查询相应的资源对象实体
     * @param key 资源关键字
     * @return 返回查找到的资源对象，如果没有找到将返回null
     */
    E get(K key);

    /**
     * 从存储中删除指定资源实体
     * @param entity 资源对象
     */
    void delete(E entity);

    /**
     * 从存储中删除指定的资源实体列表
     * @param entities 资源对象列表
     */
    void deleteAll(List<E> entities);

    /**
     * 将指定资源实体更新存储到数据库中
     * @param t 资源对象
     */
    void saveOrUpdate(E t);

    /**
     * 将指定资源实体列表全部更新存储到数据库中
     * @param entities 资源对象列表
     */
    void saveOrUpdateAll(List<E> entities);
}
