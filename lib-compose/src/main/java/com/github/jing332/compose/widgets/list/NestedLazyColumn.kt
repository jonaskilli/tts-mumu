package com.github.jing332.compose.widgets.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

@Preview
@Composable
private fun PreviewNestedList() {
    MaterialTheme {
        val list: List<Element> = remember {
            mutableStateListOf<Element>().apply {
                add(GroupElement(true, "group1", children = mutableStateListOf(ItemElement("1233"))))
            }
        }
        NestedLazyColumn(
            list = list,
            onListChange = {},
            headerContent = {
                Text("group")
            },
            itemContent = {
                Text("hello")
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedLazyColumn(
    modifier: Modifier = Modifier,
    list: List<Element>, // 外部传入的列表
    onListChange: (List<Element>) -> Unit, // 列表更改后的回调
    headerContent: @Composable BoxScope.(GroupElement) -> Unit,
    itemContent: @Composable BoxScope.(ItemElement) -> Unit
) {
    val state = rememberLazyListState()
    // 内部维护的 cacheList，用于拖动时的实时更新
    val (cacheList, setCacheList) = remember { mutableStateOf(list) }

    val reorderState = rememberReorderableLazyListState(
        listState = state,
        onMove = { from, to ->
            val fromKey = from.key ?: return@rememberReorderableLazyListState
            val toKey = to.key ?: return@rememberReorderableLazyListState
            // 更新 cacheList，实时响应 UI 变化
            val newList = updateNestedList(cacheList, fromKey, toKey)
            setCacheList(newList)
        },
        onDragEnd = { _, _ ->
            // 拖动结束后，调用 onListChange，通知外部状态
            onListChange(cacheList)
        }
    )

    fun LazyListScope.listContent(elements: List<Element>) {
        elements.forEach { element ->
            when (element) {
                is GroupElement -> {
                    stickyHeader(key = element.key) {
                        ReorderableItem(reorderableState = reorderState, key = element.key) {
                            headerContent(element)
                        }
                    }
                    listContent(element.children)
                }

                is ItemElement -> {
                    item(key = element.key) {
                        ReorderableItem(reorderableState = reorderState, key = element.key) {
                            itemContent(element)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(modifier = modifier.detectReorderAfterLongPress(reorderState), state = state) {
        // 使用 cacheList 进行渲染
        listContent(cacheList)
    }
}

// updateNestedList 和 findElementAndParent 函数与之前版本保持一致。
fun updateNestedList(list: List<Element>, fromKey: Any, toKey: Any): List<Element> {
    // 使用 toMutableList() 创建一个可变的副本，避免修改原始列表
    val newList = list.toMutableList()

    // 查找 from 和 to 元素及其父元素
    val (fromElement, fromParent) = findElementAndParent(newList, fromKey)
    val (toElement, toParent) = findElementAndParent(newList, toKey)

    if (fromElement == null || toElement == null) {
        return newList // 如果找不到元素，则返回原始列表
    }

    // 从原位置移除元素
    if (fromParent != null) {
        fromParent.children.remove(fromElement)
    } else {
        newList.remove(fromElement)
    }

    // 将元素插入到新位置
    if (toParent != null) {
        val toIndex = toParent.children.indexOf(toElement)
        toParent.children.add(toIndex, fromElement)
    } else {
        val toIndex = newList.indexOf(toElement)
        newList.add(toIndex, fromElement)
    }

    return newList
}

// 辅助函数，用于查找元素及其父元素
fun findElementAndParent(list: List<Element>, key: Any): Pair<Element?, GroupElement?> {
    fun find(elements: List<Element>, parent: GroupElement? = null): Pair<Element?, GroupElement?> {
        for (element in elements) {
            if (element.key == key) {
                return Pair(element, parent)
            }
            if (element is GroupElement) {
                val result = find(element.children, element)
                if (result.first != null) {
                    return result
                }
            }
        }
        return Pair(null, null)
    }
    return find(list)
}
