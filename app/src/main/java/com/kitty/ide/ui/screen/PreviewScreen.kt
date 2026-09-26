package com.kitty.ide.ui.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Terminal
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
import com.kitty.ide.data.terminal.LogLevel
import com.kitty.ide.data.terminal.TerminalManager
import com.kitty.ide.ui.component.TerminalPanel
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    htmlPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    val fileName = File(htmlPath).name

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 顶栏：返回 + 文件名 + 终端 + 刷新 ──
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

            // 终端按钮
            IconButton(onClick = { showTerminal = !showTerminal }) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "终端",
                    tint = if (showTerminal) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface
                )
            }

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
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webChromeClient = WebChromeClient()

                        // 注入 console 劫持脚本
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                view?.evaluateJavascript(INJECT_CONSOLE_SCRIPT, null)
                            }
                        }

                        // JS Bridge
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onLog(level: String, message: String) {
                                val logLevel = when (level) {
                                    "WARN" -> LogLevel.WARN
                                    "ERROR" -> LogLevel.ERROR
                                    "INFO" -> LogLevel.INFO
                                    else -> LogLevel.LOG
                                }
                                Handler(Looper.getMainLooper()).post {
                                    TerminalManager.add(logLevel, message)
                                }
                            }
                        }, "AndroidConsole")

                        loadUrl("file://$htmlPath")
                        webViewRef = this
                    }
                }
            )
        }

        // ── 底部终端面板 ──
        AnimatedVisibility(
            visible = showTerminal,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            TerminalPanel(
                onClose = { showTerminal = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

/**
 * 注入到页面的 JS：劫持 console 和错误，转发到 Android 侧
 */
private const val INJECT_CONSOLE_SCRIPT = """
(function() {
    if (window.__kittyConsoleInstalled) return;
    window.__kittyConsoleInstalled = true;

    function stringify(args) {
        var parts = [];
        for (var i = 0; i < args.length; i++) {
            var a = args[i];
            if (a === null) { parts.push('null'); }
            else if (a === undefined) { parts.push('undefined'); }
            else if (typeof a === 'object') {
                try { parts.push(JSON.stringify(a)); }
                catch (e) { parts.push(String(a)); }
            } else {
                parts.push(String(a));
            }
        }
        return parts.join(' ');
    }

    function wrap(orig, level) {
        return function() {
            try { orig.apply(console, arguments); } catch (e) {}
            if (window.AndroidConsole) {
                window.AndroidConsole.onLog(level, stringify(arguments));
            }
        };
    }

    console.log = wrap(console.log, 'LOG');
    console.info = wrap(console.info, 'INFO');
    console.warn = wrap(console.warn, 'WARN');
    console.error = wrap(console.error, 'ERROR');

    window.addEventListener('error', function(e) {
        if (window.AndroidConsole) {
            var msg = e.message || 'Unknown error';
            if (e.filename) {
                msg += ' (line ' + e.lineno + ')';
            }
            window.AndroidConsole.onLog('ERROR', msg);
        }
    });

    window.addEventListener('unhandledrejection', function(e) {
        if (window.AndroidConsole) {
            window.AndroidConsole.onLog('ERROR', 'Unhandled Promise Rejection: ' + (e.reason || ''));
        }
    });
})();
"""