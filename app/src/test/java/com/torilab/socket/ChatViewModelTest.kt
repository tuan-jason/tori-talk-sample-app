package com.torilab.socket

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        viewModel = ChatViewModel()
    }

    @Test
    fun `initial state is disconnected`() = runBlocking {
        assertEquals(false, viewModel.isConnected.first())
    }

    @Test
    fun `setConnected updates state flow`() = runBlocking {
        viewModel.setConnected(true)
        assertEquals(true, viewModel.isConnected.first())

        viewModel.setConnected(false)
        assertEquals(false, viewModel.isConnected.first())
    }

    @Test
    fun `appendFromServer wraps text with blue span`() {
        viewModel.appendFromServer("server message")

        assertEquals(1, viewModel.messages.size)
        val message = viewModel.messages.last()
        assertEquals("server message", message.text)
        assertTrue("Expected one span applied to server message", message.spanStyles.size == 1)
        assertEquals(Color(0xFF1565C0), message.spanStyles.first().item.color)
    }

    @Test
    fun `appendOutgoing adds plain text`() {
        viewModel.appendOutgoing("outgoing")

        assertEquals(1, viewModel.messages.size)
        val message = viewModel.messages.last()
        assertEquals("outgoing", message.text)
        assertTrue("Outgoing message should not have styled spans", message.spanStyles.isEmpty())
    }

    @Test
    fun `clear removes all messages`() {
        viewModel.appendOutgoing("one")
        viewModel.appendFromServer("two")
        assertEquals(2, viewModel.messages.size)

        viewModel.clear()
        assertTrue(viewModel.messages.isEmpty())
    }
}
