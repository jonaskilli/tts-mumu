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
 * ⚡音频参数入口已删：音频参数改为**直接在试听文本下方就地展开**
 * （[AudioParamsDimRows]，软槽分段一行三项 + 展开滑杆、默认收起——09-12 定稿，
 * 取代 09-10 的「三键开单维弹窗」方案），本行不再需要行内入口。
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
