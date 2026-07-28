package com.github.jing332.tts_server_android.compose.forwarder

import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.github.jing332.compose.widgets.CheckedMenuItem
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForwarderTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    wakeLockEnabled: Boolean,
    onWakeLockEnabledChange: (Boolean) -> Unit,

    actions: @Composable ColumnScope.(onDismissRequest: () -> Unit) -> Unit = { },
    onClearWebData: (() -> Unit)? = null,
    onOpenWeb: () -> String,
    onAddDesktopShortCut: () -> Unit,
) {
    val context = LocalContext.current
    NavTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = {
                val url = onOpenWeb.invoke()
                if (url.isNotEmpty()) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            }) {

                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(
                        id = R.string.open_web
                    )
                )
            }

            var showOptions by remember { mutableStateOf(false) }

            IconButton(onClick = { showOptions = true }) {
                Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                    CheckedMenuItem(
                        text = { Text(text = stringResource(id = R.string.wake_lock)) },
                        checked = wakeLockEnabled,
                        onClick = onWakeLockEnabledChange,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.desktop_shortcut)) },
                        onClick = {
                            showOptions = false
                            onAddDesktopShortCut()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AddBusiness, null)
                        }
                    )

                    actions { showOptions = false }
                }
            }

        }
    )
}