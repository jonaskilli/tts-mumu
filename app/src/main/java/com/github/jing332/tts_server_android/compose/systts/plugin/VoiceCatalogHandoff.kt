package com.github.jing332.tts_server_android.compose.systts.plugin

/**
 * 音色广场 → 分类入库 的跨 Activity 交接（同进程内存单例）。
 *
 * 背景（10-10 用户：音色大厅和声音列表关系要弄清楚、广场没衔接入库）：
 * 广场（找音色）勾选后原本只补进当前配置的声音下拉，批量入库没有路——
 * 分类入库只认插件分类池。本单例让「广场勾选的音色」能带去插件管理页那条入库链。
 *
 * 链路：编辑页广场「入库(N)」→ put(pluginId, items) + startActivity(PluginManagerActivity)
 * → 列表读到非空 handoff 的插件卡片自动弹 ImportByCategoryDialog，弹窗跳过勾池子、
 * 直接以 handoff 音色进声音列表阶段（分类=广场标签映射，试听可改）→ 消费即清。
 *
 * 不落盘、不进备份：一次性交接单，进程死即消失（数据源头在插件官网，丢了重搜即可）。
 */
object VoiceCatalogHandoff {
    data class Item(
        val voiceId: String,
        val voiceName: String,
        /** 广场卡片的标签原文（如「女声」「治愈」），入库侧做字面映射，映射不上不打标签 */
        val tag: String? = null,
    )

    private var pending: Pair<String, List<Item>>? = null

    fun put(pluginId: String, items: List<Item>) {
        pending = pluginId to items
    }

    /** 只看不动（列表卡片判断要不要自动弹入库弹窗用）；消费走 [take] */
    fun peek(pluginId: String): Boolean = pending?.first == pluginId

    /** 取走该插件的待入库清单（取即清；仅当待处理插件与请求方一致时返回） */
    fun take(pluginId: String): List<Item>? {
        val p = pending ?: return null
        if (p.first != pluginId) return null
        pending = null
        return p.second
    }
}
