package com.github.jing332.tts_server_android.compose.settings

import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.clearWebViewData
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AboutDialog
import com.github.jing332.tts_server_android.compose.LocalUpdateCheckTrigger
import com.github.jing332.tts_server_android.ui.AppHelpDocumentActivity

@Composable
internal fun ColumnScope.OtherSettingsScreen() {
    DividerPreference { Text(stringResource(R.string.other)) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    if (showAboutDialog)
        AboutDialog { showAboutDialog = false }
    BasePreferenceWidget(
        onClick = {
            showAboutDialog = true
        }, title = {
            Text(stringResource(R.string.about))
        }, icon = {
            Icon(Icons.Default.Info, null)
        }
    )

    val context = LocalContext.current
    BasePreferenceWidget(
        onClick = {
            context.startActivity(
                Intent(
                    context,
                    AppHelpDocumentActivity::class.java
                ).apply { action = Intent.ACTION_VIEW }
            )
        },
        title = { Text(stringResource(R.string.app_help_document)) },
        icon = {
            Icon(Icons.AutoMirrored.Default.HelpOutline, null)
        }
    )


    val updateCheckTrigger = LocalUpdateCheckTrigger.current
    BasePreferenceWidget(
        onClick = { updateCheckTrigger.value = true },
        title = { Text(stringResource(R.string.check_update)) },
        icon = {
            Icon(Icons.Default.Refresh, null)
        }
    )


    BasePreferenceWidget(
        onClick = {
            context.clearWebViewData()
            context.toast(R.string.clear_cache_ok)
        },
        title = { Text(stringResource(R.string.clear_web_data)) },
        icon = {
            Icon(Icons.Default.CleaningServices, null)
        }
    )
}
