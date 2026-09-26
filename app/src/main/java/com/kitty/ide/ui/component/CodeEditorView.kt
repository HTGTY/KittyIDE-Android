package com.kitty.ide.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState as rememberHorizontalScrollState

/**
 * 纯原生 Compose 编辑器（0.0.1 版：能打字、黑底白字、等宽字体）
 * 后续可替换为 Sora Editor 或自定义高亮
 */
@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberHorizontalScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // VS Code 深色背景
            .padding(8.dp)
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll),
            textStyle = TextStyle(
                color = Color(0xFFD4D4D4), // VS Code 默认文字颜色
                fontFamily = FontFamily.Monospace, // 等宽字体
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            cursorBrush = SolidColor(Color(0xFF007ACC)), // VS Code 蓝色光标
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None, // 代码不自动大写
                autoCorrect = false // 关闭自动纠错
            )
        )
    }
}