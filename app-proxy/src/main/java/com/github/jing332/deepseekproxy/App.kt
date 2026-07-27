package com.github.jing332.deepseekproxy

import android.app.Application

/**
 * 独立「混元太极」APP 的 Application 入口。
 * App 重启后按上次状态自动恢复混元太极服务（与转发器一致）。
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 混元太极：若上次为「已开启」状态，App 重启后自动按原状态恢复服务
        if (ProxyService.isSavedRunning(this)) {
            ProxyService.startFromSaved(this)
        }
    }
}
