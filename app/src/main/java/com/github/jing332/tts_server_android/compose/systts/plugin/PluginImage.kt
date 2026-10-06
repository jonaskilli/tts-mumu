package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.github.jing332.compose.widgets.CenterTextImage

@Composable
fun PluginImage(modifier: Modifier = Modifier, model: Any?, name: String) {
    SubcomposeAsyncImage(
        model,
        null,
        contentScale = ContentScale.Crop,
        // 28dp（10-06 用户拍板方案 C）：原 32dp 圆配 15sp 插件名偏大——插件名自 3ef36f7
        // 降回 15sp 后圆没跟着收，看着「图标过大」。现圆随名字号一起收，与名字（15sp）
        // 体量相称、又略大一圈（引子 vs 主体）。圆内字形由 CenterTextImage 按占空比推导
        modifier = modifier.size(28.dp),
        error = {
            // 占空比 16/28≈0.57（10-06 方案 C）：插件名 15sp 时圆内字 16sp，与名字体量相称；
            // 默认 0.625 在此圆径下会算出 17.5sp、仍偏撑
            CenterTextImage(
                name.getOrElse(0) { '-' }.toString(),
                size = 28.dp,
                textRatio = 16f / 28f,
            )
        }
    )
}