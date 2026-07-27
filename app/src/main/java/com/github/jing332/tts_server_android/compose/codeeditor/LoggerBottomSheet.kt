package com.github.jing332.tts_server_android.compose.codeeditor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jing332.common.toArgb
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.runtime.console.LogListener
import com.github.jing332.script.runtime.console.LogListenerManager
import com.github.jing332.tts_server_android.compose.theme.AppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggerBottomSheet(
    registry: LogListenerManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDismissRequest: () -> Unit,
    onLaunched: () -> Unit,
) {
    var logText by remember { mutableStateOf(AnnotatedString("")) }


    val listener = remember {
        LogListener { entry ->
            logText = buildAnnotatedString {
                append(logText)

                withStyle(SpanStyle(color = Color(entry.level.toArgb(darkTheme)))) {
                    appendLine(entry.message)
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

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        SelectionContainer(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                logText, modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Preview
@Composable
fun LoggerBottomSheetPreview() = AppTheme {
    var show by remember { mutableStateOf(true) }
    if (show) {
        val logger = remember { Console() }
        LoggerBottomSheet(
            registry = logger,
            darkTheme = false,
            onDismissRequest = { show = false },
            onLaunched = {
//                logger.verbose("Hello, I am verbose.")
//                logger.debug("Hello, I am debug.")
//                logger.info("Hello, I am info.")
//                logger.warn("Hello, I am warn.")
//                logger.error("Hello, I am error.")
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        var darkMode by remember { mutableStateOf(false) }
        Column(Modifier.align(Alignment.Center)) {
            Text("Dark theme")
            Switch(darkMode, {
                darkMode = it
                show = true
            })
        }

    }
}