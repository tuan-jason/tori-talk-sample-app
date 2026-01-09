package com.torilab.socket.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Simple holder that keeps track of the current offset applied to the camera preview overlay.
 */
@Stable
class CameraPreviewOverlayState(
    initialOffset: Offset = Offset.Zero
) {
    var offset: Offset by mutableStateOf(initialOffset)
        private set

    fun dragBy(dragAmount: Offset) {
        offset = offset + dragAmount
    }

    fun snapTo(newOffset: Offset) {
        offset = newOffset
    }

    fun reset() {
        offset = Offset.Zero
    }
}

@Composable
fun rememberCameraPreviewOverlayState(
    initialOffset: Offset = Offset.Zero
): CameraPreviewOverlayState = remember(initialOffset) {
    CameraPreviewOverlayState(initialOffset)
}
