package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.R
import com.drake.net.utils.withIO

/**
 * 「切换来源插件」的校验与确认（用户 09-12 拍板，**三处共用**：插件卡片菜单 / 顶栏失效配置项 /
 * 批量修改配置的「来源插件」段）。
 *
 * 规则（用户 09-12 明确裁定）：
 * - **只按音色 id（voice）严格比对**目标插件的音色清单；命中才切，未命中的**保持原样**并点名列出
 *   ——理由：对不上的话切了也放不出声。
 * - **不做「按名字匹配」兜底**（名字可能被改过/带序号，会错配）。
 * - **不保留强制全切口子**（对不上就别切）。
 */

/** 配置项的音色 id（voice）；非插件来源返回空串 */
internal fun voiceIdOf(tts: SystemTtsV2): String =
    ((tts.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.voice.orEmpty()

/**
 * 拉取目标插件的**全部音色 id**（遍历其所有池子）。
 * 引擎为纯脚本执行（同「按插件音色分类入库」的用法），**必须在 IO 线程调用**。
 */
internal fun loadPluginVoiceIds(context: Context, plugin: Plugin): Set<String> {
    val engine = TtsPluginUiEngineV2(context, plugin)
    return try {
        engine.eval()
        engine.onLoad()
        buildSet {
            engine.getLocales().keys.forEach { poolId ->
                runCatching { engine.getVoices(poolId) }.getOrNull().orEmpty()
                    .forEach { voice -> if (voice.id.isNotBlank()) add(voice.id) }
            }
        }
    } finally {
        runCatching { engine.destroy() }
    }
}

/**
 * 换来源插件前的校验确认弹窗：先拉目标插件音色清单（转圈），再显示「能匹配 N 项 / 匹配不上 M 项」，
 * 确认键只提交命中的那些项。
 *
 * @param message 说明文案（各调用处自备，点明把什么东西换成什么）
 * @param items 待切换的配置项（调用处已按来源筛好）
 * @param onConfirm 回调**只传命中的项**，由调用处执行落库
 */
@Composable
internal fun SourceSwitchCheckDialog(
    title: String,
    message: String,
    items: List<SystemTtsV2>,
    targetPluginId: String,
    targetPluginName: String,
    onDismiss: () -> Unit,
    onConfirm: (List<SystemTtsV2>) -> Unit,
) {
    val context = LocalContext.current
    // null=还在校验；非空=校验完成（读不到插件时也置空集会配合 loadFailed 提示）
    var voiceIds by remember { mutableStateOf<Set<String>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(targetPluginId, items.size) {
        voiceIds = null
        loadFailed = false
        val loaded = runCatching {
            withIO {
                dbm.pluginDao.getByPluginId(targetPluginId)
                    ?.let { loadPluginVoiceIds(context, it) }
            }
        }.getOrNull()
        voiceIds = loaded ?: emptySet()
        loadFailed = loaded == null
    }
    val ids = voiceIds
    val matched = if (ids == null) emptyList() else items.filter { voiceIdOf(it) in ids }
    val unmatched = if (ids == null) emptyList() else items.filterNot { voiceIdOf(it) in ids }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        content = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    ids == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "正在检查目标插件有没有这些音色…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    loadFailed -> Text(
                        "没能读到「$targetPluginName」的音色清单，无法校验，已停止切换。",
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> {
                        Text(
                            "能匹配上：${matched.size} 项",
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (unmatched.isNotEmpty()) {
                            Text(
                                "匹配不上：${unmatched.size} 项（目标插件没有这些音色，保持原样）",
                                color = MaterialTheme.colorScheme.error
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                            ) {
                                items(unmatched, { it.id }) { tts ->
                                    val voice = voiceIdOf(tts)
                                    Text(
                                        text = if (voice.isBlank()) tts.displayName
                                        else "${tts.displayName}　·　$voice",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                if (ids != null && !loadFailed) {
                    TextButton(
                        onClick = { onConfirm(matched) },
                        enabled = matched.isNotEmpty()
                    ) {
                        Text("只切换能匹配的 ${matched.size} 项")
                    }
                }
            }
        }
    )
}
