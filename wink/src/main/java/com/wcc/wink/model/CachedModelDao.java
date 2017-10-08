package com.wcc.wink.model;

import com.wcc.wink.util.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存数据操作模型
 * Created by wenbiao.xie on 2016/6/16.
 */
public class CachedModelDao<K, E> implements ModelDao<K, E> {

    final ModelDao<K, E> proxy;
    final Map<K, E> caches = new HashMap<>();
    final KeyGenerator<K, E> keyGenerator;

    public CachedModelDao(ModelDao<K, E> target, KeyGenerator<K, E> generator) {
        this.proxy = target;
        this.keyGenerator = generator;
    }

    @Override
    public void init() throws Exception {
        proxy.init();
        Collection<E> results = proxy.all();
        if (!Utils.isEmpty(results)) {
            for ( E e: results) {
                caches.put(keyGenerator.key(e), e);
            }
        }
    }

    public boolean exists(K key) {
        return caches.containsKey(key);
    }

    @Override
    public Collection<E> all() {
        return caches.values();
    }

    @Override
    public E get(K key) {
        return caches.get(key);
    }

    @Override
    public void delete(E entity) {
        proxy.delete(entity);
        caches.remove(keyGenerator.key(entity));
    }

    @Override
    public void deleteAll(List<E> entities) {
        proxy.deleteAll(entities);

        if (!Utils.isEmpty(entities)) {
            for ( E e: entities) {
                caches.remove(keyGenerator.key(e));
            }
        }
    }

    @Override
    public void saveOrUpdate(E t) {
        proxy.saveOrUpdate(t);
        K key = keyGenerator.key(t);
        caches.put(key, t);
    }

    @Override
    public void saveOrUpdateAll(List<E> entities) {
        proxy.saveOrUpdateAll(entities);
        if (!Utils.isEmpty(entities)) {
            for ( E t: entities) {
                K key = keyGenerator.key(t);
                caches.put(key, t);
            }
        }
    }
}
