package com.github.jing332.tts_server_android.compose.systts.plugin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val sharedVM: SharedViewModel = viewModel()
                CompositionLocalProvider(LocalNavController provides navController) {
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
                                        // pluginId+name 全同视为同一插件（10-10 用户令改双条件，
                                        // 与导入面 saveJsDirect / 朗读规则 SpeechRuleManagerActivity
                                        // 同口径）：保存时若已有同 pluginId 同 name 且主键不同的条目，
                                        // 借旧主键 REPLACE 覆盖；仅 id 同名字不同 = 不同插件，并存。
                                        // 编辑本体（主键相同）与全新 pluginId 照旧直插。
                                        val entity = if (it.pluginId.isNotBlank()) {
                                            dbm.pluginDao.getByPluginIdAndName(it.pluginId, it.name)
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
}