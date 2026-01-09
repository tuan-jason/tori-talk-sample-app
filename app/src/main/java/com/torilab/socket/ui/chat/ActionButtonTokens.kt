package com.torilab.socket.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralizes styling tokens for the compact action buttons displayed on [WsChatScreen].
 */
object ActionButtonTokens {

    private val palette = listOf(
        Color(0xFF6C63FF),
        Color(0xFFFFB74D),
        Color(0xFF26A69A),
        Color(0xFFEF5350),
        Color(0xFF42A5F5),
        Color(0xFFFF7043)
    )

    val buttonHeight: Dp = 36.dp
    val minButtonWidth: Dp = 72.dp
    val buttonSpacing: Dp = 4.dp

    fun colorFor(index: Int): Color {
        if (palette.isEmpty()) {
            return Color(0xFF6C63FF)
        }
        val normalizedIndex = ((index % palette.size) + palette.size) % palette.size
        return palette[normalizedIndex]
    }
}
