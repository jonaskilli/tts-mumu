package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Speed
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
 * 试听文本输入行：🎧=用这句文本试听，⚡=打开三层音频参数弹窗。
 *
 * 两者同框（用户 09-10 定稿）：调参与试听在同一行形成闭环——改文本→调参数→立刻用这句听，
 * 取代原先挂在朗读标签卡顶部、离试听两 cards 远的独立「音频参数」按钮。
 *
 * @param onAudioParams 传 null 则不显示参数入口（调用方无三层参数可调时）
 */
@Composable
fun AuditionTextField(
    modifier: Modifier,
    onAudition: (String) -> Unit,
    onAudioParams: (() -> Unit)? = null,
) {
    var text by remember { AppConfig.testSampleText }
    OutlinedTextField(
        modifier = modifier,
        label = { Text("💬 " + stringResource(R.string.audition_text)) },
        value = text,
        onValueChange = { text = it },
        trailingIcon = {
            Row {
                IconButton(onClick = { onAudition(text) }) {
                    Icon(Icons.Default.Headset, stringResource(id = R.string.audition))
                }
                if (onAudioParams != null)
                    IconButton(onClick = onAudioParams) {
                        Icon(Icons.Default.Speed, stringResource(id = R.string.audio_params))
                    }
            }
        }
    )
}