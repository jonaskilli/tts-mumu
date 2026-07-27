package com.github.jing332.tts_server_android.compose.systts.list.ui

abstract class ItemDescriptor {
    open val name: String = "name"
    open val desc: String = "desc"
    open val bottom: String = "bottom"
    open val type: String = "type"
    open val tagName: String = "tag"
    open val standby: Boolean = false
}