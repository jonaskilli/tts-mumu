package com.github.jing332.tts.util

import cn.hutool.cache.impl.TimedCache
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractCachedManager<K, V>(timeout: Long, delay: Long) {
    protected val cache = TimedCache<K, V>(timeout, ConcurrentHashMap())

    init {
        cache.schedulePrune(delay)
        cache.setListener { k, v ->
            if (onCacheRemove(k, v))
                cache.put(k, v) // 隐患 迭代时修改
        }
    }

    /**
     * @return true: cache extend
     */
    open fun onCacheRemove(key: K, value: V): Boolean = false
}