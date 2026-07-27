package com.github.jing332.script.runtime

import com.github.jing332.script.BackstageWebView
import com.github.jing332.script.defineFunction
import com.github.jing332.script.ensureArgumentsLength
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class GlobalWebview : Global() {
    companion object {
        const val NAME = "webview"

        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val obj = GlobalWebview()
            obj.defineFunction("loadUrl", obj::js_loadUrl, 3)
            obj.defineFunction("loadHtml", obj::js_loadHtml, 3)

            obj.init(scope, NAME, sealed)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun js_loadUrl(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<out Any>,
    ) =
        ensureArgumentsLength(args, 1..3) {
            val url = (args[0] as CharSequence).toString()
            val headers = args.getOrNull(1) as? Map<CharSequence, CharSequence>
            val script = (args.getOrNull(2) as? CharSequence)?.toString() ?: BackstageWebView.JS
            val webview = BackstageWebView(
                BackstageWebView.Payload.Url(url),
                headerMap = headers?.map { it.key.toString() to it.value.toString() }?.toMap()
                    ?: emptyMap(),
                js = script,
            )
            val ret = runBlocking { webview.getHtmlResponse() }
            ret.onSuccess {
                it
            }.onFailure {
                if (it is BackstageWebView.Error.E)
                    Context.reportError(it.description)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun js_loadHtml(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<out Any>,
    ) =
        ensureArgumentsLength(args, 1..3) {
            val html = (args[0] as CharSequence).toString()
            val headers = args.getOrNull(1) as? Map<CharSequence, CharSequence>
            val script = (args.getOrNull(2) as? CharSequence)?.toString() ?: BackstageWebView.JS
            val webview = BackstageWebView(
                BackstageWebView.Payload.Data(html),
                headerMap = headers?.map { it.key.toString() to it.value.toString() }?.toMap()
                    ?: emptyMap(),
                js = script
            )
            val ret = runBlocking { webview.getHtmlResponse() }
            ret.onSuccess {
                it
            }.onFailure {
                if (it is BackstageWebView.Error.E)
                    Context.reportError(it.description)
            }
        }
}

