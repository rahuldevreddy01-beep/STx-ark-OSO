package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.LauncherPreferences
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import org.json.JSONObject

private const val LOCAL_ORB_HOST = "stx-local.assets"
private const val LOCAL_ORB_ORIGIN = "https://$LOCAL_ORB_HOST"

private class OrbBridgeCallbacks(
    private val mainHandler: Handler,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onOrbReady() {
        mainHandler.post { onReady() }
    }

    @JavascriptInterface
    fun onOrbError(message: String?) {
        val clean = message?.takeIf { it.isNotBlank() } ?: "WebGL 3D renderer unavailable"
        mainHandler.post { onError(clean) }
    }
}

private fun serveLocalAsset(context: Context, uri: Uri?): WebResourceResponse? {
    if (uri == null || uri.host != LOCAL_ORB_HOST) return null
    val rawPath = uri.path?.trimStart('/')?.takeIf { it.isNotBlank() } ?: return null
    if (rawPath.contains("..")) return null
    val stream = runCatching { context.assets.open(rawPath) }.getOrNull() ?: return null
    val mime = when {
        rawPath.endsWith(".html", ignoreCase = true) -> "text/html"
        rawPath.endsWith(".js", ignoreCase = true) ||
            rawPath.endsWith(".mjs", ignoreCase = true) -> "application/javascript"
        rawPath.endsWith(".css", ignoreCase = true) -> "text/css"
        rawPath.endsWith(".json", ignoreCase = true) -> "application/json"
        rawPath.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
        rawPath.endsWith(".png", ignoreCase = true) -> "image/png"
        else -> "application/octet-stream"
    }
    return WebResourceResponse(
        mime,
        "UTF-8",
        200,
        "OK",
        mapOf("Access-Control-Allow-Origin" to "*"),
        stream
    )
}

/**
 * Hardware-accelerated Android WebView wrapper hosting the exact Three.js WebGL orb from
 * `assets/stxark-ai.html` served over same-origin virtual HTTPS (`stx-local.assets`) with
 * local Three.js ES modules so it renders offline immediately with zero CORS issues.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StxHtmlOrbView(
    preferences: LauncherPreferences,
    activeTranscript: String = "",
    speechPulseTick: Int = 0,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    compactDialMode: Boolean = false,
    interactiveTouch: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val targetUrl = remember(compactDialMode) {
        if (compactDialMode) {
            "$LOCAL_ORB_ORIGIN/stxark-ai.html?embed=1&dial=1"
        } else {
            "$LOCAL_ORB_ORIGIN/stxark-ai.html?embed=1"
        }
    }

    var orbReady by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val settingsJson = remember(
        preferences.orbRotationSpeed,
        preferences.orbBloomStrength,
        preferences.orbScale,
        preferences.orbAutoRotate,
        preferences.orbShowTextNodes,
        preferences.orbShowDebris,
        preferences.orbSpeechGlowReactive
    ) {
        JSONObject().apply {
            put("speed", preferences.orbRotationSpeed.toDouble())
            put("bloomStrength", preferences.orbBloomStrength.toDouble())
            put("scale", preferences.orbScale.toDouble())
            put("autoRotate", preferences.orbAutoRotate)
            put("showText", preferences.orbShowTextNodes)
            put("showDebris", preferences.orbShowDebris)
            put("speechGlowReactive", preferences.orbSpeechGlowReactive)
        }.toString()
    }

    fun pushSettings(webView: WebView?) {
        webView ?: return
        runCatching {
            webView.evaluateJavascript("window.applyOrbSettings?.($settingsJson);", null)
        }
    }

    LaunchedEffect(settingsJson, orbReady, webViewRef) {
        if (orbReady) {
            pushSettings(webViewRef)
        }
    }

    LaunchedEffect(activeTranscript, speechPulseTick, isSpeaking, isListening, orbReady) {
        if (orbReady && preferences.orbSpeechGlowReactive && activeTranscript.isNotBlank()) {
            val quoted = JSONObject.quote(activeTranscript)
            runCatching {
                webViewRef?.evaluateJavascript("window.triggerStxText?.($quoted);", null)
            }
        }
    }

    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            val wv = webViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching {
                    wv.onResume()
                    wv.evaluateJavascript("window.setPaused?.(false);", null)
                }
                Lifecycle.Event.ON_PAUSE -> runCatching {
                    wv.evaluateJavascript("window.setPaused?.(true);", null)
                    wv.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("stx_html_orb_view"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                object : WebView(ctx) {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouchEvent(event: MotionEvent?): Boolean {
                        if (!interactiveTouch) return false
                        if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        return super.onTouchEvent(event)
                    }

                    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                        return !interactiveTouch || super.onInterceptTouchEvent(ev)
                    }
                }.apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    setLayerType(View.LAYER_TYPE_NONE, null)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = false
                        loadWithOverviewMode = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    addJavascriptInterface(
                        OrbBridgeCallbacks(
                            mainHandler = mainHandler,
                            onReady = {
                                loadError = null
                                orbReady = true
                                pushSettings(this)
                            },
                            onError = { msg ->
                                loadError = msg
                                orbReady = false
                            }
                        ),
                        "AndroidOrbBridge"
                    )

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            if (consoleMessage?.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                android.util.Log.e("StxHtmlOrbView", "JS Error: ${consoleMessage.message()}")
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            return serveLocalAsset(ctx.applicationContext, request?.url)
                                ?: super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pushSettings(view)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                loadError = error?.description?.toString()
                                    ?: "Unable to load 3D Holographic Orb"
                            }
                        }
                    }

                    mainHandler.post { webViewRef = this }
                    loadUrl(targetUrl)
                }
            },
            update = { wv ->
                if (webViewRef !== wv) {
                    mainHandler.post { webViewRef = wv }
                }
                if (wv.tag != reloadNonce) {
                    wv.tag = reloadNonce
                    if (reloadNonce > 0) {
                        orbReady = false
                        loadError = null
                        wv.loadUrl(targetUrl)
                    }
                }
            },
            onRelease = { wv ->
                if (webViewRef === wv) {
                    mainHandler.post { webViewRef = null }
                }
                runCatching {
                    wv.evaluateJavascript("window.setPaused?.(true);", null)
                    wv.removeJavascriptInterface("AndroidOrbBridge")
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.destroy()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!orbReady && loadError == null && !compactDialMode) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "IGNITING 3D HOLOGRAPHIC ORB…",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = HudCyan,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "WEBGL THREE.JS SCENE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = HudSlate,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!compactDialMode) {
            loadError?.let { err ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD060A12))
                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(6.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "3D ORB RENDER STANDBY",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = HudCrimson,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$err\nSwitch to Holo Girl or retry below.",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudIce,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                            .clickable {
                                loadError = null
                                orbReady = false
                                reloadNonce++
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("stx_html_orb_retry"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RETRY 3D ORB",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = HudIce
                        )
                    }
                }
            }
        }
    }
}
