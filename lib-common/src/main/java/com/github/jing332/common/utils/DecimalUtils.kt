package com.github.jing332.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

object DecimalUtils {
}

fun Float.toScale(scale: Int = 2) =
    BigDecimal(this.toDouble()).setScale(scale, RoundingMode.HALF_UP).toFloat()