package com.github.jing332.compose.widgets

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap

// https://gist.github.com/tkuenneth/ddf598663f041dc79960cda503d14448?permalink_comment_id=4660486#gistcomment-4660486
@Composable
fun adaptiveIconPainterResource(@DrawableRes id: Int): Painter {
    val res = LocalContext.current.resources
    val theme = LocalContext.current.theme

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // A8
        // Android O supports adaptive icons
        val adaptiveIcon = ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
        if (adaptiveIcon != null)
            BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
        else
            painterResource(id)
    } else
        painterResource(id)
}

@Composable
fun AppLauncherIcon(modifier: Modifier, @DrawableRes resourceId: Int) {
    Image(
        modifier = modifier.clip(CircleShape),
        painter = adaptiveIconPainterResource(resourceId),
        contentDescription = "LOGO"
    )
}