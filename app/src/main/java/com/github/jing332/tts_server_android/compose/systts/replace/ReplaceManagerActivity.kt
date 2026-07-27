package com.github.jing332.tts_server_android.compose.systts.replace

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.replace.edit.RuleEditScreen
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.service.systts.SystemTtsService

class ReplaceManagerActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val sharedVM: SharedViewModel = viewModel()
                CompositionLocalProvider(LocalNavController provides navController) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Manager.id
                    ) {
                        composable(NavRoutes.Manager.id) {
                            ReplaceRuleManagerScreen(sharedVM) { finishAfterTransition() }
                        }
                        composable(NavRoutes.Edit.id) { stackEntry ->
                            var rule by rememberSaveable {
                                mutableStateOf(
                                    sharedVM.getOnce(NavRoutes.Edit.KEY_DATA) ?: ReplaceRule()
                                )
                            }
                            RuleEditScreen(rule, onRuleChange = { rule = it }, onSave = {
                                dbm.replaceRuleDao.insert(rule)
                                if (rule.isEnabled)
                                    SystemTtsService.notifyUpdateConfig(isOnlyReplacer = true)
                            })
                        }
                    }
                }
            }
        }
    }
}