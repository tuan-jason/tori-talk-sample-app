package com.torilab.socket

import android.util.Log
import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.SessionEvent
import com.torilab.android.common.transcript.UserTranscriptType

private const val TAG = "SessionEventReducer"

class SessionEventReducer(
    private val chatViewModel: ChatViewModel,
    private val talkSession: TalkSession? = null
) {

    fun handle(event: SessionEvent) {
        when (event) {
            is SessionEvent.Connected -> {
                chatViewModel.setConnected(true)
                chatViewModel.appendFromServer("SocketConnected")
            }

            is SessionEvent.Disconnected -> {
                chatViewModel.setConnected(false)
                chatViewModel.appendFromServer("SocketDisconnected")
            }

            is SessionEvent.RetryConnectSuccess -> {
                chatViewModel.setConnected(true)
                chatViewModel.appendFromServer("SocketRetryConnectSuccess")
            }

            is SessionEvent.RetryConnectFailed -> {
                chatViewModel.setConnected(false)
                chatViewModel.appendFromServer("SocketRetryConnectFailed")
            }

            is SessionEvent.TextMessage -> chatViewModel.appendFromServer(event.msg.content)

            is SessionEvent.VideoMessage -> {
                chatViewModel.appendFromServer(event.msg.content)
            }

            is SessionEvent.IsResponding -> chatViewModel.appendFromServer("Responding....")

            is SessionEvent.GeneralError -> chatViewModel.appendFromServer("There's an error: ${event.error}")

            is SessionEvent.StreamingStop -> chatViewModel.appendFromServer("Video streaming stopped.")

            is SessionEvent.FaceEmotion -> chatViewModel.appendFromServer("Got face emotion: ${event.msg}")

            is SessionEvent.TextEmotion -> chatViewModel.appendFromServer("Got text emotion: ${event.msg}")

            SessionEvent.RetryingConnect -> Unit

            is SessionEvent.UserTranscript -> {
                if (event.msg.isFinal) {
                    Log.v(TAG, " Got final transcript: ${event.msg.text}\nrichDataType: ${event.msg.richDataType}")
                    if (event.msg.richDataType == UserTranscriptType.EMOTIONAL) return
                    chatViewModel.addTranscript(event.msg.text)
                    talkSession?.let { session ->
                        chatViewModel.sendVideoMessage(
                            talkSession = session,
                            message = event.msg.text,
                            emotionCode = event.msg.voiceEmotion?.id,
                            markDown = false,
                            isFromSTT = true
                        )

                    }
                } else {
                    Log.w(TAG, "Got partial transcript: ${event.msg.text}")
                }
            }

            is SessionEvent.AgentTranscript -> {
                chatViewModel.addAgentTranscript(event.msg)
            }
        }
    }
}
