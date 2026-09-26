package com.kitty.ide.ui.component

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeEditorView(
    initialContent: String = "",
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                // 允许混合内容（CDN是https，但本地asset是file）
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                webViewClient = WebViewClient()

                // 添加 JS 桥接
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onContentChanged(newContent: String) {
                        // 注意：这里在子线程，需要切回主线程更新 Compose 状态
                        webViewState.value?.post {
                            onContentChanged(newContent)
                        }
                    }
                }, "AndroidBridge")

                loadUrl("file:///android_asset/editor/index.html")

                webViewState.value = this
            }
        },
        update = { webView ->
            // 如果外部传入了初始内容，在页面加载完成后注入
            if (initialContent.isNotEmpty()) {
                webView.evaluateJavascript(
                    "window.setEditorContent(`${initialContent.replace("`", "\\`")}`);",
                    null
                )
            }
        }
    )
}