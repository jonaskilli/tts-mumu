package com.github.jing332.tts_server_android.compose.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.TextCheckBox
import com.github.jing332.tts_server_android.R

/**
 * 单入口备份弹窗：顶部完整/分享模式单选，内容项按模式给默认勾选且可自由取消，
 * 底部"保存到"区：本地默认勾选 + WebDAV 可选，两者可同时勾选。
 */
@Composable
internal fun BackupDialog(
    onDismissRequest: () -> Unit,
    onBackupRequested: (BackupProfile, List<Type>, saveToLocal: Boolean, uploadToWebDav: Boolean) -> Unit,
) {
    var profile by remember { mutableStateOf(BackupProfile.PERSONAL_FULL) }
    val checkedTypes = remember {
        mutableStateListOf<Type>().apply { addAll(defaultTypes(BackupProfile.PERSONAL_FULL)) }
    }

    // 切模式时重置为该模式默认勾选集
    fun resetTypes(target: BackupProfile) {
        checkedTypes.clear()
        checkedTypes.addAll(defaultTypes(target))
    }

    var saveToLocal by remember { mutableStateOf(true) }
    var uploadToWebDav by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(modeTitleRes(profile))) },
        content = {
            LazyColumn(Modifier.fillMaxWidth()) {
                // 模式切换（用户 09-09）：RadioButton 行改 SegmentedButton 分段（同编辑页
                // 「朗读全部/标签」样式），宽度适配文字不均分，居中放置
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        SegmentedButton(
                            selected = profile == BackupProfile.PERSONAL_FULL,
                            onClick = {
                                if (profile != BackupProfile.PERSONAL_FULL) {
                                    profile = BackupProfile.PERSONAL_FULL
                                    resetTypes(profile)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) {
                            Text(stringResource(R.string.personal_complete_backup), maxLines = 1)
                        }
                        SegmentedButton(
                            selected = profile == BackupProfile.SHARE_SANITIZED,
                            onClick = {
                                if (profile != BackupProfile.SHARE_SANITIZED) {
                                    profile = BackupProfile.SHARE_SANITIZED
                                    resetTypes(profile)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) {
                            Text(stringResource(R.string.share_backup), maxLines = 1)
                        }
                    }
                    Text(
                        text = stringResource(modeWarningRes(profile)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.backup_included_content),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                items(availableTypes(profile)) { type ->
                    TextCheckBox(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(stringResource(type.nameStrId)) },
                        checked = type in checkedTypes,
                        onCheckedChange = { checked ->
                            if (checked) {
                                checkedTypes.add(type)
                            } else {
                                // 取消"插件"时连带取消"插件变量"
                                if (type == Type.Plugin) checkedTypes.remove(Type.PluginVars)
                                if (type == Type.Preference) checkedTypes.remove(Type.WebDav)
                                checkedTypes.remove(type)
                            }
                        },
                        horizontalArrangement = Arrangement.Start,
                    )
                }

                // 保存到：本地默认 + WebDAV 可选，可同时
                item {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.backup_save_to),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    TextCheckBox(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(stringResource(R.string.backup_save_local)) },
                        checked = saveToLocal,
                        onCheckedChange = { saveToLocal = it },
                        horizontalArrangement = Arrangement.Start,
                    )
                    TextCheckBox(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(stringResource(R.string.backup_to_webdav)) },
                        checked = uploadToWebDav,
                        onCheckedChange = { uploadToWebDav = it },
                        horizontalArrangement = Arrangement.Start,
                    )
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = {
                if (checkedTypes.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.backup_need_content), Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                if (!saveToLocal && !uploadToWebDav) {
                    Toast.makeText(context, context.getString(R.string.backup_need_save_target), Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onBackupRequested(profile, checkedTypes.toList(), saveToLocal, uploadToWebDav)
                onDismissRequest()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
    )
}

private fun availableTypes(profile: BackupProfile): List<Type> = when (profile) {
    BackupProfile.PERSONAL_FULL -> Type.typeList
    BackupProfile.SHARE_SANITIZED -> listOf(
        Type.Preference,
        Type.List,
        Type.SpeechRule,
        Type.ReplaceRule,
        Type.Plugin,
    )
}

private fun defaultTypes(profile: BackupProfile): List<Type> = availableTypes(profile)

private fun modeTitleRes(profile: BackupProfile): Int = when (profile) {
    BackupProfile.PERSONAL_FULL -> R.string.personal_complete_backup
    BackupProfile.SHARE_SANITIZED -> R.string.share_backup
}

private fun modeWarningRes(profile: BackupProfile): Int = when (profile) {
    BackupProfile.PERSONAL_FULL -> R.string.personal_backup_sensitive_warning
    BackupProfile.SHARE_SANITIZED -> R.string.share_backup_privacy_warning
}
