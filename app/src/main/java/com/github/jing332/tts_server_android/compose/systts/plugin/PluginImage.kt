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
        modifier = modifier.size(32.dp),
        error = {
            CenterTextImage(name.getOrElse(0) { '-' }.toString())
        }
    )
}