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
        // 「默认」与「绿色」共用米豆绿中性族（目目 09-14 二次定案）：
        // 官方灰白 #FEF7FF 作底"太白太亮还偏冷紫"，指定换回默认主题那套米豆绿底；
        // 绿色主题色本身很好、不动，只换底与容器色
        AppTheme.DEFAULT -> beanNeutral(defaultTheme(darkTheme), darkTheme)
        AppTheme.DYNAMIC_COLOR -> dynamicColorTheme(darkTheme, context)
        AppTheme.GREEN -> beanNeutral(greenTheme(darkTheme), darkTheme)
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
 * 例外：默认 / 绿色两主题走 beanNeutral（米豆绿族，目目 09-14 指定，见下），不套这套纯灰白。
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

/**
 * 米豆绿同族中性色（目目 09-14 二次定案）：「默认」与「绿色」两个主题共用。
 * 起因：官方基线灰白（#FEF7FF，微紫）大面积铺底"太白太亮"，目目指定换回
 * 默认主题原本的米豆绿底（surface #FAFAF3 / background #FDFDF6）。
 * 老生成器只给这两个主题 15 枚旧 token，surfaceContainer 五档回落库默认紫，
 * 于是"底绿、卡片弹窗紫"断层——这里按 MD3 亮度梯度把缺的几档补成同族。
 * 与 baselineNeutral 的区别：那套是官方纯灰白，这套带主题绿调、更柔和。
 * 彩色槽（primary/secondary/tertiary 及其容器）一律不动：绿色是主题色。
 */
private fun beanNeutral(scheme: ColorScheme, darkTheme: Boolean): ColorScheme =
    if (!darkTheme) scheme.copy(
        surface = Color(0xFFFAFAF3),
        onSurface = Color(0xFF1A1C18),
        surfaceVariant = Color(0xFFDFE4D7),
        onSurfaceVariant = Color(0xFF43483E),
        background = Color(0xFFFDFDF6),
        onBackground = Color(0xFF1A1C18),
        outline = Color(0xFF73796D),
        outlineVariant = Color(0xFFC3C8BB),
        surfaceDim = Color(0xFFDEDED6),
        surfaceBright = Color(0xFFFAFAF3),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F6EF),
        surfaceContainer = Color(0xFFF2F2EA),
        surfaceContainerHigh = Color(0xFFECECE4),
        surfaceContainerHighest = Color(0xFFE6E6DE),
        inverseSurface = Color(0xFF2F312D),
        inverseOnSurface = Color(0xFFF1F1EA),
    ) else scheme.copy(
        surface = Color(0xFF121410),
        onSurface = Color(0xFFC6C7C0),
        surfaceVariant = Color(0xFF43483E),
        onSurfaceVariant = Color(0xFFC3C8BB),
        background = Color(0xFF1A1C18),
        onBackground = Color(0xFFE3E3DC),
        outline = Color(0xFF8D9287),
        outlineVariant = Color(0xFF43483E),
        surfaceDim = Color(0xFF121410),
        surfaceBright = Color(0xFF383A35),
        surfaceContainerLowest = Color(0xFF0D0F0B),
        surfaceContainerLow = Color(0xFF1A1C18),
        surfaceContainer = Color(0xFF1E201C),
        surfaceContainerHigh = Color(0xFF292B26),
        surfaceContainerHighest = Color(0xFF343631),
        inverseSurface = Color(0xFFE3E3DC),
        inverseOnSurface = Color(0xFF1A1C18),
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