package com.github.jing332.tts_server_android.service.keepalive

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.github.jing332.tts_server_android.conf.SysTtsConfig

/**
 * JobScheduler 定时唤醒服务
 * 利用 Android JobScheduler 机制定期唤醒应用，防止被系统杀死
 */
class KeepAliveJobService : JobService() {

    companion object {
        private const val JOB_ID = 1001
        private const val TAG = "KeepAliveJobService"

        // 定时唤醒间隔（毫秒）- 15分钟（JobScheduler 最小间隔限制）
        private const val MIN_INTERVAL_MS = 15 * 60 * 1000L

        /**
         * 调度保活任务
         */
        fun schedule(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            // 如果已经调度，先取消
            cancel(context)

            val componentName = ComponentName(context, KeepAliveJobService::class.java)

            val builder = JobInfo.Builder(JOB_ID, componentName).apply {
                // 设置最小延迟
                setMinimumLatency(5000)
                // 设置超时重试
                setOverrideDeadline(MIN_INTERVAL_MS)
                // 需要网络时执行（可选）
                setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                // 设备充电时执行（降低电量消耗）
                setRequiresCharging(false)
                // 设备空闲时执行
                setRequiresDeviceIdle(false)
                // 持久化，设备重启后仍然有效
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setPersisted(true)
                }
                // 设置退避策略
                setBackoffCriteria(10000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            }

            val result = jobScheduler.schedule(builder.build())
            if (result == JobScheduler.RESULT_SUCCESS) {
                android.util.Log.d(TAG, "Job scheduled successfully")
            } else {
                android.util.Log.e(TAG, "Job scheduling failed")
            }
        }

        /**
         * 取消保活任务
         */
        fun cancel(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        android.util.Log.d(TAG, "Job started")

        // 如果保活服务未运行且用户启用了保活，启动它
        if (!KeepAliveService.isRunning && SysTtsConfig.isKeepAliveEnabled) {
            KeepAliveService.start(this)
        }

        // 重新调度下一次任务
        schedule(this)

        // 任务完成
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        android.util.Log.d(TAG, "Job stopped")
        // 重新调度任务
        return true
    }
}
