package com.github.jing332.tts_server_android

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.github.jing332.compose.widgets.InterceptorResult
import com.github.jing332.compose.widgets.ModelInterceptor

data class PackageDrawable(val packageName: String, val id: Int)

object AsyncImageInterceptor : ModelInterceptor {
    private fun modelInterceptor(context: Context, model: Any?): Pair<Any?, Int?>? {
        return when (model) {
            is CharSequence -> {
                when (model.toString().lowercase()) {
                    "male" -> R.drawable.male to R.string.male
                    "female" -> R.drawable.female to R.string.female
                    else -> null
                }
            }

            is PackageDrawable -> {
//                context.packageManager.crea
                try {
                    val ctx = context.createPackageContext(
                        model.packageName,
                        Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                    )
                    AppCompatResources.getDrawable(ctx, model.id) to null
                } catch (_: Exception) {
                    null
                }
            }

            else -> null
        }
    }


    override fun apply(
        context: Context,
        model: Any?,
        contentDescription: String?,
    ): InterceptorResult {
        val ret = modelInterceptor(context, model)
        val m = if (ret == null) model else ret.first
        var desc: String? = null
        ret?.second?.let { desc = context.getString(it) }

        return InterceptorResult(m, desc)
    }
}