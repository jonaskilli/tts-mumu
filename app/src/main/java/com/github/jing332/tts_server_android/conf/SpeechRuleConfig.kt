package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object SpeechRuleConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("speech_rule", 0)) }

    val textParam by lazy {
        mutableDataSaverStateOf(pref, key = "textParam", "这是一个Android系统TTS应用，内置微软演示接口，可自定义HTTP请求，可导入其他本地TTS引擎，以及根据中文双引号的简单旁白/对话识别朗读 ，还有自动重试，备用配置，文本替换等更多功能。")
    }

    // 角色管理栏启用配置项签名(基于启用项 id/标签/顺序的 hashCode)。
    // 用于判断进入角色管理栏时是否需要重新 eval 朗读规则并生成角色文件：
    // 签名与上次一致则跳过耗时的 JS eval + handleText，直接复用磁盘上已生成的文件。
    // Int.MIN_VALUE 作为"未初始化"哨兵，强制首次生成。
    val lastRoleSig by lazy { mutableDataSaverStateOf(pref, key = "lastRoleSig", Int.MIN_VALUE) }
}
