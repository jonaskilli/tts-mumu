package com.github.jing332.tts_server_android.compose.systts.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.jing332.tts_server_android.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingAddConfigButtonGroup(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    addBgm: () -> Unit,
    addLocal: () -> Unit,
    addPlugin: () -> Unit,
    addGroup: () -> Unit,
) {
    val context = LocalContext.current
    var expended by rememberSaveable { mutableStateOf(false) }

    BackHandler(expended) { expended = !expended }
    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expended,
        button = {
            ToggleFloatingActionButton(
                modifier =
                Modifier
                    .semantics {
                        traversalIndex = -1f
                        stateDescription =
                            if (expended) context.getString(R.string.expended) else context.getString(
                                R.string.collapsed
                            )
                        contentDescription = context.getString(R.string.add_config)
                        role = Role.DropdownList
                    }
                    .animateFloatingActionButton(
                        visible = visible || expended,
                        alignment = Alignment.BottomEnd
                    )
                    .zIndex(1f),
                checked = expended,
                onCheckedChange = { expended = !expended }
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = stringResource(R.string.desc_fab_add_config),
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                addLocal()
                expended = false
            },
            text = { Text(stringResource(R.string.add_local_tts)) },
            icon = { Icon(Icons.Default.PhoneAndroid, null) }
        )
        Spacer(Modifier.height(2.dp))
        FloatingActionButtonMenuItem(
            onClick = {
                addPlugin()
                expended = false
            },
            text = { Text(stringResource(R.string.systts_add_plugin_tts)) },
            icon = { Icon(Icons.Default.Javascript, null) }
        )
        Spacer(Modifier.height(2.dp))
        FloatingActionButtonMenuItem(
            onClick = {
                addBgm()
                expended = false
            },
            text = { Text(stringResource(R.string.add_bgm_tts)) },
            icon = { Icon(Icons.Default.MusicNote, null) }
        )
        Spacer(Modifier.height(2.dp))
        FloatingActionButtonMenuItem(
            onClick = {
                addGroup()
                expended = false
            },
            text = { Text(stringResource(R.string.add_group)) },
            icon = { Icon(Icons.Default.AddCard, null) },
        )
//        }
    }
}

