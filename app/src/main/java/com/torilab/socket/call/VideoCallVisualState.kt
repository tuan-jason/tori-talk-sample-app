package com.torilab.socket.call

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds the render state for the video call area so the UI can react to
 * audio-only sessions without needing to duplicate state handling logic.
 */
class VideoCallVisualState {

    var videoRenderer by mutableStateOf<View?>(null)
        private set

    var isAudioOnlyPlaceholderVisible by mutableStateOf(false)
        private set

    var isStartVideoCallInProgress by mutableStateOf(false)
        private set

    /** Shows the progress indicator until a start result arrives. */
    fun onStartVideoCallRequested() {
        isStartVideoCallInProgress = true
    }

    /** Hides the progress indicator once the start result is handled. */
    fun onStartVideoCallResultHandled() {
        isStartVideoCallInProgress = false
    }

    /**
     * Updates the state once the call is started. When [renderer] is null the UI switches
     * to the audio-only placeholder, otherwise the provided view is rendered.
     */
    fun onCallStarted(renderer: View?) {
        onStartVideoCallResultHandled()
        videoRenderer = renderer
        isAudioOnlyPlaceholderVisible = renderer == null
    }

    /** Clears any active renderer or placeholder when the call fails. */
    fun onCallFailed() {
        reset()
    }

    /** Clears any active renderer or placeholder when the call stops. */
    fun onCallStopped() {
        reset()
    }

    private fun reset() {
        videoRenderer = null
        isAudioOnlyPlaceholderVisible = false
        isStartVideoCallInProgress = false
    }
}
