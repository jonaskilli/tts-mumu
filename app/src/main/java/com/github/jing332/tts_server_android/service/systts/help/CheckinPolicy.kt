package com.github.jing332.tts_server_android.service.systts.help

/**
 * 签到日界判据（10-10，对账 TOP5 第 5 条 / 插件 2026-10-02 真实缺陷防御）。
 *
 * 两条铁律（蓝本 = DSH 插件 auto-checkin.ts / zcode-upstream.ts，本文件只收口判据，
 * 行为在各调用方）：
 *
 * A. **UTC+8 日界**：各渠道每日额度按 UTC+8 结算，「今天」必须算术平移取 UTC+8 的
 *    YYYY-MM-DD，不能用本机时区——用户出差/改系统时区时，取本机时区偏东会提前把当天
 *    记成已签（真的漏签）、偏西会一天跑两次（插件 utc8DateString 同款结论）。
 *
 * B. **coversToday**：「查到已签」≠「今天已签」——qoder 活动每日 10:00（UTC+8）才刷新，
 *    上午查到的 already-claimed 属于**昨天**，照常记账 = 当天整天静默漏领（插件
 *    ClaimOutcome.coversToday 同款语义，插件注释原文定位的真实缺陷）。本 app 的对应
 *    判据：签到成功后的 lastCheckinDate（UTC+8 日期串）只有 === utc8Today() 才算
 *    今天已签；上游返回的「今日已签」文案不作为记账依据。
 */
object CheckinPolicy {

    /** UTC+8 偏移毫秒（照插件 model-queue.ts QODER_BILLING_UTC_OFFSET_MS = 8 * 3_600_000） */
    const val UTC8_OFFSET_MS = 8 * 3_600_000L

    /**
     * UTC+8 的「今天」（YYYY-MM-DD）。
     * 算术平移：now + 8h 后取 UTC 日历日期（与插件 `new Date(nowMs + OFFSET).toISOString().slice(0,10)`
     * 逐字对应），不读本机时区。
     */
    fun utc8Today(now: Long = System.currentTimeMillis()): String =
        java.time.Instant.ofEpochMilli(now + UTC8_OFFSET_MS).toString().take(10) // ISO 前缀即 YYYY-MM-DD

    /**
     * checkedAt 那次签到算不算「今天（UTC+8）」。
     * 供「签到成功后记账」与「启动/闹钟补签判断」两处用：checkedAt 按 UTC+8 取日期
     * 与 utc8Today 比较；<=0（从未签到）不算。
     */
    fun coversToday(checkedAtMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (checkedAtMs <= 0L) return false
        return utc8Today(checkedAtMs) == utc8Today(now)
    }
}
