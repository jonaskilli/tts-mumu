package com.github.jing332.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

object DecimalUtils {
}

fun Float.toScale(scale: Int = 2) =
    BigDecimal(this.toDouble()).setScale(scale, RoundingMode.HALF_UP).toFloat()

/**
 * 音频参数值显示文本（用户 09-10 定稿）：**按实际精度显示**——
 * 最多 2 位小数，末尾无意义的 0 去掉，但至少保留 1 位，避免整数看不出是倍率：
 * 1.00 → "1.0"、1.50 → "1.5"、0.97 → "0.97"、1.04 → "1.04"、2.00 → "2.0"。
 *
 * 卡片参数行 / 音频参数弹窗 / 日志快捷面板 / 编辑页内嵌终值行 / 试听弹窗 /
 * 日志发音人信息 六处统一走此函数，保证同一个值在任何地方显示完全一致。
 */
fun Float.toParamText(): String {
    val s = "%.2f".format(this)
    val trimmed = s.trimEnd('0')
    return if (trimmed.endsWith(".")) trimmed + "0" else trimmed
}