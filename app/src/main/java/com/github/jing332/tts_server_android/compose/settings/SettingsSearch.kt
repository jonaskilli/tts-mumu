package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope

class SettingsSearch(private val query: String) {
    fun active(): Boolean = query.isNotBlank()

    fun hit(vararg keywords: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return keywords.any { it.lowercase().contains(q) }
    }
}

@Composable
fun rememberSettingsSearch(query: String): SettingsSearch =
    remember(query) { SettingsSearch(query) }

/**
 * 搜索激活时按关键词过滤；未激活或命中则正常渲染内容。
 * 关键词含标题/副标题含义与常用别名，大小写不敏感。
 */
@Composable
fun ColumnScope.SettingItem(
    search: SettingsSearch,
    vararg keywords: String,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!search.active() || search.hit(*keywords)) content()
}

@Composable
fun SettingsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        placeholder = { Text("搜索设置") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        },
        shape = ShapeDefaults.ExtraLarge
    )
}
