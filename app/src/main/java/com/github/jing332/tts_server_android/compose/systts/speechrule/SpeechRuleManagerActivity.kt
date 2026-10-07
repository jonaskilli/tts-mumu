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
    // 由外部传入的待运行规则数据库 id（角色管理界面"运行朗读规则"快捷键使用）
    private var ruleDbId by mutableStateOf<Long?>(null)
    private var autoDebug by mutableStateOf(false)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null) {
            importRuleFromIntent(intent)
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val sharedVM: SharedViewModel = viewModel()
                CompositionLocalProvider(LocalNavController provides navController) {
                    // 角色管理快捷键：按 id 取出规则并跳转编辑页运行
                    LaunchedEffect(ruleDbId) {
                        val id = ruleDbId
                        if (id != null) {
                            val rule = withIO { dbm.speechRuleDao.all.find { it.id == id } }
                            if (rule != null) {
                                sharedVM.put(NavRoutes.SpeechRuleEdit.KEY_DATA, rule)
                                if (autoDebug) sharedVM.put("autoDebug", true)
                                navController.navigate(NavRoutes.SpeechRuleEdit.id)
                            }
                            ruleDbId = null
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
                                scope.launch {
                                    withIO {
                                        // ruleId+name 全同视为同一条规则（10-10 用户令改双条件）：
                                        // 保存时若已有同 ruleId 同 name 且主键不同的条目，借旧主键
                                        // REPLACE 覆盖；仅 id 同名字不同（如 M 直连与 1008 主线共用
                                        // mingwuyan）= 不同规则，并存不再静默顶掉。与导入面
                                        // saveJsDirect 同口径。编辑本体（主键相同）与全新 ruleId 照旧直插。
                                        val entity = if (it.ruleId.isNotBlank()) {
                                            dbm.speechRuleDao.getByRuleIdAndName(it.ruleId, it.name)
                                                ?.takeIf { twin -> twin.id != it.id }
                                                ?.let { twin -> it.copy(id = twin.id) } ?: it
                                        } else it
                                        dbm.speechRuleDao.insert(entity)
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

        importRuleFromIntent(intent)
    }

    // 角色管理快捷键传入：ruleDbId 指定要运行的规则, autoDebug 控制是否自动运行
    private fun importRuleFromIntent(intent: Intent) {
        val id = intent.getLongExtra("ruleDbId", -1L)
        if (id > 0L) {
            ruleDbId = id
            autoDebug = intent.getBooleanExtra("autoDebug", false)
            intent.removeExtra("ruleDbId")
            intent.removeExtra("autoDebug")
        }
    }
}