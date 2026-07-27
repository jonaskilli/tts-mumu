package com.github.jing332.database.entities.systts.source

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class LocalTtsParameter(val type: String, val key: String, val value: String) : Parcelable {
    companion object {
        const val TYPE_BOOL: String = "Boolean"
        const val TYPE_INT: String = "Int"
        const val TYPE_FLOAT: String = "Float"
        const val TYPE_STRING: String = "String"


        val typeList = listOf(
            TYPE_BOOL,
            TYPE_INT,
            TYPE_FLOAT,
            TYPE_STRING
        )
    }

    fun putValueFromBundle(b: Bundle) {
        when (type) {
            TYPE_BOOL -> b.putBoolean(key, value.toBoolean())
            TYPE_INT -> b.putInt(key, value.toInt())
            TYPE_FLOAT -> b.putFloat(key, value.toFloat())
            else -> b.putString(key, value)
        }
    }
}