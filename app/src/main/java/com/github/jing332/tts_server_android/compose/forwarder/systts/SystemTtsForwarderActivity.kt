package com.github.jing332.tts_server_android.compose.forwarder.systts

import android.os.Bundle
import androidx.activity.compose.setContent
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme

/**
 * 转发器独立承载页，从「设置」入口启动，使底栏不再单独占用一栏。
 */
class SystemTtsForwarderActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SystemTtsForwarderScreen()
            }
        }
    }
}
