package com.github.jing332.tts_server_android.compose

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.jing332.common.DateFormatConst
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppLauncherIcon
import com.github.jing332.tts_server_android.BuildConfig
import com.github.jing332.tts_server_android.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VersionInfo(modifier: Modifier) {
    val context = LocalContext.current
    val view = LocalView.current

    var isBuildTimeExpanded by remember { mutableStateOf(false) }
    val versionNameText = remember {
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    SelectionContainer {
        Column(modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { isBuildTimeExpanded = !isBuildTimeExpanded }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLauncherIcon(Modifier.size(64.dp), R.mipmap.ic_app_launcher_round)
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = versionNameText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            AnimatedVisibility(visible = isBuildTimeExpanded) {
                Text(
                    text = DateFormatConst.dateFormatSec.format(BuildConfig.BUILD_TIME * 1000),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun AboutDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = {
            VersionInfo(Modifier.padding(vertical = 8.dp))

        },
        content = {
            fun openUrl(uri: String) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = uri.toUri()
                    }
                )
            }

            Column(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Github - TTS Server",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            openUrl("https://github.com/jing332/tts-server-android")
                        }
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        buttons = {
            TextButton(onClick = {
                onDismissRequest()
                context.startActivity(
                    Intent(context, LibrariesActivity::class.java).setAction(Intent.ACTION_VIEW)
                )
            }) {
                Text(text = stringResource(id = R.string.open_source_license))
            }

            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.confirm))
            }
        })
}