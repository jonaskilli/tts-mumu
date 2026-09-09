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
 */
@Composable
fun AuditionTextField(
    modifier: Modifier,
    onAudition: (String) -> Unit,
) {
    var text by remember { AppConfig.testSampleText }
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
