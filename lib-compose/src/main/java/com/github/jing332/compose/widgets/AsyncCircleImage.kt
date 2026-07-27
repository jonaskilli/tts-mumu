package com.github.jing332.compose.widgets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class InterceptorResult(val model: Any?, val contentDescription: Any?)

object AsyncCircleImageSettings {
    val defaultInterceptor by lazy {
        object : ModelInterceptor {
            override fun apply(
                context: Context,
                model: Any?,
                contentDescription: String?,
            ): InterceptorResult {
                return InterceptorResult(model, contentDescription)
            }
        }
    }


    var interceptor: ModelInterceptor = defaultInterceptor
}

fun interface ModelInterceptor {
    fun apply(context: Context, model: Any?, contentDescription: String?): InterceptorResult
}


@Composable
fun AsyncCircleImage(
    model: Any? = null,
    contentDescription: String? = null,
    interceptor: ModelInterceptor? = AsyncCircleImageSettings.interceptor,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.size(32.dp),
) {
    val context = LocalContext.current
    val result = remember(model) { interceptor?.apply(context, model, contentDescription) }
    val desc: String? = remember(result) {
        val desc = result?.contentDescription
        return@remember try {
            when (desc) {
                is CharSequence -> desc.toString()
                is Int -> context.getString(desc)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    AsyncImage(
        result?.model,
        desc,
        modifier = modifier
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}