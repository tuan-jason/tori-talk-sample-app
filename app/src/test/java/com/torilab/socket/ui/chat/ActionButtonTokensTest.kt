package com.torilab.socket.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionButtonTokensTest {

    @Test
    fun `color palette cycles for additional buttons`() {
        val expectedPalette = listOf(
            Color(0xFF6C63FF),
            Color(0xFFFFB74D),
            Color(0xFF26A69A),
            Color(0xFFEF5350),
            Color(0xFF42A5F5),
            Color(0xFFFF7043)
        )

        expectedPalette.forEachIndexed { index, color ->
            assertEquals(color, ActionButtonTokens.colorFor(index))
        }

        // Extra buttons should wrap to the beginning of the palette.
        assertEquals(
            expectedPalette.first(),
            ActionButtonTokens.colorFor(expectedPalette.size * 2)
        )
    }

    @Test
    fun `button size keeps compact height with flexible width`() {
        assertEquals(36.dp, ActionButtonTokens.buttonHeight)
        assertEquals(72.dp, ActionButtonTokens.minButtonWidth)
    }
}
