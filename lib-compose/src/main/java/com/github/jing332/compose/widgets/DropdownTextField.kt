package com.github.jing332.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.max


//val CustomColorKey = SemanticsPropertyKey<String>("Label")
//
//// 2. (可选) 定义合并策略。这里我们选择使用父组件的值。
//val CustomColorKeyWithParentPriority = SemanticsPropertyKey<String>(
//    name = "Label",
//    mergePolicy = { parentValue, childValue -> parentValue }
//)
@Suppress("DEPRECATION")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DropdownTextField(
    modifier: Modifier = Modifier,
    labelText: String = "",
    label: @Composable() (() -> Unit) = { Text(labelText) },
    value: Any,
    values: List<Any>,
    entries: List<String>,
    icons: List<Any?> = emptyList(),
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    onValueSame: (current: Any, new: Any) -> Boolean = { current, new -> current == new },
    onSelectedChange: (value: Any, entry: String) -> Unit,
) {
    val selectedIndex = remember(value, values) { values.indexOf(value) }
    var selectedText =
        remember(entries, selectedIndex) { entries.getOrNull(max(0, selectedIndex)) ?: "" }
    val icon = remember(icons, selectedIndex) { icons.getOrNull(selectedIndex) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(values, entries) {
        values.getOrNull(entries.indexOf(selectedText))?.let {
            onSelectedChange.invoke(it, selectedText)
        }
    }

    // Non-null causes placeholder issues
    @Composable
    fun leading(): @Composable (() -> Unit)? {
        return if (leadingIcon == null && icon != null) {
            {
                AsyncCircleImage(icon)
            }
        } else leadingIcon
    }

    val view = LocalView.current
    CompositionLocalProvider(
        LocalTextInputService provides null // Disable Keyboard
    ) {
        ExposedDropdownMenuBox(
            modifier = modifier,
            expanded = expanded,
            onExpandedChange = {
                if (enabled) expanded = !expanded
            },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .semantics(true) {
                        isEditable = false
                        text = AnnotatedString("")
                        editableText = AnnotatedString("$labelText, $selectedText")
                    }
                    .fillMaxWidth(),
                leadingIcon = leadingIcon,
                readOnly = true,
                enabled = enabled,
                value = selectedText,
                onValueChange = { },
                label = label,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                entries.forEachIndexed { index, text ->
                    val checked = onValueSame(value, values[index])
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(
                                if (checked) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Unspecified
                            )
                            .semantics {
                                selected = selectedIndex == index
                            },
                        text = {
                            Text(
                                text,
                                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = leading(),
                        onClick = {
                            expanded = false
                            selectedText = text
                            onSelectedChange.invoke(values[index], text)
                        }
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun PreviewDropdownTextField() {
    var key by remember { mutableIntStateOf(1) }
    DropdownTextField(
        labelText = "所属分组",
        value = key,
        values = listOf(1, 2, 3),
        entries = listOf("1", "2", "3"),
    ) { k, _ ->
        key = k as Int
    }
}