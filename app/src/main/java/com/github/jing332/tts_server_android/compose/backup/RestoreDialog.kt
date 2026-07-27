package com.github.jing332.tts_server_android.compose.backup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.launch

@Composable
internal fun RestoreDialog(
    onDismissRequest: () -> Unit,
    bytes: ByteArray, 
    vm: BackupRestoreViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var needRestart by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                needRestart = vm.restore(bytes)
                isLoading = false
            }.onFailure {
                context.displayErrorDialog(it)
                onDismissRequest() 
            }
        }
    }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.restore)) },
        content = {
            LoadingContent(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                isLoading = isLoading
            ) {
                if (!isLoading)
                    if (needRestart) Text(stringResource(id = R.string.restore_restart_msg))
                    else Text(stringResource(id = R.string.restore_finished))
            }
        },
        buttons = {
            if (needRestart) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(id = R.string.cancel))
                }
                TextButton(onClick = { app.restart() }) {
                    Text(stringResource(id = R.string.restart))
                }
            } else {
                if (!isLoading) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    )
}
