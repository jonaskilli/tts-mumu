package com.github.jing332.tts_server_android.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R

sealed class PagerDestination(
    val index: Int,
    @StringRes val strId: Int,
    @StringRes val contentDescId: Int,
    val icon: @Composable () -> Unit = {},
) {
    companion object {
        val routes by lazy {
            listOf(
                SystemTts,
                SystemTtsLog,
                HunyuanTaiji,
                SystemTtsForwarder,
                Settings
            )
        }
    }

    object SystemTts : PagerDestination(0, R.string.system_tts, R.string.system_tts, {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_config),
            contentDescription = null
        )
    })

    object SystemTtsLog : PagerDestination(1, R.string.log, R.string.log, {
        Icon(
            Icons.AutoMirrored.Default.TextSnippet,
            contentDescription = null
        )
    })

    object HunyuanTaiji : PagerDestination(2, R.string.hunyuan_taiji, R.string.hunyuan_taiji, {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_taiji),
            tint = Color.Unspecified,
            contentDescription = null
        )
    })

    object SystemTtsForwarder : PagerDestination(3, R.string.forwarder, R.string.forwarder, {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_app_notification),
            contentDescription = null
        )
    })

    object Settings : PagerDestination(4, R.string.settings, R.string.settings, {
        Icon(Icons.Default.Settings, contentDescription = null)
    })
}