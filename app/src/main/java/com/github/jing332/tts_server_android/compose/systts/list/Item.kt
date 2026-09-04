package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.github.jing332.common.utils.StringUtils.limitLength
import com.github.jing332.common.utils.performLongPress
import com.github.jing332.compose.widgets.AppDropdownMenu
import com.github.jing332.compose.widgets.LongClickIconButton
import com.github.jing332.compose.widgets.htmlcompose.HtmlText
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Item(
    modifier: Modifier,
    name: String,
    tagName: String,
    type: String,
    desc: String,
    params: String,
    reorderState: ReorderableLazyListState,

    standby: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,

    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAudition: () -> Unit,
    onExport: () -> Unit,
    onMoveToSubGroup: () -> Unit = {},
    onSwitchTag: () -> Unit = {},
    isInSubGroup: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current

    val limitNameLen by remember { AppConfig.limitNameLength }
    val limitedName = remember(name, limitNameLen) {
        if (limitNameLen == 0) name else name.limitLength(limitNameLen)
    }

    ElevatedCard(
        modifier = modifier
    ) {
        // 卡片中间区域单击：与右侧显式按钮互补
        // 默认(swapButton=false,右侧是编辑)：单击=试听；交换后(swapButton=true,右侧是试听)：单击=编辑
        val swapButton = AppConfig.isSwapListenAndEditButton.value
        ConstraintLayout(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .combinedClickable(
                    onClickLabel = stringResource(if (swapButton) R.string.edit else R.string.audition),
                    onClick = { if (swapButton) onEdit() else onAudition() },
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(context.getString(R.string.edit)) { onEdit(); true },
                        CustomAccessibilityAction(context.getString(R.string.delete)) { onDelete(); true },
                        CustomAccessibilityAction(context.getString(R.string.copy)) { onCopy(); true },
                        CustomAccessibilityAction(context.getString(R.string.audition)) { onAudition(); true },
                        CustomAccessibilityAction(context.getString(R.string.export_config)) { onExport(); true }
                    )
                }
                .padding(vertical = 4.dp)
        ) {
            val (
                checkRef,
                nameRef,
                contentRef,
                targetRef,
                typeRef,
                buttonsRef,
            ) = createRefs()
            Row(
                Modifier
                    .constrainAs(checkRef) {
                        start.linkTo(parent.start)
                        top.linkTo(nameRef.top)
                        bottom.linkTo(contentRef.bottom)

                        height = Dimension.fillToConstraints
                    }
                    .detectReorder(reorderState)) {
                if (isInSubGroup) {
                    RadioButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .semantics {
                                role = Role.Switch
                                context
                                    .getString(
                                        if (enabled) R.string.config_enabled_desc else R.string.config_disabled_desc,
                                        limitedName
                                    )
                                    .let {
                                        contentDescription = it
                                        stateDescription = it
                                    }
                            },
                        selected = enabled,
                        onClick = { onEnabledChange(!enabled) },
                    )
                } else {
                    Checkbox(
                        modifier = Modifier
                            .fillMaxHeight()
                            .semantics {
                                role = Role.Switch
                                context
                                    .getString(
                                        if (enabled) R.string.config_enabled_desc else R.string.config_disabled_desc,
                                        limitedName
                                    )
                                    .let {
                                        contentDescription = it
                                        stateDescription = it
                                    }
                            },
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
            }
            Text(
                limitedName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .constrainAs(nameRef) {
                        start.linkTo(checkRef.end)
                        top.linkTo(parent.top)
                    }
                    .padding(bottom = 4.dp)
            )

            Column(
                Modifier
                    .constrainAs(contentRef) {
                        start.linkTo(checkRef.end)
                        top.linkTo(nameRef.bottom)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                HtmlText(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                )

                HtmlText(
                    text = params,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                )
            }

            val limitLen by remember { AppConfig.limitTagLength }
            val limitedTagName = remember(tagName, limitLen) {
                if (limitLen == 0) tagName else tagName.limitLength(limitLen)
            }
            if (limitedTagName.isNotEmpty())
                TagScreen(
                    Modifier
                        .constrainAs(targetRef) {
                            top.linkTo(nameRef.top)
                            end.linkTo(parent.end)
                        }
                        .padding(end = 4.dp)
                        .clickable { onSwitchTag() },
                    tag = limitedTagName,
                )

            Row(modifier = Modifier.constrainAs(buttonsRef) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {
                val swapButton = AppConfig.isSwapListenAndEditButton.value
                IconButton(
                    modifier = Modifier,
                    onClick = { if (swapButton) onAudition() else onEdit() }
                ) {
                    if (swapButton)
                        Icon(Icons.Default.Headphones, stringResource(id = R.string.audition))
                    else
                        Icon(Icons.Default.Edit, stringResource(id = R.string.edit_desc, name))
                }

                var showOptions by remember { mutableStateOf(false) }
                LongClickIconButton(
                    onClick = { showOptions = true },
                    onLongClick = { if (swapButton) onEdit() else onAudition() },
                    onLongClickLabel = stringResource(id = if (swapButton) R.string.edit else R.string.audition)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        stringResource(id = R.string.more_options_desc, name)
                    )

                    AppDropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }) {

                        DropdownMenuItem(
                            text = { Text(stringResource(id = if (swapButton) R.string.edit else R.string.audition)) },
                            onClick = {
                                showOptions = false
                                if (swapButton)
                                    onEdit()
                                else
                                    onAudition()
                            },
                            leadingIcon = {
                                Icon(
                                    if (swapButton) Icons.Default.Edit else Icons.Default.Headphones,
                                    null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.copy)) },
                            onClick = {
                                showOptions = false
                                onCopy()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CopyAll, stringResource(R.string.copy))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.export_config)) },
                            onClick = {
                                showOptions = false
                                onExport()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Output, stringResource(R.string.export_config))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("移动到子分组") },
                            onClick = {
                                showOptions = false
                                onMoveToSubGroup()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountTree, null)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.delete)) },
                            onClick = {
                                showOptions = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(typeRef) {
                        end.linkTo(parent.end)
//                        top.linkTo(buttonsRef.bottom)
                        bottom.linkTo(parent.bottom)
                    }
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (standby) {
                    Text(
                        modifier = Modifier.padding(end = 4.dp),
                        text = stringResource(id = R.string.systts_standby),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = type.limitLength(14, "…"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    // 插件名按字符数截短（14字+省略号，中文字符宽度统一可预测），
                    // 右侧留 8dp 空隙避免与左侧采样率标签贴死
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

        }

    }
}

@Composable
private fun TagScreen(modifier: Modifier = Modifier, tag: String) {
    // small(8dp)对齐M3 chip默认圆角；Medium字重与旁边Bold的显示名拉开层级
    OutlinedCard(shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
