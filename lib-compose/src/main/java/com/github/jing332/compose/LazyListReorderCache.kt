package com.github.jing332.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Collections

class LazyListReorderCache<T : Any>(private val source: List<T>) {
    var list: List<T> by mutableStateOf(source)

    var isDragging by mutableStateOf(false)
    fun move(from: Int, to: Int) {
        isDragging = true

        list = list.toMutableList().apply {
            Collections.swap(this, from, to)
        }
    }

    fun ended() {
        isDragging = false
    }
}

@Composable
fun <T : Any> rememberLazyListReorderCache(
    source: List<T>
): LazyListReorderCache<T> {
    return remember {
        LazyListReorderCache(source)
    }.apply {
        LaunchedEffect(source) {
            if (!isDragging)
                this@apply.list = source
        }
    }
}

class ListReorderCache<T : Any>(initial: List<T>) {
    var canUpdate by mutableStateOf(false)
    var list: List<T> by mutableStateOf(initial)
}

@Composable
fun <T : Any> cacheUpdater(source: List<T>): ListReorderCache<T> {
    return remember {
        ListReorderCache(source)
    }.apply {
        LaunchedEffect(source) {
            if (!canUpdate)
                this@apply.list = source
        }
    }
}

data class GroupListKey(val itemKey: Any? = null, val groupKey: Any? = null){

}