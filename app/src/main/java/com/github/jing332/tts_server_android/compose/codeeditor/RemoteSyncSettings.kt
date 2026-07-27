package com.github.jing332.tts_server_android.compose.codeeditor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.DenseOutlinedField
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.CodeEditorConfig

@Composable
internal fun RemoteSyncSettings(onDismissRequest: () -> Unit) {
    var port by remember {
        mutableIntStateOf(CodeEditorConfig.remoteSyncPort.value)
    }
    val context = LocalContext.current
    AppDialog(
        title = { Text(stringResource(id = R.string.remote_sync_service)) },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(id = R.string.remote_sync_service_description),
                    modifier = Modifier.padding(8.dp)
                )

                DenseOutlinedField(label = { Text(context.getString(R.string.label_port)) },
                    value = if (port <= 0) "" else port.toString(),
                    onValueChange = {
                        try {
                            port = if (it.isBlank()) 0 else it.toInt()
                        } catch (_: NumberFormatException) {
                        }
                    })

            }
        },
        onDismissRequest = onDismissRequest,
        buttons = {
            val context = LocalContext.current
            Row {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://github.com/jing332/tts-server-psc")
                    })
                }) {
                    Text(stringResource(id = R.string.learn_more))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    CodeEditorConfig.remoteSyncPort.value = port
                    onDismissRequest()
                }) {
                    Text(stringResource(id = R.string.save))
                }
            }
        }
    )
}