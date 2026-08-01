package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.jing332.common.utils.ASFUriUtils.getPath
import com.github.jing332.common.utils.FileUtils.audioList
import com.github.jing332.common.utils.toScale
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppSelectionDialog
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.ExoPlayerActivity
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

class BgmConfigUI : IConfigUI() {
    override val showSpeechEdit: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val config = systemTts.config as BgmConfiguration

        val context = LocalContext.current
        val filePicker =
            rememberLauncherForActivityResult(contract = AppActivityResultContracts.filePickerActivity()) {
                runCatching {
                    val path =
                        context.getPath(it.second, it.first is FilePickerActivity.RequestSelectDir)
                    if (path.isNullOrBlank()) context.toast(R.string.path_is_empty)
                    else {
                        onSystemTtsChange(
                            systemTts.copy(
                                config = config.copy(
                                    musicList = config.musicList.toMutableList().apply { add(path) }
                                )
                            )
                        )

                    }
                }.onFailure {
                    context.displayErrorDialog(it)
                }
            }

        var showMusicList by remember { mutableStateOf("") }
        if (showMusicList != "") {
            val audioFiles = remember(showMusicList) {
                try {
                    File(showMusicList).audioList()
                } catch (e: Exception) {
                    context.displayErrorDialog(e)
                    null
                }
            } ?: return

            AppSelectionDialog(
                onDismissRequest = { showMusicList = "" },
                title = { Text(showMusicList) },
                value = Any(),
                values = audioFiles,
                entries = audioFiles.map { it.name },
                onClick = { value, _ ->
                    context.startActivity(Intent(context, ExoPlayerActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = (value as File).toUri()
                    })
                }
            )
        }

        val saveSignal = remember { mutableStateOf<(() -> Unit)?>(null) }
        DefaultFullEditScreen(
            modifier,
            title = stringResource(id = R.string.edit_bgm_tts),
            verticalScrollEnabled = false,
            onCancel = onCancel,
            onSave = {
                saveSignal.value?.invoke()
                onSave()
            }
        ) {
            BasicInfoEditScreen(
                modifier = Modifier.padding(8.dp),
                systemTts = systemTts,
                onSystemTtsChange = onSystemTtsChange
            )

            ParamsEditScreen(
                modifier = Modifier
                    .fillMaxWidth(),
                systemTts = systemTts,
                onSystemTtsChange = onSystemTtsChange
            )

            OutlinedCard(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                FilesAccessPermissionContent(Modifier.fillMaxWidth())

                Row(
                    Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        filePicker.launch(
                            FilePickerActivity.RequestSelectFile(
                                fileMimes = listOf("audio/*")
                            )
                        )
                    }) {
                        Icon(Icons.Default.AudioFile, stringResource(R.string.add_file))
                        Text(stringResource(id = R.string.add_file))
                    }
                    VerticalDivider(Modifier.height(16.dp))
                    TextButton(onClick = {
                        filePicker.launch(
                            FilePickerActivity.RequestSelectDir()
                        )
                    }) {
                        Icon(Icons.Default.CreateNewFolder, stringResource(R.string.add_folder))
                        Text(stringResource(id = R.string.add_folder))
                    }
                }

                LazyColumn(
                    Modifier
                        .padding(8.dp)
                ) {
                    items(config.musicList) { item ->
                        Row(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickableRipple {
                                    showMusicList = item
                                }
                        ) {
                            Text(
                                item,
                                modifier = Modifier.weight(1f),
                                lineHeight = LocalTextStyle.current.lineHeight * 0.8
                            )
                            IconButton(onClick = {
                                onSystemTtsChange(
                                    systemTts.copy(
                                        config = config.copy(
                                            musicList = config.musicList.toMutableList()
                                                .apply { remove(item) }
                                        ),
                                    )
                                )
                            }) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    stringResource(id = R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    ) {
        val config = systemTts.config as BgmConfiguration

        LabelSlider(
            modifier = modifier.padding(vertical = 12.dp),
            text = stringResource(R.string.label_speech_volume, "%.2f".format(config.volume)),
            value = config.volume.toFloat(),
            onValueChange = {
                onSystemTtsChange(systemTts.copy(config = config.copy(volume = it.toScale(2))))
            }, valueRange = 0.1f..1f, step = 0.05f
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                onSystemTtsChange(systemTts.copy(config = config.copy(volume = 1f)))
            }) {
                Text(stringResource(id = R.string.reset))
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun FilesAccessPermissionContent(modifier: Modifier = Modifier) {
        val context = LocalContext.current

        @Composable
        fun ColumnScope.warnButton(text: String, onClick: () -> Unit) {
            FilledTonalButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp),
                onClick = onClick,
                content = {
                    Text(
                        text,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }

        Column(modifier) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // A11
                var isGranted by remember { mutableStateOf(Environment.isExternalStorageManager()) }
                val permissionCheckerObserver = remember {
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isGranted = Environment.isExternalStorageManager()
                        }
                    }
                }
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                DisposableEffect(lifecycle, permissionCheckerObserver) {
                    lifecycle.addObserver(permissionCheckerObserver)
                    onDispose { lifecycle.removeObserver(permissionCheckerObserver) }
                }

                if (!isGranted) {
                    warnButton(text = stringResource(id = R.string.grant_permission_all_file)) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            setData(Uri.parse("package:${context.packageName}"))
                        })
                    }
                }
            }

            val storagePermission =
                rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (!storagePermission.status.isGranted)
                warnButton(text = stringResource(R.string.grant_permission_storage_file)) {
                    storagePermission.launchPermissionRequest()
                }
        }
    }

}