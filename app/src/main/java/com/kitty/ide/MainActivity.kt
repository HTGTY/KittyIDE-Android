package com.kitty.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kitty.ide.data.syntax.SyntaxLoader
import com.kitty.ide.ui.navigation.KittyNavHost
import com.kitty.ide.ui.theme.FontManager
import com.kitty.ide.ui.theme.KittyIDETheme
import com.kitty.ide.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)
        FontManager.init(this)
        SyntaxLoader.init(this)

        // 异步加载语法文件，不阻塞启动页
        lifecycleScope.launch {
            SyntaxLoader.loadAll()
        }

        setContent {
            val mode by ThemeManager.themeMode
            val darkTheme = when (mode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            KittyIDETheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KittyNavHost()
                }
            }
        }
    }
}