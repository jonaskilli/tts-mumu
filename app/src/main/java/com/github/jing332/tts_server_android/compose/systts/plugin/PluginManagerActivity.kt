package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.theme.AppTheme

class PluginManagerActivity : ComposeActivity() {
    private var jsCode by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent != null) importJsCodeFromIntent(intent)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val sharedVM: SharedViewModel = viewModel()
                CompositionLocalProvider(LocalNavController provides navController) {
                    LaunchedEffect(jsCode) {
                        if (jsCode.isNotBlank()) {
                            sharedVM.put(NavRoutes.PluginEdit.KEY_DATA, Plugin(code = jsCode))
                            navController.navigate(NavRoutes.PluginEdit.id)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.PluginManager.id
                    ) {
                        composable(NavRoutes.PluginManager.id) {
                            PluginManagerScreen(sharedVM) { finish() }
                        }

                        composable(NavRoutes.PluginEdit.id) {
                            val plugin: Plugin = rememberSaveable {
                                checkNotNull(sharedVM.getOnce(NavRoutes.PluginEdit.KEY_DATA)) { "No Plugin Data" }
                            }

                            PluginEditorScreen(plugin, onSave = { dbm.pluginDao.insert(it) })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        importJsCodeFromIntent(intent)
    }

    private fun importJsCodeFromIntent(intent: Intent) {
        jsCode = intent.getStringExtra("js") ?: return
        intent.removeExtra("js")
    }
}