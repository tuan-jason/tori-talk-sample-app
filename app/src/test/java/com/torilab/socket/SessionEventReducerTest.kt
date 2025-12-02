package com.torilab.socket

import com.castalk.socket.data.model.SessionEvent
import com.castalk.socket.data.model.emotion.EmotionModel
import com.castalk.socket.data.model.jsonrpc.ErrorInfo
import com.castalk.socket.data.model.jsonrpc.call.VideoMessagePayload
import com.castalk.socket.data.model.jsonrpc.chat.TextMessagePayload
import com.castalk.socket.data.model.jsonrpc.chat.TypingResponsePayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionEventReducerTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var reducer: SessionEventReducer

    @Before
    fun setUp() {
        viewModel = ChatViewModel()
        reducer = SessionEventReducer(viewModel)
    }

    private fun lastMessage() = viewModel.messages.last().text

    @Test
    fun `connection events toggle view model state`() = runBlocking {
        reducer.handle(SessionEvent.Connected)
        assertTrue(viewModel.isConnected.first())
        assertEquals("SocketConnected", lastMessage())

        reducer.handle(SessionEvent.Disconnected(code = 1, reason = "bye"))
        assertEquals(false, viewModel.isConnected.first())
        assertEquals("SocketDisconnected", lastMessage())

        reducer.handle(SessionEvent.RetryConnectSuccess)
        assertTrue(viewModel.isConnected.first())
        assertEquals("SocketRetryConnectSuccess", lastMessage())

        reducer.handle(SessionEvent.RetryConnectFailed)
        assertEquals(false, viewModel.isConnected.first())
        assertEquals("SocketRetryConnectFailed", lastMessage())
    }

    @Test
    fun `message events append content`() {
        val textPayload = TextMessagePayload().apply { content = "text" }
        reducer.handle(SessionEvent.TextMessage(textPayload))
        assertEquals("text", lastMessage())

        val videoPayload = VideoMessagePayload().apply { content = "video" }
        reducer.handle(SessionEvent.VideoMessage(videoPayload))
        assertEquals("video", lastMessage())
    }

    @Test
    fun `other events map to readable messages`() {
        reducer.handle(SessionEvent.IsResponding(TypingResponsePayload(subscriptionId = "sub", clientId = "client")))
        assertEquals("Responding....", lastMessage())

        reducer.handle(SessionEvent.GeneralError(ErrorInfo(code = 1, message = "bad", data = null)))
        assertTrue(lastMessage().contains("There's an error"))

        reducer.handle(SessionEvent.StreamingStop)
        assertEquals("Video streaming stopped.", lastMessage())

        reducer.handle(SessionEvent.FaceEmotion(EmotionModel(emotionId = "happy", eventId = "face")))
        assertTrue(lastMessage().contains("face emotion"))

        reducer.handle(SessionEvent.TextEmotion(EmotionModel(emotionId = "sad", eventId = "text")))
        assertTrue(lastMessage().contains("text emotion"))
    }

    @Test
    fun `retrying connect does not alter messages`() {
        val initialSize = viewModel.messages.size
        reducer.handle(SessionEvent.RetryingConnect)
        assertEquals(initialSize, viewModel.messages.size)
    }
}
