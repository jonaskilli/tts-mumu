package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsMigration
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.AppConst
import kotlinx.serialization.encodeToString

@Composable
fun MigrationTips(modifier: Modifier = Modifier) {
    var showConfigExport by remember {
        mutableStateOf("")
    }

    if (showConfigExport.isNotEmpty())
        ConfigExportBottomSheet(showConfigExport) {
            showConfigExport = ""
        }

    var show by remember {
        mutableStateOf(SystemTtsMigration.needMigrate())
    }
    var enabled by remember {
        mutableStateOf(false)
    }

    if (show)
        AppDialog(
            modifier = modifier, onDismissRequest = {}, title = {
                Text(stringResource(R.string.migration_data))
            },
            content = {
                Text(stringResource(R.string.systts_list_migration_desc))
            }, buttons = {
                Row {
                    TextButton(
                        enabled = enabled,
                        onClick = {
                            SystemTtsMigration.migrate()
                            show = false
                        },
                    ) { Text(stringResource(android.R.string.ok)) }

                    TextButton(
                        onClick = {
                            showConfigExport =
                                AppConst.jsonBuilder.encodeToString(dbm.systemTtsDao.getAllGroupWithTts())
                            enabled = true
                        },
                    ) { Text(stringResource(R.string.export_config)) }
                }
            }
        )
}