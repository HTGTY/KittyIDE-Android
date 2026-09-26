package com.kitty.ide.ui.screen

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    htmlPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val fileName = File(htmlPath).name

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 顶部栏：返回 + 文件名 + 刷新 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回编辑器",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── WebView 预览区域 ──
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        // 允许 HTML 里引用相对路径的 CSS / JS
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()

                        loadUrl("file://$htmlPath")
                        webViewRef = this
                    }
                }
            )
        }
    }
}