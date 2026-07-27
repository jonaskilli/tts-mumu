package com.github.jing332.tts_server_android.compose.forwarder

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jing332.compose.widgets.AppWebView
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebScreen(
    modifier: Modifier,
    url: String = "",
    state: WebViewState = rememberWebViewState(url),
    navigator: WebViewNavigator = rememberWebViewNavigator(),
) {
    AppWebView(modifier = modifier, state = state, navigator = navigator)
}