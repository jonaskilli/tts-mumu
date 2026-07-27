package com.github.jing332.tts_server_android.compose

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.script.runtime.console.LogListenerManager
import com.github.jing332.tts_server_android.R
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.compose.enableComposeSupport
import com.petterp.floatingx.listener.IFxTouchListener
import com.petterp.floatingx.view.IFxInternalHelper

object LoggerFloatingManager {
    const val TAG = "LoggerFloatingManager"

    fun show(context: Context, logListenerManager: LogListenerManager) {
        if (FloatingX.isInstalled(TAG))
            FloatingX.control(TAG).show()
        else
            FloatingX.install {
                setTag(TAG)
                enableComposeSupport()
                setContext(context)
                setGravity(FxGravity.BOTTOM_OR_CENTER)

                val show = mutableStateOf(false)
                val dragView = ComposeView(context).apply {
//                    layoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
                    setContent {
                        SmallFloatingActionButton(
                            modifier = Modifier.padding(8.dp),
                            onClick = { show.value = !show.value }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(stringResource(R.string.log))
                                Icon(
                                    if (show.value) Icons.Default.KeyboardDoubleArrowDown else Icons.Default.KeyboardDoubleArrowRight,
                                    stringResource(R.string.log)
                                )
                            }
                        }
                    }
                }

                val contentView = ComposeView(context).apply {
//                    layoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
                    setContent {
                        LoggerFloatingScreen(
                            modifier = Modifier.fillMaxSize(),
                            show = show.value,
                            registry = logListenerManager
                        ) { }
                    }
                }

                setTouchListener(object : IFxTouchListener {
                    override fun onInterceptTouchEvent(
                        event: MotionEvent,
                        control: IFxInternalHelper?,
                    ): Boolean {
                        return control?.checkPointerDownTouch(dragView, event) ?: true
                    }
                })

                setLayoutView(
                    LinearLayout(context).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        orientation = LinearLayout.VERTICAL
                        addView(dragView)
                        addView(contentView)
                    }
                )

            }.show()
    }

    fun hide() {
        FloatingX.control(TAG).hide()
    }
}