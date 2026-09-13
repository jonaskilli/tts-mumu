package com.github.jing332.tts.speech.plugin.engine

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 通用换声弹窗桥请求（用户 09-13「完全同源」定稿）。
 *
 * 插件 JS 经 ttsrv.showVoicePickerDialog 提交请求 → [VoicePickerBus.request]；
 * app 端挂在插件 UI 宿主组合（PluginTtsUI.EditContentScreen）的观察者读到非空请求后，
 * 渲染与日志快捷面板完全同款的 VoicePickerDialog（更换发音人+音频参数两分段）。
 *
 * @param bindingKey 绑定键（角色名）；空=非绑定模式（与日志面板分支同语义）
 * @param anchorTag  锚点 tag（角色当前绑定的 tag id）；弹窗按它解析参数/显示基准配置项
 * @param title      弹窗标题
 */
data class VoicePickerRequest(
    val bindingKey: String,
    val anchorTag: String,
    val title: String,
)

/**
 * 换声弹窗请求总线：lib-tts（桥入口）与 app（弹窗宿主）之间的单槽通道。
 * 任意时刻最多一个请求（StateFlow 单值），宿主关闭弹窗即 [clear]。
 */
object VoicePickerBus {
    val request = MutableStateFlow<VoicePickerRequest?>(null)

    private var invoker: ((String, List<Any?>) -> Unit)? = null
    private var callbackName: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 提交请求（桥入口调用；inv=引擎注入的 JS 回调通道，cbName=PluginJS 上的回调函数名） */
    fun submit(req: VoicePickerRequest, inv: ((String, List<Any?>) -> Unit)?, cbName: String) {
        invoker = inv
        callbackName = cbName
        request.value = req
    }

    /** 宿主关闭弹窗后清槽（请求作废，不再回喊） */
    fun clear() {
        request.value = null
        invoker = null
        callbackName = null
    }

    /**
     * 弹窗内变化回喊插件 JS（event: applied=换声落库 / deleted=配置项删除 / marked=标记变化）。
     * payload 为 JSON 字符串 {"event":..,"tag":..}；Rhino 调用投递到主线程执行——
     * 插件 JS 回调会刷新自己的原生视图，必须在主线程跑（换声确认的 Compose 点击本就在主线程，
     * 删除走 withIO 协程，故统一 post 兜底）。
     */
    fun notifyMutated(event: String, tag: String) {
        val inv = invoker ?: return
        val cb = callbackName ?: return
        val payload = org.json.JSONObject().put("event", event).put("tag", tag).toString()
        mainHandler.post {
            runCatching { inv.invoke(cb, listOf(payload)) }
        }
    }
}
