package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.JsConsoleManager
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.LoggerFloatingManager
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.toCode
import com.github.jing332.tts_server_android.ui.view.ErrorDialogActivity
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("DEPRECATION")
class PluginPreviewActivity : ComposeActivity() {
    companion object {
        const val KEY_SOURCE = "source"
        const val KEY_PLUGIN = "plugin"
        const val ACTION_FINISH = "finish"

        private val logger = KotlinLogging.logger { PluginPreviewActivity::class.java.name }
    }

    private val mReceiver by lazy { MyBroadcastReceiver() }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FINISH) {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppConst.localBroadcast.unregisterReceiver(mReceiver)
        AppConst.localBroadcast.sendBroadcastSync(Intent(ErrorDialogActivity.ACTION_FINISH))
        LoggerFloatingManager.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConst.localBroadcast.registerReceiver(mReceiver, IntentFilter(ACTION_FINISH))

        val argSource: PluginTtsSource? = intent.getParcelableExtra(KEY_SOURCE)
        val plugin = intent.getParcelableExtra<Plugin>(KEY_PLUGIN)
        if (argSource == null || plugin == null) {
            finish()
            return
        }

        val source = (if (argSource.locale.isBlank()) {
            argSource.copy(locale = AppLocale.current(this).toCode())// eg: en-US, zh-CN)
        } else argSource).copy(plugin = plugin)

        logger.atDebug {
            message = "loading preview plugin ui"
            payload = mapOf("source" to source, "plugin" to plugin)
        }


        LoggerFloatingManager.show(this, JsConsoleManager.ui)
        setContent {
            AppTheme {
                var systts by rememberSaveable {
                    mutableStateOf(
                        SystemTtsV2(
                            config = TtsConfigurationDTO(source = source)
                        )
                    )
                }

                PluginPreviewScreen(
                    plugin = plugin,
                    systts = systts,
                    onSysttsChange = { systts = it },
                    onSave = {
                        intent.putExtra(KEY_SOURCE, systts.ttsConfig.source as PluginTtsSource)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PluginPreviewScreen(
        plugin: Plugin,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = { Text(stringResource(id = R.string.plugin_preview_ui)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(id = R.string.nav_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onSave()
                        }) {
                            Icon(Icons.Default.Save, stringResource(id = R.string.save))
                        }

                    }
                )
            }) { paddingValues ->
            val ui = remember { PluginTtsUI() }
            ui.EditContentScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                systts = systts,
                onSysttsChange = onSysttsChange,
                showBasicInfo = false,
                plugin = plugin,
                showPluginSelector = false,
                onSaved = { finish() },
            )
        }
    }
}