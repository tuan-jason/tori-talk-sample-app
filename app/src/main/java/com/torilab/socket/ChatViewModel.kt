package com.torilab.socket

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.camera.PublishCameraResult
import com.castalk.socket.data.model.recording.GetRecordingResult
import com.castalk.socket.data.model.scene.GetScheduleResult
import com.castalk.socket.data.model.scene.PinSceneResult
import com.castalk.socket.data.model.scene.SkipSceneResult
import com.castalk.socket.data.model.scene.UnpinSceneResult
import com.torilab.android.common.camera.CameraFacing
import com.torilab.android.common.recording.RecordingMessage
import com.torilab.android.common.scene.SceneInfo
import com.torilab.android.common.transcript.AvatarTranscript
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"
    // Compose observes this directly
    val messages = mutableStateListOf<AnnotatedString>()
    
    // Transcript state for video call transcripts
    val transcripts = mutableStateListOf<String>()
    private var lastAgentTranscriptMsgId: Long? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun appendFromServer(text: String) = messages.add(buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF1565C0))) {
            append(text)
        }
    })
    fun appendOutgoing(text: String) = messages.add(buildAnnotatedString {
        append(text)
    })
    fun clear() = messages.clear()
    
    fun addTranscript(text: String) {
        transcripts.add(text)
    }

    /**
     * Adds transcript texts coming from the agent when the payload is new.
     * Returns `true` when the texts were appended, `false` when ignored due to duplication.
     */
    fun addAgentTranscript(transcript: AvatarTranscript): Boolean {
        if (lastAgentTranscriptMsgId == transcript.msgId) {
            return false
        }
        lastAgentTranscriptMsgId = transcript.msgId
        transcript.texts.forEach { addTranscript(it) }
        return true
    }
    
    fun clearTranscripts() {
        transcripts.clear()
    }

    fun sendVideoMessage(
        talkSession: TalkSession,
        message: String,
        emotionCode: Int?,
        markDown: Boolean?,
        params: Map<String, Any?>? = null,
        isFromSTT: Boolean = false
    ) {
        Log.d(TAG, "Calling sendVideoMessage with session: $talkSession")
        talkSession.sendVideoMessage(
            message = message,
            emotionCode = emotionCode,
            markDown = markDown,
            params = params
        ) { result ->
            result.onSuccess { response ->
                Log.i(TAG, "sendVideoMessage success subscriptionId: ${response.result.subscriptionId}")
                appendOutgoing("subscriptionId: ${response.result.subscriptionId} Message: $message")

                if (!isFromSTT) {
                    sendRecordingText(
                        talkSession = talkSession,
                        request = RecordingMessage(msgId = response.result.id ?: 0L, text = message)
                    )
                }
            }.onFailure { e ->
                Log.e(TAG, "sendVideoMessage failed: ${e.message}")
                appendOutgoing("sendVideoMessage exception: ${e.message}")
            }
        }
    }

    fun fetchSceneInfo(
        talkSession: TalkSession,
        payload: String?,
        onResult: (Result<SceneInfo>) -> Unit
    ) {
        talkSession.getSceneInfo(
            payload = payload,
            callback = onResult
        )
    }

    fun skipScene(
        talkSession: TalkSession,
        nextVideoId: String?,
        onResult: (SkipSceneResult) -> Unit
    ) {
        talkSession.skipScene(
            nextVideoId = nextVideoId,
            callback = onResult
        )
    }

    fun pinScene(
        talkSession: TalkSession,
        pinVideoId: String?,
        onResult: (PinSceneResult) -> Unit
    ) {
        talkSession.pinScene(
            pinVideoId = pinVideoId,
            callback = onResult
        )
    }

    fun unpinScene(
        talkSession: TalkSession,
        onResult: (UnpinSceneResult) -> Unit
    ) {
        talkSession.unpinScene(callback = onResult)
    }

    fun getSchedule(
        talkSession: TalkSession,
        onResult: (GetScheduleResult) -> Unit
    ) {
        talkSession.getSchedule(callback = onResult)
    }

    fun getRecording(
        talkSession: TalkSession,
        onResult: (GetRecordingResult) -> Unit
    ) {
        talkSession.getRecording(callback = onResult)
    }

    fun publishCamera(
        cameraFacing: CameraFacing,
        talkSession: TalkSession,
        onResult: (PublishCameraResult) -> Unit
    ) {
        talkSession.publishCamera(facing = cameraFacing, callback = onResult)
    }

    fun unpublishCamera(talkSession: TalkSession) {
        talkSession.unpublishCamera()
    }

    fun sendRecordingText(
        talkSession: TalkSession,
        request: RecordingMessage
    ) {
        talkSession.sendRecordingText(message = request)
    }

}
