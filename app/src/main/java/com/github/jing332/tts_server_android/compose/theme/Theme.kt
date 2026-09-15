package com.github.jing332.tts_server_android.compose.theme

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.github.jing332.tts_server_android.conf.AppConfig

/**
 * 获取当前主题
 */
@Composable
fun appTheme(
    themeType: AppTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    context: Context = LocalContext.current,
): ColorScheme {
    val base = when (themeType) {
        AppTheme.DEFAULT -> defaultTheme(darkTheme)
        AppTheme.DYNAMIC_COLOR -> dynamicColorTheme(darkTheme, context)
        AppTheme.GREEN -> greenTheme(darkTheme)
        AppTheme.RED -> redTheme(darkTheme)
        AppTheme.PINK -> pinkTheme(darkTheme)
        AppTheme.BLUE -> blueTheme(darkTheme)
        AppTheme.CYAN -> cyanTheme(darkTheme)
        AppTheme.ORANGE -> orangeTheme(darkTheme)
        AppTheme.PURPLE -> purpleTheme(darkTheme)
        AppTheme.BROWN -> brownTheme(darkTheme)
        AppTheme.GRAY -> grayTheme(darkTheme)
    }
    // 动态取色（Android 12+ 壁纸派生）：中性色本就与彩色同种子派生、整族和谐，不覆写
    return if (themeType == AppTheme.DYNAMIC_COLOR) base else themedNeutral(base, darkTheme)
}

/**
 * 补齐 M3 1.2 新增、而 Color2.kt 没生成的中性容器槽（surfaceContainer 系 + surfaceDim/Bright）。
 *
 * 为什么必须补：这 7 个槽缺失会回落库默认的淡紫白，跟各主题自己的暖底同屏两种色相。
 * 取各主题的 surface 当底、再掺一点该主题 primary ⇒ 每个主题一套自己的底色。
 * （此前是 10 个主题统一米豆绿，被目目推翻：各主题底色本就不同，不许强改成默认底色。）
 *
 * ⚠️ 只补这 7 个槽，background/surface/outline/inverse* 一律透传各主题原值——
 * 顺手覆写会把各主题自己的色相抹平，那正是要避免的。
 */
private fun themedNeutral(scheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    // 相对 surface 的通道偏移：Low / Container / High / Highest / Dim / Bright / Lowest
    val d = if (!darkTheme) intArrayOf(-4, -8, -14, -20, -28, -2, 5)
    else intArrayOf(8, 12, 23, 34, 0, 38, -5)
    fun slot(i: Int) = tinted(scheme.surface, scheme.primary, d[i].toFloat())
    return scheme.copy(
        surfaceContainerLow = slot(0),
        surfaceContainer = slot(1),
        surfaceContainerHigh = slot(2),
        surfaceContainerHighest = slot(3),
        surfaceDim = slot(4),
        surfaceBright = slot(5),
        surfaceContainerLowest = slot(6),
    )
}

/** 主色掺入比例：够各主题区分色相，又不至于把中性底染成彩块 */
private const val NEUTRAL_TINT = 0.04f

/**
 * 以 surface 为底，各通道平移 [delta] 定明度阶梯，再掺 [NEUTRAL_TINT] 比例的 primary 带上主题色相；
 * 掺色会压暗，最后把整体亮度拉回平移后的基准，否则各槽之间的明度关系就乱了。
 */
private fun tinted(surface: Color, primary: Color, delta: Float): Color {
    val s = floatArrayOf(surface.red, surface.green, surface.blue)
    val p = floatArrayOf(primary.red, primary.green, primary.blue)
    val base = FloatArray(3) { (s[it] * 255f + delta).coerceIn(0f, 255f) }
    val mixed = FloatArray(3) { base[it] + (p[it] * 255f - base[it]) * NEUTRAL_TINT }
    val back = (base.sum() - mixed.sum()) / 3f
    return Color(
        ((mixed[0] + back) / 255f).coerceIn(0f, 1f),
        ((mixed[1] + back) / 255f).coerceIn(0f, 1f),
        ((mixed[2] + back) / 255f).coerceIn(0f, 1f),
    )
}

//全局主题状态
private val themeTypeState: MutableState<AppTheme> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    mutableStateOf(AppTheme.DEFAULT)
}

@Composable
private fun InitTheme() {
    val theme = try {
        AppConfig.theme.value
    } catch (e: Exception) {
        e.printStackTrace()
        AppTheme.DEFAULT
    }
    setAppTheme(themeType = theme)
}

/**
 * 设置主题
 */
fun setAppTheme(themeType: AppTheme) {
    themeTypeState.value = themeType
    AppConfig.theme.value = themeType
}

/**
 * 获取当前主题
 */
fun getAppTheme(): AppTheme = themeTypeState.value

/**
 * 根Context
 */
@Suppress("DEPRECATION")
@Composable
fun AppTheme(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    //初始化主题
    InitTheme()

    //获取当前主题
    val targetTheme = appTheme(themeType = themeTypeState.value)
    val activity = LocalView.current.context as ComponentActivity

    MaterialTheme(
        colorScheme = themeAnimation(targetTheme = targetTheme),
        typography = Typography
    ) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
