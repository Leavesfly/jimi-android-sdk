package io.leavesfly.jimi.android.llm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 缓存
 * 使用 LRU 策略缓存已创建的 LLM 实例，避免重复创建
 */
public class LLMCache {

    private static final int DEFAULT_MAX_SIZE = 5;

    private final int maxSize;
    private final Map<String, LLM> cache;

    public LLMCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public LLMCache(int maxSize) {
        this.maxSize = maxSize;
        // 使用 LinkedHashMap 实现 LRU
        this.cache = new LinkedHashMap<String, LLM>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, LLM> eldest) {
                boolean shouldRemove = size() > LLMCache.this.maxSize;
                if (shouldRemove) {
                    // 关闭被移除的 LLM 实例
                    eldest.getValue().shutdown();
                }
                return shouldRemove;
            }
        };
    }

    /**
     * 获取或创建 LLM 实例
     *
     * @param modelName 模型名称（作为缓存 key）
     * @param factory   创建工厂
     * @return LLM 实例
     */
    public synchronized LLM getOrCreate(String modelName, LLMCreator factory) {
        LLM llm = cache.get(modelName);
        if (llm == null) {
            llm = factory.create(modelName);
            cache.put(modelName, llm);
        }
        return llm;
    }

    /**
     * 从缓存中获取 LLM 实例
     *
     * @param modelName 模型名称
     * @return LLM 实例，如果不存在返回 null
     */
    public synchronized LLM get(String modelName) {
        return cache.get(modelName);
    }

    /**
     * 将 LLM 实例放入缓存
     *
     * @param modelName 模型名称
     * @param llm       LLM 实例
     */
    public synchronized void put(String modelName, LLM llm) {
        LLM old = cache.put(modelName, llm);
        if (old != null && old != llm) {
            old.shutdown();
        }
    }

    /**
     * 从缓存中移除 LLM 实例
     *
     * @param modelName 模型名称
     */
    public synchronized void remove(String modelName) {
        LLM llm = cache.remove(modelName);
        if (llm != null) {
            llm.shutdown();
        }
    }

    /**
     * 清空缓存并关闭所有 LLM 实例
     */
    public synchronized void clear() {
        for (LLM llm : cache.values()) {
            llm.shutdown();
        }
        cache.clear();
    }

    /**
     * 获取缓存大小
     */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * LLM 创建器接口
     */
    public interface LLMCreator {
        LLM create(String modelName);
    }
}
