package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope

class SettingsSearch(private val query: String) {
    fun active(): Boolean = query.isNotBlank()

    fun hit(vararg keywords: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        // 「设置」即本页主题：单独搜索时命中全部项；作为后缀时剥掉再匹配（如「主题设置」→「主题」）
        if (q == "设置" || q == "settings") return true
        val q2 = q.removeSuffix("设置").removeSuffix("settings").trim()
        if (q2.isEmpty()) return true
        return keywords.any { it.lowercase().contains(q) || it.lowercase().contains(q2) }
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
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // 充色搜索条：无描边、全圆角；底色用 surfaceContainer（与底部导航栏同色的淡紫白，用户指定）
    val container = MaterialTheme.colorScheme.surfaceContainer
    if (compact) {
        // OutlinedTextField 内部最小 56dp，强压 48dp 会纵向裁字——compact 改自绘胶囊（BasicTextField 无内部最小高度）
        Surface(
            modifier = modifier.padding(end = 12.dp),
            shape = ShapeDefaults.ExtraLarge,
            color = container
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search, null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    // 须整份覆盖为 bodyMedium：顶栏标题槽 ambient 是 titleLarge（lineHeight 28sp），
                    // 只改 fontSize 会继承大行高，字形贴行盒顶部→框内文字整体偏上
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    "搜索设置项",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close, null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        return
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (compact) Modifier.height(48.dp).padding(end = 12.dp) else Modifier.fillMaxWidth())
            .padding(horizontal = if (compact) 0.dp else 16.dp, vertical = if (compact) 0.dp else 4.dp),
        singleLine = true,
        placeholder = {
            Text(
                "搜索设置项",
                style = if (compact) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search, null,
                modifier = if (compact) Modifier.size(20.dp) else Modifier
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Default.Close, null,
                        modifier = if (compact) Modifier.size(20.dp) else Modifier
                    )
                }
            }
        },
        shape = ShapeDefaults.ExtraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
        )
    )
}
