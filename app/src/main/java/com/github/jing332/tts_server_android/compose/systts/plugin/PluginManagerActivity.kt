package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

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
                            val scope = rememberCoroutineScope()
                            val plugin: Plugin = rememberSaveable {
                                checkNotNull(sharedVM.getOnce(NavRoutes.PluginEdit.KEY_DATA)) { "No Plugin Data" }
                            }
                            // 第11项: 列表项"运行键"传入的自动调试标志
                            val autoDebug = remember {
                                sharedVM.getOnce<Boolean>("autoDebug") ?: false
                            }

                            PluginEditorScreen(plugin, autoDebug = autoDebug, onSave = {
                                scope.launch {
                                    withIO {
                                        // pluginId 相同视为同一插件：保存时若已有同 pluginId 且主键不同的
                                        // 条目，借旧主键 REPLACE 覆盖——防 js 直导/手动新建插出同 pluginId
                                        // 双条目（音频配置按 pluginId 关联插件，双条目会解析到旧插件）。
                                        // 编辑本体（主键相同）与全新 pluginId 照旧直插。与朗读规则
                                        // SpeechRuleManagerActivity 的同名处理同口径。
                                        val entity = if (it.pluginId.isNotBlank()) {
                                            dbm.pluginDao.getMetaByPluginId(it.pluginId)
                                                ?.takeIf { twin -> twin.id != it.id }
                                                ?.let { twin -> it.copy(id = twin.id) } ?: it
                                        } else it
                                        dbm.pluginDao.insert(entity)
                                    }
                                }
                            })
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