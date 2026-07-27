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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jing332.common.utils.toast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.ShortCuts
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsEditContainerScreen
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

val LocalNavController = compositionLocalOf<NavHostController> { error("No nav controller") }
val LocalDrawerState = compositionLocalOf<DrawerState> { error("No drawer state") }
val LocalUpdateCheckTrigger =
    staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

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
                var showAutoCheckUpdaterDialog by remember { mutableStateOf(false) }
                val updateCheckTrigger = LocalUpdateCheckTrigger.current
                if (showAutoCheckUpdaterDialog) {
                    // 👈 改用原生 Log
                    Log.i(TAG, "Check for update") 
                    AutoUpdateCheckerDialog(updateCheckTrigger.value, fromGithubAction = true) {
                        showAutoCheckUpdaterDialog = false
                        updateCheckTrigger.value = false
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // A13
                    val notificationPermission =
                        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                    if (!notificationPermission.status.isGranted) {
                        LaunchedEffect(notificationPermission) {
                            notificationPermission.launchPermissionRequest()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    showAutoCheckUpdaterDialog = AppConfig.isAutoCheckUpdateEnabled.value
                }

                val excludeFromRecent by AppConfig.isExcludeFromRecent
                LaunchedEffect(excludeFromRecent) {
                    (application.getSystemService(ACTIVITY_SERVICE) as ActivityManager).let { manager ->
                        manager.appTasks.forEach { task ->
                            task?.setExcludeFromRecents(excludeFromRecent)
                        }
                    }
                }

                LaunchedEffect(updateCheckTrigger.value) {
                    if (updateCheckTrigger.value) showAutoCheckUpdaterDialog = true
                }

                MainScreen { finish() }
            }
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
                        println("UpdateSystemTTS: $it")
                    },
                    onSave = {
                        navController.popBackStack()
                        dbm.systemTtsV2.insert(stateSystemTts)
                        if (stateSystemTts.isEnabled) SystemTtsService.notifyUpdateConfig()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
