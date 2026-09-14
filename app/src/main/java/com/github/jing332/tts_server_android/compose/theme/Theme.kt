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
    val scheme = when (themeType) {
        // 动态取色（Android 12+ 壁纸派生）不覆写：它的中性色本就与彩色同种子派生、整族和谐，
        // 强行替换成基线中性反而制造色相断裂；其余手写主题统一套官方基线中性
        AppTheme.DEFAULT -> defaultTheme(darkTheme)
        AppTheme.DYNAMIC_COLOR -> dynamicColorTheme(darkTheme, context)
        AppTheme.GREEN -> baselineNeutral(greenTheme(darkTheme), darkTheme)
        AppTheme.RED -> baselineNeutral(redTheme(darkTheme), darkTheme)
        AppTheme.PINK -> baselineNeutral(pinkTheme(darkTheme), darkTheme)
        AppTheme.BLUE -> baselineNeutral(blueTheme(darkTheme), darkTheme)
        AppTheme.CYAN -> baselineNeutral(cyanTheme(darkTheme), darkTheme)
        AppTheme.ORANGE -> baselineNeutral(orangeTheme(darkTheme), darkTheme)
        AppTheme.PURPLE -> baselineNeutral(purpleTheme(darkTheme), darkTheme)
        AppTheme.BROWN -> baselineNeutral(brownTheme(darkTheme), darkTheme)
        AppTheme.GRAY -> baselineNeutral(grayTheme(darkTheme), darkTheme)
    }
    return scheme
}

/**
 * 中性色统一为 MD3 官方基线值（目目 09-14 定案，推翻 0e76bad 作废时保留的"原版观感"）：
 * 10 个手写主题由老生成器产出，只带 15 枚旧 token——surface/background/surfaceVariant 走种子
 * 派生（绿种子下页面泛绿），而 M3 1.2 新增的 surfaceContainer 系没跟种子、回落库默认基线紫，
 * 结果"页面绿、卡片弹窗紫"，一个屏幕两种色相。现把 neutral 家族整体对齐官方基线：
 * 页面/子分组/卡片/弹窗同一族灰白（微紫调），各主题只保留自己的彩色（primary/secondary/
 * tertiary 及其容器），换任何配色都是同一套干净底色。
 * 数值 = material 3 官方 baseline light/dark scheme（含 surfaceDim/Bright/Lowest 等全部中性槽）。
 */
private fun baselineNeutral(scheme: ColorScheme, darkTheme: Boolean): ColorScheme =
    if (!darkTheme) scheme.copy(
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        background = Color(0xFFFEF7FF),
        onBackground = Color(0xFF1D1B20),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        surfaceDim = Color(0xFFDED8E1),
        surfaceBright = Color(0xFFFEF7FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
    ) else scheme.copy(
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E0E9),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        surfaceDim = Color(0xFF141218),
        surfaceBright = Color(0xFF3B383E),
        surfaceContainerLowest = Color(0xFF0F0D13),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        inverseSurface = Color(0xFFE6E0E9),
        inverseOnSurface = Color(0xFF1D1B20),
    )

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