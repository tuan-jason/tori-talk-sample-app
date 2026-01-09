package com.torilab.socket

import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.SessionEvent
import com.castalk.socket.data.model.emotion.EmotionModel
import com.castalk.socket.data.model.jsonrpc.ErrorInfo
import com.castalk.socket.data.model.jsonrpc.call.VideoMessagePayload
import com.castalk.socket.data.model.jsonrpc.chat.TextMessagePayload
import com.castalk.socket.data.model.jsonrpc.chat.TypingResponsePayload
import com.castalk.socket.data.model.transcript.TranscriptData
import com.torilab.android.common.emotion.Emotion
import com.torilab.android.common.transcript.AvatarTranscript
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `transcript event adds transcript to view model`() {
        val transcriptData = TranscriptData(
            text = "Hello from transcript",
            isFinal = true,
            voiceEmotion = Emotion.HAPPY
        )
        
        reducer.handle(SessionEvent.UserTranscript(transcriptData))
        
        assertEquals(1, viewModel.transcripts.size)
        assertEquals("Hello from transcript", viewModel.transcripts[0])
    }

    @Test
    fun `partial transcript event does not add to view model`() {
        val partialTranscriptData = TranscriptData(
            text = "Partial transcript...",
            isFinal = false,
            voiceEmotion = null
        )
        
        reducer.handle(SessionEvent.UserTranscript(partialTranscriptData))
        
        // Partial transcripts should not be added to the list
        assertEquals(0, viewModel.transcripts.size)
    }

    @Test
    fun `final transcript triggers follow up video message when talk session provided`() {
        val talkSession = mockk<TalkSession>(relaxed = true)
        reducer = SessionEventReducer(viewModel, talkSession)

        val transcriptData = TranscriptData(
            text = "Final answer",
            isFinal = true,
            voiceEmotion = Emotion.SURPRISE
        )

        reducer.handle(SessionEvent.UserTranscript(transcriptData))

        assertEquals(listOf("Final answer"), viewModel.transcripts)
        verify(exactly = 1) {
            talkSession.sendVideoMessage(
                message = "Final answer",
                emotionCode = Emotion.SURPRISE.id,
                markDown = false,
                params = null,
                callback = any()
            )
        }
    }

    @Test
    fun `partial transcript does not trigger video message even when talk session provided`() {
        val talkSession = mockk<TalkSession>(relaxed = true)
        reducer = SessionEventReducer(viewModel, talkSession)

        val transcriptData = TranscriptData(
            text = "Working...",
            isFinal = false,
            voiceEmotion = Emotion.CALM
        )

        reducer.handle(SessionEvent.UserTranscript(transcriptData))

        assertTrue(viewModel.transcripts.isEmpty())
        verify(exactly = 0) {
            talkSession.sendVideoMessage(
                message = any(),
                emotionCode = any(),
                markDown = any(),
                params = any(),
                callback = any()
            )
        }
    }

    @Test
    fun `agent transcript is added only once per unique message id`() {
        val firstTranscript = AvatarTranscript(
            msgId = 42,
            texts = listOf("Hello from agent", "Still agent")
        )
        reducer.handle(SessionEvent.AgentTranscript(firstTranscript))
        assertEquals(listOf("Hello from agent", "Still agent"), viewModel.transcripts)

        // Duplicate event with the same msgId should be ignored
        reducer.handle(SessionEvent.AgentTranscript(firstTranscript))
        assertEquals(listOf("Hello from agent", "Still agent"), viewModel.transcripts)

        // A new msgId should be appended normally
        val secondTranscript = AvatarTranscript(
            msgId = 43,
            texts = listOf("New agent response")
        )
        reducer.handle(SessionEvent.AgentTranscript(secondTranscript))
        assertEquals(
            listOf("Hello from agent", "Still agent", "New agent response"),
            viewModel.transcripts
        )
    }
}
