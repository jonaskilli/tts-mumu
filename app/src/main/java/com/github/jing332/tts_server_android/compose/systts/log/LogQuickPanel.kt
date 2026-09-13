package com.github.jing332.tts_server_android.compose.systts.log

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.LogEntry
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.compose.systts.common.LOCAL_SOUND_TAG_NAME

/**
 * 日志快捷面板「发音人调整」：点带 configId 的"请求音频"主行弹出。
 *
 * 09-13 起整体实现抽到 [VoicePickerDialog]（通用换声弹窗，与角色管理插件桥共用同一弹窗，
 * 完全同源——目目 09-13 定稿），本文件只剩日志面板宿主的参数翻译：
 * - 锚点=日志主行 configId；
 * - bindingKey=entry.roleName（多角色日志），空且 tagName 命中「本地音效N」=本地音效槽位（同名绑定键）；
 * - sharedVM 保留主列表联动（换声后定位高亮、标记版本联动）。
 */
@Composable
fun LogQuickPanel(
    onDismissRequest: () -> Unit,
    entry: LogEntry,
) {
    // 主界面共享状态：换旁白发音人后通知主列表定位高亮（Activity 级单例，与 MainPager 同实例）
    val sharedVM: SharedViewModel = viewModel()

    // 只为取 bindingKey/anchorTag 读一次配置；实体与缺失提示由 VoicePickerDialog 内部处理
    //（配置项已删时弹窗内部 Toast 后自动 onDismissRequest）
    val config = remember(entry.configId) {
        (dbm.systemTtsV2.get(entry.configId)?.config as? TtsConfigurationDTO)
    }
    // 本地音效槽位单独标记（目目 09-13）：这类槽位的候选不能取自发音人池（组件内已处理），
    // bindingKey 落槽位名「本地音效N」（目目 09-12：tagName 带序号，按角色那种处理）
    val isLocalSoundSlot = entry.roleName.isBlank() &&
        config?.speechRule?.tagName?.let { LOCAL_SOUND_TAG_NAME.matches(it) } == true
    val bindingKey = entry.roleName.ifBlank {
        if (isLocalSoundSlot) config?.speechRule?.tagName.orEmpty() else ""
    }

    VoicePickerDialog(
        anchorConfigId = entry.configId,
        anchorTag = config?.speechRule?.tag.orEmpty(),
        bindingKey = bindingKey,
        titleText = stringResource(R.string.log_panel_title),
        titleBadge = entry.roleName,
        isLocalSoundSlot = isLocalSoundSlot,
        sharedVM = sharedVM,
        onDismissRequest = onDismissRequest,
    )
}
