package com.github.jing332.tts_server_android.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R

/**
 * 厂商后台白名单工具类
 * 提供跳转到各厂商后台管理页面的功能
 */
object BackgroundWhitelistUtils {

    /**
     * 检查是否在电池优化白名单中
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * 请求电池优化白名单
     */
    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizationWhitelist(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isIgnoringBatteryOptimizations(context)) {
                context.toast(R.string.added_battery_optimization_whitelist)
            } else {
                kotlin.runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }.onFailure {
                    context.toast(R.string.system_not_support_please_manual_set)
                }
            }
        }
    }

    /**
     * 获取厂商类型
     */
    private fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.lowercase()
    }

    /**
     * 跳转到厂商后台管理页面
     * 引导用户将应用加入白名单
     */
    fun openManufacturerBatterySettings(context: Context): Boolean {
        val manufacturer = getDeviceManufacturer()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                openXiaomiSettings(context)
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                openHuaweiSettings(context)
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                openOppoSettings(context)
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                openVivoSettings(context)
            }
            manufacturer.contains("samsung") -> {
                openSamsungSettings(context)
            }
            manufacturer.contains("meizu") -> {
                openMeizuSettings(context)
            }
            manufacturer.contains("oneplus") -> {
                openOnePlusSettings(context)
            }
            else -> {
                // 通用设置
                openGenericSettings(context)
            }
        }
    }

    /**
     * 小米/红米 - 后台设置
     */
    private fun openXiaomiSettings(context: Context): Boolean {
        return try {
            // 尝试打开应用详情页的自启动管理
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                // 备用：打开应用详情
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 华为/荣耀 - 后台设置
     */
    private fun openHuaweiSettings(context: Context): Boolean {
        return try {
            // 尝试打开电池优化设置
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                // 备用方案：启动管理
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * OPPO/Realme - 后台设置
     */
    private fun openOppoSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * Vivo/iQOO - 后台设置
     */
    private fun openVivoSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.iqoo.daemon",
                    "com.iqoo.daemon.ui.BgPowerManagerActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.vivo.abe",
                        "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * 三星 - 后台设置
     */
    private fun openSamsungSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * 魅族 - 后台设置
     */
    private fun openMeizuSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.powerui.PowerAppPermissionActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.powerui.AppPowerManagerActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * 一加 - 后台设置
     */
    private fun openOnePlusSettings(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppBatteryUsageActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                openGenericSettings(context)
            }
        }
    }

    /**
     * 通用设置页面
     */
    private fun openGenericSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查并提示用户设置自启动
     */
    fun checkAndPromptAutoStart(context: Context, onPrompt: (() -> Unit)? = null) {
        if (!isIgnoringBatteryOptimizations(context)) {
            onPrompt?.invoke()
        }
    }
}