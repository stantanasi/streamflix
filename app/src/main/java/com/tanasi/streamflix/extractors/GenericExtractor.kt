package com.tanasi.streamflix.extractors

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.media3.common.MimeTypes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tanasi.streamflix.models.Video
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class GenericExtractor (private val context: Context) : Extractor() {

    override val name = "Generic extractor"
    override val mainUrl = ""


    private var webView: WebView? = null

    private val deferredVideoUrl = CompletableDeferred<String?>()
    private var interceptedHeaders: Map<String, String>? = null
    private var interceptedUrl: String? = null


    override suspend fun extract(link: String): Video {
        return extract(link, emptyMap())
    }


    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun extract(link: String, headers: Map<String, String>): Video = withContext(Dispatchers.Main) {
        if (deferredVideoUrl.isCompleted) {
            deferredVideoUrl.cancel()
        }

        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

            webChromeClient = WebChromeClient()

            addJavascriptInterface(VideoUrlJsInterface(), "StreamFlix")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url.toString()
                    if ((url.endsWith(".m3u8") || url.contains(".m3u8") || url.contains(".mp4") || url.contains(".ts")) && !url.startsWith(
                            "blob:"
                        )
                    ) {
                        if (interceptedUrl != null) {
                            interceptedUrl = url
                            interceptedHeaders = request?.requestHeaders
                            if (!deferredVideoUrl.isCompleted) {
                                deferredVideoUrl.complete(url)
                                webView?.post { cleanup() }
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }


                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectJavaScript()
                }
            }
            loadUrl(link, headers)
        }

        val videoUrl = try {
            withTimeout(15_000L) {
                deferredVideoUrl.await()
            }
        } catch (e: Exception) {
            null
        }

        cleanup()

        if (videoUrl.isNullOrEmpty()) {
            throw Exception("No video URL found by WebViewExtractor")
        }

        return@withContext Video(
            source = videoUrl.toString(),
            subtitles = listOf(),
            type = if (videoUrl.toString()
                    .endsWith("m3u8")
            ) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4,
            headers = interceptedHeaders ?: emptyMap(),
        )
    }

    private fun injectJavaScript() {
        val jsCode = """
        (function() {
            var playButton = document.querySelector("a.verystream.iconPlay");
            if (playButton) {
                playButton.click();
            }
            // Hook fetch
            const originalFetch = window.fetch;
            window.fetch = function(resource, init) {
                try {
                    let url = resource instanceof Request ? resource.url : resource;
                    let headers = {};

                    if (resource instanceof Request) {
                        resource.headers.forEach((v, k) => { headers[k] = v; });
                    }

                    if (init && init.headers) {
                        if (init.headers.forEach) {
                            init.headers.forEach((v, k) => { headers[k] = v; });
                        } else if (typeof init.headers === 'object') {
                            Object.assign(headers, init.headers);
                        }
                    }

                    if (typeof url === 'string' && (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.ts'))) {
                        Android.onVideoUrlFound("XHR_URL:" + url);
                        Android.onVideoUrlFound("XHR_HEADERS:" + JSON.stringify(headers));
                    }
                } catch(e) { /* ignore */ }
                return originalFetch.apply(this, arguments);
            };

            // Hook XHR open/send/setRequestHeader
            const originalXHROpen = XMLHttpRequest.prototype.open;
            const originalXHRSend = XMLHttpRequest.prototype.send;
            const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

            XMLHttpRequest.prototype.open = function(method, url) {
                this._url = url;
                this._headers = {};
                return originalXHROpen.apply(this, arguments);
            };

            XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                if (!this._headers) this._headers = {};
                this._headers[header] = value;
                return originalSetRequestHeader.apply(this, arguments);
            };

            XMLHttpRequest.prototype.send = function(body) {
                if (this._url && (this._url.includes('.m3u8') || this._url.includes('.mp4') || this._url.includes('.ts'))) {
                    Android.onVideoUrlFound("XHR_URL:" + this._url);
                    Android.onVideoUrlFound("XHR_HEADERS:" + JSON.stringify(this._headers || {}));
                }
                return originalXHRSend.apply(this, arguments);
            };

            // MediaSource logging
            const originalAddSourceBuffer = MediaSource.prototype.addSourceBuffer;
            MediaSource.prototype.addSourceBuffer = function(mime) {
                Android.onVideoUrlFound("MediaSource MIME: " + mime);
                return originalAddSourceBuffer.call(this, mime);
            };

            const originalAppendBuffer = SourceBuffer.prototype.appendBuffer;
            SourceBuffer.prototype.appendBuffer = function(buffer) {
                Android.onVideoUrlFound("Appending buffer of size: " + buffer.byteLength);
                return originalAppendBuffer.call(this, buffer);
            };

            // Video tag
            var video = document.querySelector('video');
            if (video) {
                var src = video.currentSrc || video.src;
                if (src && !src.startsWith('blob:')) {
                    Android.onVideoUrlFound("VIDEO_SRC:" + src);
                }
            }

            // Script scan
            var scripts = document.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                var text = scripts[i].innerText;
                var match = text.match(/(https?:\/\/[^\s'"\\]+?\.m3u8)/);
                if (match) {
                    Android.onVideoUrlFound("SCRIPT_M3U8:" + match[1]);
                    break;
                }
            }

            // 1. Video tag scan
            var video = document.querySelector('video');
            if (video) {
                var src = video.currentSrc || video.src;
                if (src && !src.startsWith('blob:')) {
                    Android.onVideoUrlFound("VIDEO_SRC:" + src);
                }
            }

            // 2. Iframe detection
            var iframes = document.getElementsByTagName('iframe');
            for (var i = 0; i < iframes.length; i++) {
                var src = iframes[i].src;
                if (src && (src.includes(".mp4") || src.includes(".m3u8"))) {
                    Android.onVideoUrlFound("IFRAME_SRC:" + src);
                }
            }

            // 3. Script content scan for .m3u8 URLs
            var scripts = document.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                var html = scripts[i].innerHTML;
                var match = html.match(/(https?:\/\/[^\s'"\\]+?\.m3u8)/);
                if (match) {
                    Android.onVideoUrlFound("SCRIPT_M3U8:" + match[1]);
                    break;
                }
            }

            // 4. Observe dynamic DOM changes for delayed video loading
            const observer = new MutationObserver(() => {
                var video = document.querySelector('video');
                if (video) {
                    var src = video.currentSrc || video.src;
                    if (src && !src.startsWith('blob:')) {
                        Android.onVideoUrlFound("VIDEO_SRC_MUTATION:" + src);
                    }
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
            
            // Delayed poll after 5 seconds to catch late video.src changes
            setTimeout(function() {
                var video = document.querySelector('video');
                if (video) {
                    var src = video.currentSrc || video.src;
                    if (src && !src.startsWith('blob:')) {
                        Android.onVideoUrlFound("VIDEO_SRC_POLL:" + src);
                    }
                }
            }, 5000);
        })();
    """.trimIndent()

        webView?.evaluateJavascript(jsCode, null)
    }



    private fun cleanup() {
        webView?.apply {
            stopLoading()
            clearCache(true)
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
    }

    private fun runOnMainThread(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            block()
        }
    }


    inner class VideoUrlJsInterface {
        @JavascriptInterface
        fun onVideoUrlFound(message: String) {
            Log.d("WebViewExtractor", "onVideoUrlFound: $message")
            when {
                message.startsWith("XHR_URL:") -> {
                    val url = message.removePrefix("XHR_URL:")
                    if (!deferredVideoUrl.isCompleted) deferredVideoUrl.complete(url)
                    interceptedUrl = url
                }

                message.startsWith("XHR_HEADERS:") -> {
                    val jsonHeaders = message.removePrefix("XHR_HEADERS:")
                    try {
                        val map = parseJsonToMap(jsonHeaders)
                        interceptedHeaders = map
                    } catch (e: Exception) {
                        interceptedHeaders = emptyMap()
                    }
                }
                message.startsWith("VIDEO_SRC_MUTATION:") -> {
                    val url = message.removePrefix("VIDEO_SRC_MUTATION:")
                    if (!deferredVideoUrl.isCompleted) deferredVideoUrl.complete(url)
                    interceptedUrl = url
                }

                else -> {
                    if (!deferredVideoUrl.isCompleted) deferredVideoUrl.complete(message)
                    interceptedUrl = message
                }
            }
        }

        fun parseJsonToMap(json: String): Map<String, String> {
            Log.d("WebViewExtractor", "parseJsonToMap: $json")
            return Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
        }

    }

}
