package com.github.jing332.tts_server_android.ui.view

import android.content.Context
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

object Attributes {
    @ColorInt
    fun Context.colorAttr(resId: Int): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(
            resId,
            typedValue,
            true
        )
        return typedValue.data
    }


    @DrawableRes
    fun Context.drawableAttr(resId: Int): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(
            resId,
            typedValue,
            true
        )
        return typedValue.resourceId
    }





    @get:DrawableRes
    val Context.selectableItemBackground: Int
        get() = drawableAttr(android.R.attr.selectableItemBackground)

    @get:DrawableRes
    val Context.selectableItemBackgroundBorderless: Int
        get() = drawableAttr(android.R.attr.selectableItemBackgroundBorderless)

}