package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig

/**
 * 试听文本输入行：🎧=用这句文本试听。
 *
 * ⚡音频参数入口已删（用户 09-10 三键直出定稿）：音频参数键改为直接列在试听文本下方
 * （[AudioParamsDimChipsRow]，点语速/音量/音高开单维弹窗 [AudioParamsDimDialog]），
 * 不再需要行内入口。
 *
 * 本地音效配置（tagName=本地音效N）读写 [AppConfig.localSoundSampleText]，
 * 其余配置读写 [AppConfig.testSampleText]——两份文本互不影响（用户 09-13）。
 */

/** 本地音效标签名判定（tagName 口径，与 LogQuickPanel.isLocalSoundSlot 同源） */
fun isLocalSoundTagName(tagName: String): Boolean = tagName.matches(Regex("本地音效\\d*"))

@Composable
fun AuditionTextField(
    modifier: Modifier,
    onAudition: (String) -> Unit,
    isLocalSound: Boolean = false,
) {
    var text by remember(isLocalSound) {
        if (isLocalSound) AppConfig.localSoundSampleText else AppConfig.testSampleText
    }
    OutlinedTextField(
        modifier = modifier,
        label = { Text("💬 " + stringResource(R.string.audition_text)) },
        value = text,
        onValueChange = { text = it },
        trailingIcon = {
            IconButton(onClick = { onAudition(text) }) {
                Icon(Icons.Default.Headset, stringResource(id = R.string.audition))
            }
        }
    )
}
