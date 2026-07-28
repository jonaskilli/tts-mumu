package com.github.jing332.tts_server_android.compose.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.ArrowCircleUp

import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.backup.BackupRestoreActivity
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.directlink.LinkUploadRuleActivity
import com.github.jing332.tts_server_android.compose.theme.getAppTheme
import com.github.jing332.tts_server_android.compose.theme.setAppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.constant.FilePickerMode
import androidx.core.content.ContextCompat.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog)
        ThemeSelectionDialog(
            onDismissRequest = { showThemeDialog = false },
            currentTheme = getAppTheme(),
            onChangeTheme = {
                setAppTheme(it)
            }
        )

    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
            topBar = {
                NavTopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    scrollBehavior = scrollBehaviour,
                )
            }
        ) { paddingValues ->
            val context = LocalContext.current
            Column(
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                DividerPreference { Text(stringResource(id = R.string.app_name)) }

            // 后台保活设置入口（使用 Activity 启动，与备份恢复保持一致）
            BasePreferenceWidget(
                onClick = {
                    context.startActivity(
                        Intent(context, KeepAliveSettingsActivity::class.java)
                    )
                },
                title = { Text(stringResource(id = R.string.keep_alive_settings)) },
                subTitle = { Text(stringResource(R.string.keep_alive_settings_summary)) },
                icon = { Icon(Icons.Default.PowerSettingsNew, null) }
            )

                BasePreferenceWidget(
                    icon = {
                        Icon(Icons.Default.SettingsBackupRestore, null)
                    },
                    onClick = {
                        context.startActivity(
                            Intent(
                                context,
                                BackupRestoreActivity::class.java
                            ).apply { action = Intent.ACTION_VIEW })
                    },
                    title = { Text(stringResource(id = R.string.backup_restore)) },
                )

                BasePreferenceWidget(
                    icon = {
                        Icon(Icons.Default.Link, null)
                    },
                    onClick = {
                        context.startActivity(
                            Intent(
                                context, LinkUploadRuleActivity::class.java
                            ).apply { action = Intent.ACTION_VIEW })
                    },
                    title = { Text(stringResource(id = R.string.direct_link_settings)) },
                )

                BasePreferenceWidget(
                    icon = { Icon(Icons.Default.ColorLens, null) },
                    onClick = { showThemeDialog = true },
                    title = { Text(stringResource(id = R.string.theme)) },
                    subTitle = { Text(stringResource(id = getAppTheme().stringResId)) },
                )

                val languageKeys = remember {
                    mutableListOf("").apply { addAll(AppLocale.localeMap.keys.toList()) }
                }

                val languageNames = remember {
                    AppLocale.localeMap.map { "${it.value.displayName} - ${it.value.getDisplayName(it.value)}" }
                        .toMutableList()
                        .apply { add(0, context.getString(R.string.follow_system)) }
                }

                var langMenu by remember { mutableStateOf(false) }
                DropdownPreference(
                    Modifier.minimumInteractiveComponentSize(),
                    expanded = langMenu,
                    onExpandedChange = { langMenu = it },
                    icon = {
                        Icon(Icons.Default.Language, null)
                    },
                    title = { Text(stringResource(id = R.string.language)) },
                    subTitle = {
                        Text(
                            if (AppLocale.getLocaleCodeFromFile(context).isEmpty()) {
                                stringResource(id = R.string.follow_system)
                            } else {
                                AppLocale.getLocaleFromFile(context).displayName
                            }
                        )
                    }) {
                    languageNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = {
                                Text(name)
                            }, onClick = {
                                langMenu = false

                                AppLocale.saveLocaleCodeToFile(context, languageKeys[index])
                                AppLocale.setLocale(app as Context)
                            }
                        )
                    }
                }

                var filePickerMode by remember { AppConfig.filePickerMode }
                var expanded by remember { mutableStateOf(false) }
                DropdownPreference(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    icon = { Icon(Icons.Default.FileOpen, null) },
                    title = { Text(stringResource(id = R.string.file_picker_mode)) },
                    subTitle = {
                        Text(
                            when (filePickerMode) {
                                FilePickerMode.PROMPT -> stringResource(id = R.string.file_picker_mode_prompt)
                                FilePickerMode.BUILTIN -> stringResource(id = R.string.file_picker_mode_builtin)
                                else -> stringResource(id = R.string.file_picker_mode_system)
                            }
                        )
                    },
                    actions = {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.file_picker_mode_prompt)) },
                            onClick = {
                                expanded = false
                                filePickerMode = FilePickerMode.PROMPT
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.file_picker_mode_builtin)) },
                            onClick = {
                                expanded = false
                                filePickerMode = FilePickerMode.BUILTIN
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.file_picker_mode_system)) },
                            onClick = {
                                expanded = false
                                filePickerMode = FilePickerMode.SYSTEM
                            }
                        )
                    }
                )

                var autoCheck by remember { AppConfig.isAutoCheckUpdateEnabled }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.auto_check_update)) },
                    subTitle = { Text(stringResource(id = R.string.check_update_summary)) },
                    checked = autoCheck,
                    onCheckedChange = { autoCheck = it },
                    icon = {
                        Icon(Icons.Default.ArrowCircleUp, contentDescription = null)
                    }
                )

                var excludeFromRecent by remember { AppConfig.isExcludeFromRecent }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.exclude_from_recent)) },
                    subTitle = { Text(stringResource(id = R.string.exclude_from_recent_summary)) },
                    checked = excludeFromRecent,
                    onCheckedChange = { excludeFromRecent = it },
                    icon = {
                        Icon(Icons.Default.HideSource, contentDescription = null)
                    }
                )

                var maxDropdownCount by remember { AppConfig.spinnerMaxDropDownCount }
                SliderPreference(
                    title = { Text(stringResource(id = R.string.spinner_drop_down_max_count)) },
                    subTitle = { Text(stringResource(id = R.string.spinner_drop_down_max_count_summary)) },
                    value = maxDropdownCount.toFloat(),
                    onValueChange = { maxDropdownCount = it.toInt() },
                    label = if (maxDropdownCount == 0) stringResource(id = R.string.unlimited) else maxDropdownCount.toString(),
                    valueRange = 0f..50f,
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuOpen, null) }
                )

                SysttsSettingsScreen()
                OtherSettingsScreen()

                Spacer(Modifier.navigationBarsPadding())
            }
        }
}
