package com.torilab.socket.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentToggleStateTest {

    private val devUrl = "dev-url"
    private val uatUrl = "uat-url"

    @Test
    fun `default state exposes dev url and UAT label`() {
        val state = EnvironmentToggleState(devUrl, uatUrl)

        assertEquals(devUrl, state.currentUrl)
        assertEquals("UAT", state.toggleButtonLabel)
    }

    @Test
    fun `toggle switches url to uat and flips label to dev`() {
        val state = EnvironmentToggleState(devUrl, uatUrl)

        val toggledUrl = state.toggle()

        assertEquals(uatUrl, toggledUrl)
        assertEquals(uatUrl, state.currentUrl)
        assertEquals("DEV", state.toggleButtonLabel)
    }

    @Test
    fun `second toggle returns to dev url and UAT label`() {
        val state = EnvironmentToggleState(devUrl, uatUrl)

        state.toggle()
        val toggledUrl = state.toggle()

        assertEquals(devUrl, toggledUrl)
        assertEquals(devUrl, state.currentUrl)
        assertEquals("UAT", state.toggleButtonLabel)
    }
}
