package com.github.jing332.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.github.jing332.compose.ComposeWidgetSettings
import kotlin.math.max

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextFieldSelectionDialog(
    modifier: Modifier,

    labelText: String = "",
    label: @Composable () -> Unit = { Text(labelText) },
    leadingIcon: @Composable (() -> Unit)? = null,

    value: Any,
    values: List<Any>,
    entries: List<String>,
    icons: List<Any?> = emptyList(),
    enabled: Boolean = true,

    onSelectedChange: (key: Any, value: String) -> Unit,
    onValueSame: (current: Any, new: Any) -> Boolean = { current, new -> current == new },
    onEntryLongClick: ((key: Any, value: String) -> Unit)? = null,
    trailingContent: (@Composable RowScope.(itemValue: Any, entry: String, onHighlight: () -> Unit) -> Unit)? = null,
    selectedMultiValues: Set<Any> = emptySet(),
    onMultiSelectedChange: ((Set<Any>) -> Unit)? = null,
    extraButtons: @Composable BoxScope.() -> Unit = {},
    categoryMap: Map<Any, String> = emptyMap(),
    onCategoryChange: ((itemValue: Any, category: String?) -> Unit)? = null,
    waitCategorySwitch: Boolean = false,
    onWaitCategorySwitchChange: ((Boolean) -> Unit)? = null,
    autoNextSwitch: Boolean = false,
    onAutoNextSwitchChange: ((Boolean) -> Unit)? = null,
) {
    val selectedText = entries.getOrNull(max(0, values.indexOf(value))) ?: ""
    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(values, entries) {
        values.getOrNull(entries.indexOf(selectedText))?.let {
            onSelectedChange.invoke(it, selectedText)
        }
    }
    if (expanded) {
        AppSelectionDialog(
            onDismissRequest = { expanded = false },
            title = label,
            value = value,
            values = values,
            entries = entries,
            icons = icons,
            onClick = { v, entry ->
                onSelectedChange.invoke(v, entry)
                expanded = false
            },
            onValueSame = onValueSame,
            onLongClick = onEntryLongClick,
            trailingContent = trailingContent,
            selectedMultiValues = selectedMultiValues,
            onMultiSelectedChange = onMultiSelectedChange,
            extraButtons = extraButtons,
            categoryMap = categoryMap,
            onCategoryChange = onCategoryChange,
            waitCategorySwitch = waitCategorySwitch,
            onWaitCategorySwitchChange = onWaitCategorySwitchChange,
            autoNextSwitch = autoNextSwitch,
            onAutoNextSwitchChange = onAutoNextSwitchChange,
        )
    }

    Box(
        modifier = modifier
            .clickable(
                enabled = enabled,
                role = Role.DropdownList,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { expanded = !expanded }
    ) {
        CompositionLocalProvider(
            LocalTextInputService provides null,
            LocalTextToolbar provides EmptyTextToolbar,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .semantics(true) {
                        isEditable = false
                        text = AnnotatedString("")
                        editableText = AnnotatedString("$labelText, $selectedText")
                    }
                    .fillMaxWidth(),
                enabled = false,
                colors = if (enabled) OutlinedTextFieldDefaults.colors(
                    disabledContainerColor = Color.Transparent,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface,

                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

                    disabledBorderColor = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,

                    disabledPrefixColor = MaterialTheme.colorScheme.onSurface,
                    disabledSuffixColor = MaterialTheme.colorScheme.onSurface,
                )
                else
                    OutlinedTextFieldDefaults.colors(),

                leadingIcon = leadingIcon,
                readOnly = true,
                value = selectedText,
                onValueChange = { },
                label = label,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
            )
        }
    }
}

@Composable
fun AppSpinner(
    modifier: Modifier = Modifier,
    labelText: String = "",
    label: @Composable (() -> Unit) = { Text(labelText) },
    leadingIcon: @Composable (() -> Unit)? = null,

    value: Any,
    values: List<Any>,
    entries: List<String>,
    icons: List<Any?> = emptyList(),
    maxDropDownCount: Int = ComposeWidgetSettings.maxDropDownCount,
    enabled: Boolean = true,

    onValueSame: (current: Any, new: Any) -> Boolean = { current, new -> current == new },
    onSelectedChange: (key: Any, value: String) -> Unit,
    onEntryLongClick: ((key: Any, value: String) -> Unit)? = null,
    trailingContent: (@Composable RowScope.(itemValue: Any, entry: String, onHighlight: () -> Unit) -> Unit)? = null,
    selectedMultiValues: Set<Any> = emptySet(),
    onMultiSelectedChange: ((Set<Any>) -> Unit)? = null,
    extraButtons: @Composable BoxScope.() -> Unit = {},
    categoryMap: Map<Any, String> = emptyMap(),
    onCategoryChange: ((itemValue: Any, category: String?) -> Unit)? = null,
    waitCategorySwitch: Boolean = false,
    onWaitCategorySwitchChange: ((Boolean) -> Unit)? = null,
    autoNextSwitch: Boolean = false,
    onAutoNextSwitchChange: ((Boolean) -> Unit)? = null,
) {
    if (values.isNotEmpty() && !values.contains(value)) {
        onSelectedChange.invoke(values[0], entries[0])
    }

    val index = remember(value, values) { values.indexOf(value) }
    val icon = remember(icons, index) { icons.getOrNull(index) }

    // Non-null causes placeholder issues
    @Composable
    fun leading(): @Composable (() -> Unit)? {
        return if (leadingIcon == null && icon != null) {
            {
                AsyncCircleImage(icon)
            }
        } else leadingIcon
    }

    if (maxDropDownCount > 0 && values.size > maxDropDownCount) {
        TextFieldSelectionDialog(
            modifier = modifier,
            label = label,
            labelText = labelText,
            leadingIcon = leading(),
            value = value,
            values = values,
            entries = entries,
            icons = icons,
            enabled = enabled,
            onValueSame = onValueSame,
            onSelectedChange = onSelectedChange,
            onEntryLongClick = onEntryLongClick,
            trailingContent = trailingContent,
            selectedMultiValues = selectedMultiValues,
            onMultiSelectedChange = onMultiSelectedChange,
            extraButtons = extraButtons,
            categoryMap = categoryMap,
            onCategoryChange = onCategoryChange,
            waitCategorySwitch = waitCategorySwitch,
            onWaitCategorySwitchChange = onWaitCategorySwitchChange,
            autoNextSwitch = autoNextSwitch,
            onAutoNextSwitchChange = onAutoNextSwitchChange,
        )
    } else
        DropdownTextField(
            modifier = modifier,
            label = label,
            labelText = labelText,
            leadingIcon = leading(),
            value = value,
            values = values,
            entries = entries,
            icons = icons,
            enabled = enabled,
            onSelectedChange = onSelectedChange,
            onValueSame = onValueSame,
        )
}


@Preview
@Composable
private fun ExposedDropTextFieldPreview() {
    var key by remember { mutableIntStateOf(1) }
    val list = 0.rangeTo(10).toList()
    AppSpinner(
        labelText = "所属分组",
        value = key,
        values = list,
        entries = list.map { it.toString() },
        maxDropDownCount = 11,
        leadingIcon = {
            IconButton(
                onClick = {}
            ) {
                Icon(Icons.Default.Add, "添加", tint = Color.Blue)
            }
        },
        onSelectedChange = { k, _ ->
            key = k as Int
        }
    )
}