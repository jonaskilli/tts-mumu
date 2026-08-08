package com.github.jing332.tts_server_android.compose.systts.list.ui

abstract class ItemDescriptor {
    open val name: String = "name"
    open val desc: String = "desc"
    open val bottom: String = "bottom"
    open val type: String = "type"
    open val tagName: String = "tag"
    open val standby: Boolean = false

    companion object {
        /**
         * 简化 tagName 显示：去掉【...】括号，只保留标签。
         * 【女/女青年01】 → 女青年01
         * 旁白 → 旁白（无括号的不动）
         */
        fun formatTagName(raw: String): String {
            if (raw.isBlank()) return raw
            // 匹配【...】xxx 或 【...】
            val bracketMatch = Regex("^【(.+?)】.*$").matchEntire(raw) ?: return raw
            val inside = bracketMatch.groupValues[1]  // 如 "女/女青年01"
            // inside 取最后的 "/" 或空格之后部分：
            // 女/女青年01 → 女青年01，主角 男主01 → 男主01
            return inside.substringAfterLast('/').substringAfterLast(' ').trim()
        }
    }
}