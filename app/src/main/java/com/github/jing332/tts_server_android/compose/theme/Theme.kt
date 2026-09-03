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
import androidx.compose.ui.graphics.lerp
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
    // 动态取色自带全套色槽；手写配色补齐 M3 1.2 新增的 surfaceContainer 系
    return if (themeType == AppTheme.DYNAMIC_COLOR) scheme
    else scheme.deriveSurfaceContainers(darkTheme)
}

/**
 * 补齐 M3 1.2 新增的 surfaceContainer 系色槽（surfaceDim/Bright/ContainerLowest~Highest）。
 * 手写的 lightColorScheme/darkColorScheme 只覆盖了旧 29 槽，新槽留的是 M3 基线默认值——
 * 淡紫底！ElevatedCard(配置项卡片=surfaceContainerHighest)与 NavigationBar(底栏=
 * surfaceContainer)因此与绿色主题脱节。按 MD3 表面海拔规则用 surface 与 surfaceTint
 * 插值派生：海拔越高 tint 占比越大；明/暗侧的 Dim/Bright 分别向黑/白微调。
 */
private fun ColorScheme.deriveSurfaceContainers(darkTheme: Boolean): ColorScheme {
    val base = surface
    val tint = surfaceTint
    return copy(
        surfaceDim = if (darkTheme) lerp(base, Color.Black, 0.06f) else lerp(base, Color.Black, 0.05f),
        surfaceBright = if (darkTheme) lerp(base, Color.White, 0.08f) else base,
        surfaceContainerLowest = lerp(base, if (darkTheme) Color.Black else Color.White, 0.04f),
        // 5%→3%：用户反馈分区面板/菜单/底栏这一档还是偏深，再降一档贴近页面底色
        surfaceContainerLow = lerp(base, tint, 0.03f),
        surfaceContainer = lerp(base, tint, 0.09f),
        surfaceContainerHigh = lerp(base, tint, 0.13f),
        surfaceContainerHighest = lerp(base, tint, 0.17f),
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