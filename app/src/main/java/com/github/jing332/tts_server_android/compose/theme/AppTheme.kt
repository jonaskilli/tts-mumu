package com.github.jing332.tts_server_android.compose.theme

import androidx.compose.ui.graphics.Color
import com.github.jing332.tts_server_android.R

/**
 * 主题选择列表（设置弹窗按此顺序渲染）。
 * 默认主题=GREEN（绿色）居首位；原"默认"实为豆绿配色，改名 BEAN_GREEN 列第三位。
 * 旧存档里的 id=""（原 DEFAULT）由 restore 兜底解析到 GREEN（见 AppConfig.theme）。
 */
enum class AppTheme(val id: String, val stringResId: Int = -1, val color: Color) {
    GREEN("green", R.string.green, green_seed),
    DYNAMIC_COLOR("dynamicColor", R.string.dynamic_color, Color.Unspecified),
    BEAN_GREEN("beanGreen", R.string.theme_bean_green, green_seed),
    RED("red", R.string.red, red_seed),
    PINK("pink", R.string.pink, pink_seed),
    BLUE("blue", R.string.blue, blue_seed),
    CYAN("cyan", R.string.cyan, cyan_seed),
    ORANGE("orange", R.string.orange, orange_seed),
    PURPLE("purple", R.string.purple, purple_seed),
    BROWN("brown", R.string.brown, brown_seed),
    GRAY("gray", R.string.gray, gray_seed),
}