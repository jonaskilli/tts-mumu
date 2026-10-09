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
import kotlin.math.cbrt
import kotlin.math.pow
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
    return if (themeType == AppTheme.DYNAMIC_COLOR) dynamicNeutral(base, darkTheme) else themedNeutral(base, themeType, darkTheme)
}

/**
 * surfaceContainer 七槽——M3 官方 baseline tone 阶梯（2026-10-10 起 replacing 自创偏移表）。
 *
 * 旧法：surface 各通道 ±固定值再掺 4% primary——相邻槽只差 2~4 个 RGB 灰阶，
 * 与页底/列表卡/设置卡/底栏全挤在 3% 亮度带里，层级肉眼读不出（用户实测「层级关系一般」）。
 * 新法：tone 抄官方表（浅 100/96/94/92/90/87/98、深 4/10/12/17/22/6/24，Material Theme Builder 同款），
 * 层级差来自官方表本身（相邻槽 2~5 个 L* 步长）；静态主题查 SurfaceTones.kt 预生成表，
 * 深色锚到各主题 background（官方锚 surface=tone6，本 app 页底=background），其余原样。
 */
private fun themedNeutral(
    scheme: ColorScheme,
    themeType: AppTheme,
    darkTheme: Boolean,
): ColorScheme {
    val tones = (if (darkTheme) darkSurfaceTones else lightSurfaceTones)[themeType]
        ?: return scheme
    return scheme.copy(
        surfaceContainerLowest = tones[0],
        surfaceContainerLow = tones[1],
        surfaceContainer = tones[2],
        surfaceContainerHigh = tones[3],
        surfaceContainerHighest = tones[4],
        surfaceDim = tones[5],
        surfaceBright = tones[6],
    )
}

/**
 * 动态取色（壁纸派生）没有静态表可查：按同一官方 tone 结构现算，
 * 色相/彩度取壁纸派生的 primary（彩度截到 3，与静态主题同中性度），锚到派生 background。
 */
private fun dynamicNeutral(scheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    val steps = if (darkTheme)
        floatArrayOf(4f, 10f, 12f, 17f, 22f, 6f, 24f)
    else
        floatArrayOf(100f, 96f, 94f, 92f, 90f, 87f, 98f)
    val bg = labL(scheme.background)
    val shift = bg - (if (darkTheme) 6f else 98f)   // 官方锚：surface 深色 tone6 / 浅色 98
    val p = scheme.primary
    val (_, pa, pb) = labOf(p.red, p.green, p.blue)
    val chroma = minOf(Math.hypot(pa.toDouble(), pb.toDouble()).toFloat(), 3f)
    val hue = Math.atan2(pb.toDouble(), pa.toDouble())
    fun tone(t: Float) = labColor((t + shift).coerceIn(0f, 100f), (chroma * Math.cos(hue)).toFloat(), (chroma * Math.sin(hue)).toFloat())
    return scheme.copy(
        surfaceContainerLowest = tone(steps[0]),
        surfaceContainerLow = tone(steps[1]),
        surfaceContainer = tone(steps[2]),
        surfaceContainerHigh = tone(steps[3]),
        surfaceContainerHighest = tone(steps[4]),
        surfaceDim = tone(steps[5]),
        surfaceBright = tone(steps[6]),
    )
}

// ---- CIELAB（D65）最小实现：tone 即 L*，供动态取色现算 ----
private fun labL(color: Color): Float = labOf(color.red, color.green, color.blue)[0]

private fun labOf(r: Float, g: Float, b: Float): FloatArray {
    fun lin(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    fun f(t: Float) = if (t > 216f / 24389f) cbrt(t) else (24389f / 27f * t + 16f) / 116f
    val rl = lin(r); val gl = lin(g); val bl = lin(b)
    val x = (0.4124564f * rl + 0.3575761f * gl + 0.1804375f * bl) / 0.95047f
    val y = 0.2126729f * rl + 0.7151522f * gl + 0.0721750f * bl
    val z = (0.0193339f * rl + 0.1191920f * gl + 0.9503041f * bl) / 1.08883f
    val fx = f(x); val fy = f(y); val fz = f(z)
    return floatArrayOf(116f * fy - 16f, 500f * (fx - fy), 200f * (fy - fz))
}

private fun labColor(L: Float, a: Float, b: Float): Color {
    val fy = (L + 16f) / 116f
    val fx = fy + a / 500f
    val fz = fy - b / 200f
    fun finv(t: Float): Float {
        val t3 = t * t * t
        return if (t3 > 216f / 24389f) t3 else (116f * t - 16f) / (24389f / 27f)
    }
    val x = finv(fx) * 0.95047f
    val y = finv(fy)
    val z = finv(fz) * 1.08883f
    var rl = 3.2404542f * x - 1.5371385f * y - 0.4985314f * z
    var gl = -0.9692660f * x + 1.8760108f * y + 0.0415560f * z
    var bl = 0.0556434f * x - 0.2040259f * y + 1.0572252f * z
    fun gam(c: Float): Float {
        val v = c.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) 12.92f * v else 1.055f * v.pow(1f / 2.4f) - 0.055f
    }
    return Color(gam(rl), gam(gl), gam(bl))
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
