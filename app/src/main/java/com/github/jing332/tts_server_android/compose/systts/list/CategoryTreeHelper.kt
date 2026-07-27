package com.github.jing332.tts_server_android.compose.systts.list

import com.github.jing332.database.entities.systts.SystemTtsV2

/**
 * 子分组树节点
 */
data class SubCategoryNode(
    val name: String,
    val fullPath: String,
    val level: Int,
    val items: List<SystemTtsV2> = emptyList(),
    val children: List<SubCategoryNode> = emptyList()
)

/**
 * 扁平化的渲染项
 */
sealed class FlattenedCategoryItem {
    data class SubGroupHeader(val node: SubCategoryNode) : FlattenedCategoryItem()
    data class TtsItem(
        val item: SystemTtsV2,
        val displayLevel: Int,
        val categoryPath: String
    ) : FlattenedCategoryItem()
}

/**
 * 将音色列表按 categoryPath 构建为子分组树
 */
fun buildSubCategoryTree(items: List<SystemTtsV2>): SubCategoryNode {
    val sortedItems = items.sortedBy { it.order }
    val rootItems = mutableListOf<SystemTtsV2>()
    val childMap = mutableMapOf<String, MutableList<SystemTtsV2>>()

    for (item in sortedItems) {
        if (item.categoryPath.isBlank()) {
            rootItems.add(item)
        } else {
            val firstSlash = item.categoryPath.indexOf('/')
            val key = if (firstSlash == -1) item.categoryPath else item.categoryPath.substring(0, firstSlash)
            childMap.getOrPut(key) { mutableListOf() }.add(item)
        }
    }

    val children = childMap.map { (name, list) ->
        buildNode(name, name, 0, list)
    }.sortedBy { node -> node.items.minOfOrNull { it.order } ?: Int.MAX_VALUE }

    return SubCategoryNode("", "", -1, rootItems.sortedBy { it.order }, children)
}

private fun buildNode(name: String, fullPath: String, level: Int, items: List<SystemTtsV2>): SubCategoryNode {
    val sortedItems = items.sortedBy { it.order }
    val directItems = mutableListOf<SystemTtsV2>()
    val childMap = mutableMapOf<String, MutableList<SystemTtsV2>>()

    val prefix = if (fullPath.isEmpty()) "" else "$fullPath/"
    for (item in sortedItems) {
        if (item.categoryPath == fullPath) {
            directItems.add(item)
        } else if (item.categoryPath.startsWith(prefix)) {
            val remaining = item.categoryPath.removePrefix(prefix)
            val firstSlash = remaining.indexOf('/')
            val key = if (firstSlash == -1) remaining else remaining.substring(0, firstSlash)
            childMap.getOrPut(key) { mutableListOf() }.add(item)
        }
    }

    val children = childMap.map { (childName, list) ->
        val childFullPath = if (fullPath.isEmpty()) childName else "$fullPath/$childName"
        buildNode(childName, childFullPath, level + 1, list)
    }.sortedBy { node -> node.items.minOfOrNull { it.order } ?: Int.MAX_VALUE }

    return SubCategoryNode(name, fullPath, level, directItems.sortedBy { it.order }, children)
}

/**
 * 将子分组树扁平化为渲染列表
 */
fun flattenSubCategoryTree(node: SubCategoryNode): List<FlattenedCategoryItem> {
    val result = mutableListOf<FlattenedCategoryItem>()
    val currentPath = node.fullPath
    // 子分组显示在前面，单一配置显示在后面
    for (child in node.children) {
        result.add(FlattenedCategoryItem.SubGroupHeader(child))
        result.addAll(flattenSubCategoryTree(child))
    }
    for (item in node.items.sortedBy { it.order }) {
        result.add(FlattenedCategoryItem.TtsItem(item, node.level + 1, currentPath))
    }
    return result
}
