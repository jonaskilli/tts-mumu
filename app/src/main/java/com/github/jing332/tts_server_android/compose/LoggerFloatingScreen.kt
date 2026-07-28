package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.jing332.common.toArgb
import com.github.jing332.common.utils.limitLength
import com.github.jing332.script.runtime.console.LogListener
import com.github.jing332.script.runtime.console.LogListenerManager
import kotlin.text.appendLine

@Composable
fun LoggerFloatingScreen(
    modifier: Modifier = Modifier,
    show: Boolean,
    registry: LogListenerManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDismissRequest: () -> Unit = {},
    onLaunched: () -> Unit,
) {
    var logText by remember { mutableStateOf(AnnotatedString("")) }

    val listener = remember {
        LogListener { entry ->
            logText = buildAnnotatedString {
                append(logText)

                withStyle(SpanStyle(color = Color(entry.level.toArgb(darkTheme)))) {
                    appendLine(entry.message.limitLength(150))
                }
            }
        }
    }

    LaunchedEffect(registry) {
        registry.addLogListener(listener)
        onLaunched()
    }

    DisposableEffect(registry) {
        onDispose {
            registry.removeLogListener(listener)
        }
    }

    ElevatedCard {
        Column() {
            if (show)
                SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text =
                        logText, modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp)
                    )
                }
        }
    }
}