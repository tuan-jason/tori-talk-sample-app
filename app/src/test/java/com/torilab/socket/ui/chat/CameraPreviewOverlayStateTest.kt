package com.torilab.socket.ui.chat

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPreviewOverlayStateTest {

    @Test
    fun `dragging accumulates offset deltas`() {
        val state = CameraPreviewOverlayState()

        state.dragBy(Offset(10f, 15f))
        state.dragBy(Offset(-2.5f, 5f))

        assertEquals(Offset(7.5f, 20f), state.offset)
    }

    @Test
    fun `snapTo replaces the current offset`() {
        val state = CameraPreviewOverlayState()
        state.dragBy(Offset(1f, 1f))

        state.snapTo(Offset(-4f, 3f))

        assertEquals(Offset(-4f, 3f), state.offset)
    }
}
