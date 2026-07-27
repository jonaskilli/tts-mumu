package com.github.jing332.tts_server_android.compose.forwarder

import android.content.IntentFilter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.LocalBroadcastReceiver
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.service.forwarder.ForwarderServiceManager.startSysTtsForwarder
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.rememberSaveableWebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun BasicForwarderScreen(
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
    configScreen: @Composable () -> Unit,
    onGetUrl: () -> String,
) {
    val pages = remember { listOf(R.string.log, R.string.web) }
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                topBar(scrollBehavior)
                PrimaryTabRow(selectedTabIndex = state.currentPage, tabs = {
                    pages.forEachIndexed { index, strId ->
                        val selected = state.currentPage == index
                        TextButton(onClick = {
                            scope.launch {
                                state.animateScrollToPage(index)
                            }
                        }) { Text(stringResource(id = strId)) }
                    }
                })
            }
        }) { paddingValues ->
        val webState = rememberSaveableWebViewState().apply {
            content = WebContent.Url(onGetUrl())
        }
        val navigator = rememberWebViewNavigator()
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = state,
            userScrollEnabled = true
        ) {
            when (it) {
                0 -> configScreen()
                1 -> {
                    LocalBroadcastReceiver(intentFilter = IntentFilter(SysTtsForwarderService.ACTION_ON_STARTED)) {
                        navigator.loadUrl(onGetUrl())
                    }

                    LaunchedEffect(SysTtsForwarderService.isRunning) {
                        if (!SysTtsForwarderService.isRunning) context.startSysTtsForwarder()
                    }

                    WebScreen(
                        modifier = Modifier.fillMaxSize(),
                        state = webState,
                        navigator = navigator
                    )
                }
            }
        }
    }

}