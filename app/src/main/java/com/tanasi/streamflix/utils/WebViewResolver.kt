package com.tanasi.streamflix.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebViewResolver(private val context: Context) {

    private var webView: WebView? = null

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = suspendCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            webView = WebView(context).apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
                        ) { html ->
                            continuation.resume(html.trim().removeSurrounding("\""))
                            release()
                        }
                    }
                }
                loadUrl(url, headers)
            }
        }
    }

    private fun release() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}