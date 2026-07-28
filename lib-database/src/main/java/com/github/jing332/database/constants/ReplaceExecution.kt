package com.github.jing332.database.constants

import androidx.annotation.IntDef
import com.github.jing332.database.constants.ReplaceExecution.ReplaceExecution.AFTER
import com.github.jing332.database.constants.ReplaceExecution.ReplaceExecution.BEFORE

/**
 * 替换规则执行时机
 */
@IntDef(BEFORE, AFTER)
@Retention(AnnotationRetention.SOURCE)
annotation class ReplaceExecution {
    companion object ReplaceExecution {
        const val BEFORE = 0 // 朗读规则前
        const val AFTER = 1 // 朗读规则后
    }
}
