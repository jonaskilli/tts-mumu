@file:Suppress("DEPRECATION")

package com.github.jing332.tts_server_android.compose

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.util.Log // 👈 使用原生日志
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jing332.common.utils.toast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.ShortCuts
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsEditContainerScreen
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import com.drake.net.utils.withIO
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

val LocalNavController = compositionLocalOf<NavHostController> { error("No nav controller") }
val LocalDrawerState = compositionLocalOf<DrawerState> { error("No drawer state") }
// LocalUpdateCheckTrigger 已删（10-05 用户令：应用内更新功能整体退役——
// 更新检查硬编码指向上游 jing332/tts-server-android 的 Actions/Releases，与本 fork 无关）

fun Context.asAppCompatActivity(): AppCompatActivity {
    return this as? AppCompatActivity ?: error("Context is not an AppCompatActivity")
}

fun Context.asActivity(): Activity {
    return this as? Activity ?: error("Context is not an Activity")
}


class MainActivity : ComposeActivity() {
    companion object {
        private const val TAG = "MainActivity"
        // 👈 删除了 logger 定义，避免类加载时崩溃
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShortCuts.buildShortCuts(this)

        // 外部「打开方式」进来的密钥文件（onCreate 冷启动 / onNewIntent 热启动都走这里）
        handleViewIntent(intent)

        // 自动申请管理全部文件权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }

        setContent {
            AppTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // A13
                    val notificationPermission =
                        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                    if (!notificationPermission.status.isGranted) {
                        LaunchedEffect(notificationPermission) {
                            notificationPermission.launchPermissionRequest()
                        }
                    }
                }

                val excludeFromRecent by AppConfig.isExcludeFromRecent
                LaunchedEffect(excludeFromRecent) {
                    (application.getSystemService(ACTIVITY_SERVICE) as ActivityManager).let { manager ->
                        manager.appTasks.forEach { task ->
                            task?.setExcludeFromRecents(excludeFromRecent)
                        }
                    }
                }

                MainScreen { finish() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    /** 外部「打开方式」导入密钥（10-07 用户令）：文件管理器点 json → 选本应用 →
     *  自识别解析（v2 对象 / 插件时代扁平数组）→ 直接合并入密钥库，结果 toast。
     *  不做确认步骤：导入=合并不覆盖、重名自动跳过，无破坏性（与页内一步导入同口径）。 */
    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        intent.data = null // singleTask 复用实例，清掉防重复触发
        lifecycleScope.launch {
            val result = withIO {
                val text = try {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    Log.w(TAG, "key import open failed: ${e.message}")
                    null
                }
                text?.let { KeyListFile.parseExportText(it) }
                    ?.let { KeyListFile.importAll(KeyListFile.DEFAULT_TAG_RULE_ID, it) }
            }
            if (result == null) toast(R.string.role_list_failed)
            else toast(R.string.role_key_import_done, result.first, result.second, result.third)
        }
    }
}

@Composable
private fun MainScreen(finish: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val entryState by navController.currentBackStackEntryAsState()

    var lastBackDownTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = drawerState.isClosed) {
        val duration = 2000
        SystemClock.elapsedRealtime().let {
            if (it - lastBackDownTime <= duration) {
                finish()
            } else {
                lastBackDownTime = it
                context.toast(R.string.app_down_again_to_exit)
            }
        }
    }
    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalDrawerState provides drawerState,
    ) {
        val sharedVM: SharedViewModel = viewModel()
        NavHost(
            navController = navController,
            startDestination = NavRoutes.MainPager.id
        ) {
            composable(NavRoutes.MainPager.id) { MainPager(sharedVM) }


            composable(NavRoutes.TtsEdit.id) {
                val scope = rememberCoroutineScope()
                var stateSystemTts by rememberSaveable {
                    mutableStateOf(
                        checkNotNull(sharedVM.getOnce<SystemTtsV2>(NavRoutes.TtsEdit.DATA)) {
                            "Not found systemTts from sharedVM"
                        }
                    )
                }

                TtsEditContainerScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    systts = stateSystemTts,
                    onSysttsChange = {
                        stateSystemTts = it
                    },
                    onSave = {
                        // 保存回调（采样率/标签名刷新）已在容器层同步跑完，此刻 stateSystemTts 即最终值。
                        // 必须先落库再 popBackStack：本组合的 rememberCoroutineScope 在页面离开组合后被取消，
                        // 若先 pop 再异步落库，协程会在 withIO 挂起点被静默取消，表现为「点了保存但没保存」。
                        val current = stateSystemTts
                        scope.launch {
                            withIO {
                                // 分组兜底：groupId 无效（0 或分组已删除）时归位到默认分组，
                                // 否则插入项挂在不存在的分组下，主列表按分组查询永远不可见。
                                // 默认分组行可能已被用户删除（历史语义：删除后不自动复活），
                                // 此时按需补建真实分组行再保存，避免成为列表查不到的孤儿项
                                val toSave = current.let { tts ->
                                    if (dbm.systemTtsV2.getGroup(tts.groupId) == null) {
                                        if (dbm.systemTtsV2.getGroup(DEFAULT_GROUP_ID) == null) {
                                            dbm.systemTtsV2.insertGroup(
                                                com.github.jing332.database.entities.systts.SystemTtsGroup(
                                                    DEFAULT_GROUP_ID,
                                                    context.getString(R.string.default_group),
                                                    dbm.systemTtsV2.groupCount
                                                )
                                            )
                                        }
                                        tts.copy(groupId = DEFAULT_GROUP_ID)
                                    } else tts
                                }
                                dbm.systemTtsV2.insert(toSave)
                            }
                            if (current.isEnabled) SystemTtsService.notifyUpdateConfig()
                            navController.popBackStack()
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
