package com.torilab.socket

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.SessionEvent
import com.castalk.socket.data.model.jsonrpc.SendMessageResponse
import com.castalk.socket.data.model.jsonrpc.SendMessageResult
import com.castalk.socket.data.model.jsonrpc.call.VideoMessagePayload
import com.castalk.socket.data.model.recording.GetRecordingResult
import com.castalk.socket.data.model.scene.GetScheduleResult
import com.castalk.socket.data.model.scene.PinSceneResult
import com.castalk.socket.data.model.scene.SkipSceneResult
import com.castalk.socket.data.model.scene.UnpinSceneResult
import com.castalk.socket.data.model.transcript.TranscriptData
import com.torilab.android.common.Exception
import com.torilab.android.common.recording.Recording
import com.torilab.android.common.recording.RecordingMessage
import com.torilab.android.common.transcript.AvatarTranscript
import com.torilab.android.common.emotion.Emotion
import com.torilab.android.common.scene.SceneInfo
import com.torilab.android.common.scene.ScheduleInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        viewModel = ChatViewModel()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
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

    @Test
    fun `addTranscript adds transcript to list`() {
        viewModel.addTranscript("First transcript")
        viewModel.addTranscript("Second transcript")
        
        assertEquals(2, viewModel.transcripts.size)
        assertEquals("First transcript", viewModel.transcripts[0])
        assertEquals("Second transcript", viewModel.transcripts[1])
    }

    @Test
    fun `clearTranscripts removes all transcripts`() {
        viewModel.addTranscript("Transcript 1")
        viewModel.addTranscript("Transcript 2")
        assertEquals(2, viewModel.transcripts.size)
        
        viewModel.clearTranscripts()
        assertTrue(viewModel.transcripts.isEmpty())
    }

    @Test
    fun `addAgentTranscript ignores duplicates`() {
        val transcript = AvatarTranscript(
            msgId = 55L,
            texts = listOf("Line A", "Line B")
        )

        val firstResult = viewModel.addAgentTranscript(transcript)
        val secondResult = viewModel.addAgentTranscript(transcript)

        assertTrue(firstResult)
        assertFalse(secondResult)
        assertEquals(listOf("Line A", "Line B"), viewModel.transcripts)
    }

    @Test
    fun `sendVideoMessage appends subscription id on success`() {
        val talkSession = mockk<TalkSession>()
        every {
            talkSession.sendVideoMessage(
                message = "hello",
                emotionCode = 7,
                markDown = true,
                params = mapOf("extra" to 1),
                callback = any()
            )
        } answers {
            val response = mockk<SendMessageResponse>()
            every { response.result } returns SendMessageResult(subscriptionId = "subscription-42")
            val callback = args[4] as (Result<SendMessageResponse>) -> Unit
            callback(Result.success(response))
        }
        every { talkSession.sendRecordingText(message = any()) } answers { }

        viewModel.sendVideoMessage(
            talkSession = talkSession,
            message = "hello",
            emotionCode = 7,
            markDown = true,
            params = mapOf("extra" to 1)
        )

        assertEquals(1, viewModel.messages.size)
        val message = viewModel.messages.last().text
        assertTrue(message.contains("subscriptionId: subscription-42"))
        assertTrue(message.contains("Message: hello"))
        verify(exactly = 1) {
            talkSession.sendVideoMessage(
                message = "hello",
                emotionCode = 7,
                markDown = true,
                params = mapOf("extra" to 1),
                callback = any()
            )
        }
    }

    @Test
    fun `sendVideoMessage appends exception message on failure`() {
        val talkSession = mockk<TalkSession>()
        val exception = IllegalStateException("network down")
        every {
            talkSession.sendVideoMessage(
                message = any(),
                emotionCode = any(),
                markDown = any(),
                params = any(),
                callback = any()
            )
        } answers {
            val callback = args[4] as (Result<SendMessageResponse>) -> Unit
            callback(Result.failure(exception))
        }

        viewModel.sendVideoMessage(
            talkSession = talkSession,
            message = "retry later",
            emotionCode = null,
            markDown = false,
            params = null
        )

        assertEquals(1, viewModel.messages.size)
        val message = viewModel.messages.last().text
        assertEquals("sendVideoMessage exception: network down", message)
        verify(exactly = 1) {
            talkSession.sendVideoMessage(
                message = "retry later",
                emotionCode = null,
                markDown = false,
                params = null,
                callback = any()
            )
        }
    }

    @Test
    fun `fetchSceneInfo delegates success to caller`() {
        val talkSession = mockk<TalkSession>()
        val sceneInfo = SceneInfo(videoId = "scene-9")
        every {
            talkSession.getSceneInfo(
                payload = null,
                callback = any()
            )
        } answers {
            val callback = args[1] as (Result<SceneInfo>) -> Unit
            callback(Result.success(sceneInfo))
        }
        var received: SceneInfo? = null

        viewModel.fetchSceneInfo(talkSession = talkSession, payload = null) {
            received = it.getOrNull()
        }

        assertEquals("scene-9", received?.videoId)
        verify {
            talkSession.getSceneInfo(
                payload = null,
                callback = any()
            )
        }
    }

    @Test
    fun `fetchSceneInfo surfaces failures`() {
        val talkSession = mockk<TalkSession>()
        val error = IllegalStateException("rpc")
        every {
            talkSession.getSceneInfo(
                payload = any(),
                callback = any()
            )
        } answers {
            val callback = args[1] as (Result<SceneInfo>) -> Unit
            callback(Result.failure(error))
        }
        var failure: Throwable? = null

        viewModel.fetchSceneInfo(
            talkSession = talkSession,
            payload = "{}"
        ) {
            failure = it.exceptionOrNull()
        }

        assertEquals(error, failure)
    }

    @Test
    fun `skipScene delegates to TalkSession and returns success`() {
        val talkSession = mockk<TalkSession>()
        every {
            talkSession.skipScene(
                nextVideoId = "next",
                callback = any()
            )
        } answers {
            val callback = args[1] as (SkipSceneResult) -> Unit
            callback(SkipSceneResult.Success(Unit))
        }
        var completed: SkipSceneResult? = null

        viewModel.skipScene(
            talkSession = talkSession,
            nextVideoId = "next"
        ) {
            completed = it
        }

        assertTrue(completed is SkipSceneResult.Success)
        verify {
            talkSession.skipScene(
                nextVideoId = "next",
                callback = any()
            )
        }
    }

    @Test
    fun `skipScene surfaces failure`() {
        val talkSession = mockk<TalkSession>()
        val error = Exception(message = "skip failed", code = 10)
        every {
            talkSession.skipScene(
                nextVideoId = any(),
                callback = any()
            )
        } answers {
            val callback = args[1] as (SkipSceneResult) -> Unit
            callback(SkipSceneResult.Failure(error))
        }
        var received: SkipSceneResult? = null

        viewModel.skipScene(
            talkSession = talkSession,
            nextVideoId = null
        ) {
            received = it
        }

        val failure = received as? SkipSceneResult.Failure
        assertEquals(error, failure?.error)
    }

    @Test
    fun `pinScene delegates to TalkSession and returns success`() {
        val talkSession = mockk<TalkSession>()
        every {
            talkSession.pinScene(
                pinVideoId = "video-13",
                callback = any()
            )
        } answers {
            val callback = args[1] as (PinSceneResult) -> Unit
            callback(PinSceneResult.Success(payload = mapOf("key" to "value")))
        }
        var received: PinSceneResult? = null

        viewModel.pinScene(
            talkSession = talkSession,
            pinVideoId = "video-13"
        ) {
            received = it
        }

        assertTrue(received is PinSceneResult.Success)
        verify {
            talkSession.pinScene(
                pinVideoId = "video-13",
                callback = any()
            )
        }
    }

    @Test
    fun `pinScene surfaces failure`() {
        val talkSession = mockk<TalkSession>()
        val error = Exception(message = "pin failed", code = 88)
        every {
            talkSession.pinScene(
                pinVideoId = any(),
                callback = any()
            )
        } answers {
            val callback = args[1] as (PinSceneResult) -> Unit
            callback(PinSceneResult.Failure(error))
        }
        var received: PinSceneResult? = null

        viewModel.pinScene(
            talkSession = talkSession,
            pinVideoId = null
        ) {
            received = it
        }

        val failure = received as? PinSceneResult.Failure
        assertEquals(error, failure?.error)
    }

    @Test
    fun `unpinScene delegates to TalkSession and returns success`() {
        val talkSession = mockk<TalkSession>()
        every {
            talkSession.unpinScene(callback = any())
        } answers {
            val callback = args[0] as (UnpinSceneResult) -> Unit
            callback(UnpinSceneResult.Success(payload = mapOf("state" to "cleared")))
        }
        var received: UnpinSceneResult? = null

        viewModel.unpinScene(talkSession = talkSession) { received = it }

        assertTrue(received is UnpinSceneResult.Success)
        verify {
            talkSession.unpinScene(callback = any())
        }
    }

    @Test
    fun `unpinScene surfaces failure`() {
        val talkSession = mockk<TalkSession>()
        val error = Exception(message = "unpin failed", code = 14)
        every {
            talkSession.unpinScene(callback = any())
        } answers {
            val callback = args[0] as (UnpinSceneResult) -> Unit
            callback(UnpinSceneResult.Failure(error))
        }
        var received: UnpinSceneResult? = null

        viewModel.unpinScene(talkSession = talkSession) {
            received = it
        }

        val failure = received as? UnpinSceneResult.Failure
        assertEquals(error, failure?.error)
    }

    @Test
    fun `getSchedule delegates to TalkSession and returns success`() {
        val talkSession = mockk<TalkSession>()
        val schedule = ScheduleInfo(scenes = emptyList())
        every {
            talkSession.getSchedule(callback = any())
        } answers {
            val callback = args[0] as (GetScheduleResult) -> Unit
            callback(GetScheduleResult.Success(schedule))
        }
        var received: GetScheduleResult? = null

        viewModel.getSchedule(talkSession = talkSession) {
            received = it
        }

        val success = received as? GetScheduleResult.Success
        assertEquals(schedule, success?.schedule)
        verify {
            talkSession.getSchedule(callback = any())
        }
    }

    @Test
    fun `getSchedule surfaces failure`() {
        val talkSession = mockk<TalkSession>()
        val error = Exception(message = "schedule failed", code = 55)
        every {
            talkSession.getSchedule(callback = any())
        } answers {
            val callback = args[0] as (GetScheduleResult) -> Unit
            callback(GetScheduleResult.Failure(error))
        }
        var received: GetScheduleResult? = null

        viewModel.getSchedule(talkSession = talkSession) {
            received = it
        }

        val failure = received as? GetScheduleResult.Failure
        assertEquals(error, failure?.error)
    }

    @Test
    fun `getRecording delegates to TalkSession and returns success`() {
        val talkSession = mockk<TalkSession>()
        val recording = Recording(url = "https://cdn.castalk.ai/r.mp4")
        every {
            talkSession.getRecording(callback = any())
        } answers {
            val callback = args[0] as (GetRecordingResult) -> Unit
            callback(GetRecordingResult.Success(recording))
        }
        var received: GetRecordingResult? = null

        viewModel.getRecording(talkSession = talkSession) {
            received = it
        }

        val success = received as? GetRecordingResult.Success
        assertEquals(recording, success?.recording)
        verify {
            talkSession.getRecording(callback = any())
        }
    }

    @Test
    fun `getRecording surfaces failure`() {
        val talkSession = mockk<TalkSession>()
        val error = Exception(message = "recording failed", code = 77)
        every {
            talkSession.getRecording(callback = any())
        } answers {
            val callback = args[0] as (GetRecordingResult) -> Unit
            callback(GetRecordingResult.Failure(error))
        }
        var received: GetRecordingResult? = null

        viewModel.getRecording(talkSession = talkSession) {
            received = it
        }

        val failure = received as? GetRecordingResult.Failure
        assertEquals(error, failure?.error)
    }

    @Test
    fun `sendRecordingText delegates to TalkSession`() {
        val talkSession = mockk<TalkSession>()
        val message = RecordingMessage(msgId = 999L, text = "clip ready")
        every { talkSession.sendRecordingText(message = any()) } answers { }

        viewModel.sendRecordingText(talkSession = talkSession, request = message)

        verify(exactly = 1) { talkSession.sendRecordingText(message = message) }
    }

    @Test
    fun `SessionEventReducer does not send recording text on video message`() {
        val talkSession = mockk<TalkSession>()
        val reducer = SessionEventReducer(chatViewModel = viewModel, talkSession = talkSession)
        val payload = VideoMessagePayload().apply {
            content = "video content"
            id = 777L
        }
        every { talkSession.sendRecordingText(message = any()) } answers { }

        reducer.handle(SessionEvent.VideoMessage(payload))

        assertEquals("video content", viewModel.messages.last().text)
        verify(exactly = 0) { talkSession.sendRecordingText(message = any()) }
    }

    @Test
    fun `SessionEventReducer sends reply on final transcript`() {
        val talkSession = mockk<TalkSession>()
        val reducer = SessionEventReducer(chatViewModel = viewModel, talkSession = talkSession)
        val transcript = TranscriptData(
            text = "Final transcript",
            isFinal = true,
            voiceEmotion = Emotion.HAPPY
        )
        every {
            talkSession.sendVideoMessage(
                message = any(),
                emotionCode = any(),
                markDown = any(),
                params = any(),
                callback = any()
            )
        } answers { }

        reducer.handle(SessionEvent.UserTranscript(transcript))

        assertTrue(viewModel.transcripts.contains("Final transcript"))
        verify {
            talkSession.sendVideoMessage(
                message = "Final transcript",
                emotionCode = Emotion.HAPPY.id,
                markDown = false,
                params = null,
                callback = any()
            )
        }
    }

    @Test
    fun `TalkSession sendRecordingText executes scene action`() {
        val talkSession = TalkSession()
        val recorded = mutableListOf<RecordingMessage>()
        val latch = CountDownLatch(1)
        val repositoryProxy = rtcRepositoryProxy { message ->
            recorded.add(message)
            latch.countDown()
        }
        talkSession.setRtcRepositoryForTests(repositoryProxy)

        val request = RecordingMessage(msgId = 321L, text = "Recorder hello")
        talkSession.sendRecordingText(request)

        assertTrue("sendRecordingText did not finish in time", latch.await(1, TimeUnit.SECONDS))
        assertEquals(listOf(request), recorded)
    }

    private fun TalkSession.setRtcRepositoryForTests(repository: Any) {
        val method = this::class.java.methods.firstOrNull { it.name.startsWith("setRtcRepository$") }
            ?: error("Unable to find rtcRepository setter")
        method.isAccessible = true
        method.invoke(this, repository)
    }

    private fun rtcRepositoryProxy(onSendRecording: (RecordingMessage) -> Unit): Any {
        val repoClass = Class.forName("com.castalk.rtc.data.repository.RtcRepository")
        return Proxy.newProxyInstance(
            repoClass.classLoader,
            arrayOf(repoClass)
        ) { _, method, args ->
            when (method.name) {
                "sendTextToRecorder" -> {
                    val message = args?.getOrNull(0) as RecordingMessage
                    onSendRecording(message)
                }
                "remoteParticipantIdentity" -> "test-identity"
                else -> {
                    if (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java) {
                        false
                    } else null
                }
            }
        }
    }
}
