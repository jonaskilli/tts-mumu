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
    // 动态取色（Android 12+ 壁纸派生）不覆写：它的中性色本就与彩色同种子派生、整族和谐，
    // 强行替换反而制造色相断裂；其余 10 个手写主题统一套米豆绿中性族（目目 09-14 三次定案）
    return if (themeType == AppTheme.DYNAMIC_COLOR) base else beanNeutral(base, darkTheme)
}

/**
 * 米豆绿同族中性色（目目 09-14 三次定案）：10 个手写主题统一共用（动态取色除外）。
 *
 * 起因：老生成器只给各主题约 15 枚旧 token，M3 1.2 新增的 surfaceContainer 系没跟着走，
 * 统一回落库默认的淡紫白——于是「页面暖、卡片/弹窗冷紫白」一个屏幕两种色相，弹窗尤其
 * 「近乎白、与底打架」（目目指认）。这里把 neutral 家族整体对齐默认档原本那套米豆绿：
 * 页面 #FDFDF6 / 卡片 #F2F2EA / 弹窗 #ECECE4 / 描边 #C3C8BB，同族靠深浅分层；
 * 各主题只保留自己的彩色（primary/secondary/tertiary 及其容器）——换任何配色都是同一套
 * 暖米绿底，不再出现「底绿、卡片弹窗紫」的断裂。
 * 深色档同族（surface #121410 / background #1A1C18，与「默认」档 dark token 一致）。
 *
 * 注：此前那套「官方灰白 #FEF7FF 基线」（baselineNeutral）已作废——作底太白太亮还偏冷紫，
 * 目目否；不再区分「哪几个主题走米豆绿」，10 个主题一律走本族。
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
