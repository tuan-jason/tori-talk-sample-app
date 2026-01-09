package com.torilab.socket.call

import android.view.View
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCallVisualStateTest {

    @Test
    fun `audio only call shows placeholder`() {
        val state = VideoCallVisualState()

        state.onCallStarted(null)

        assertNull(state.videoRenderer)
        assertTrue(state.isAudioOnlyPlaceholderVisible)
    }

    @Test
    fun `video call hides placeholder`() {
        val state = VideoCallVisualState()
        val renderer = mockk<View>()

        state.onCallStarted(renderer)

        assertSame(renderer, state.videoRenderer)
        assertFalse(state.isAudioOnlyPlaceholderVisible)
    }

    @Test
    fun `stop call clears placeholder`() {
        val state = VideoCallVisualState()
        state.onCallStarted(null)

        state.onCallStopped()

        assertNull(state.videoRenderer)
        assertFalse(state.isAudioOnlyPlaceholderVisible)
    }

    @Test
    fun `call failure clears placeholder`() {
        val state = VideoCallVisualState()
        state.onCallStarted(null)

        state.onCallFailed()

        assertNull(state.videoRenderer)
        assertFalse(state.isAudioOnlyPlaceholderVisible)
    }

    @Test
    fun `start request shows progress until result handled`() {
        val state = VideoCallVisualState()

        assertFalse(state.isStartVideoCallInProgress)

        state.onStartVideoCallRequested()

        assertTrue(state.isStartVideoCallInProgress)

        state.onStartVideoCallResultHandled()

        assertFalse(state.isStartVideoCallInProgress)
    }

    @Test
    fun `call failure clears start progress`() {
        val state = VideoCallVisualState()
        state.onStartVideoCallRequested()

        state.onCallFailed()

        assertFalse(state.isStartVideoCallInProgress)
    }
}
