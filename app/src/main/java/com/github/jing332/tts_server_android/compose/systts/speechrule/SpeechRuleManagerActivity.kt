package com.github.jing332.tts_server_android.compose.systts.speechrule

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

class SpeechRuleManagerActivity : ComposeActivity() {
    private var jsCode by mutableStateOf("")

    @Suppress("DEPRECATION")
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
                            sharedVM.put(
                                NavRoutes.SpeechRuleEdit.KEY_DATA, SpeechRule(code = jsCode)
                            )
                            navController.navigate(NavRoutes.SpeechRuleEdit.id)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.SpeechRuleManager.id
                    ) {
                        composable(NavRoutes.SpeechRuleManager.id) {
                            SpeechRuleManagerScreen(sharedVM) { finishAfterTransition() }
                        }

                        composable(NavRoutes.SpeechRuleEdit.id) {
                            val scope = rememberCoroutineScope()
                            val rule = remember {
                                sharedVM.getOnce<SpeechRule>(NavRoutes.SpeechRuleEdit.KEY_DATA)
                                    ?: SpeechRule()
                            }
                            // 第11项: 列表项"运行键"传入的自动调试标志
                            val autoDebug = remember {
                                sharedVM.getOnce<Boolean>("autoDebug") ?: false
                            }
                            SpeechRuleEditScreen(rule, autoDebug = autoDebug, onSave = {
                                scope.launch { withIO { dbm.speechRuleDao.insert(it) } }
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