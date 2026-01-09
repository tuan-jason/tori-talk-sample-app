package com.torilab.socket

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.SessionEvent
import com.torilab.android.common.JsonHelper
import com.torilab.android.common.camera.CameraFacing
import com.torilab.socket.ui.chat.WsChatScreen
import com.torilab.socket.ui.chat.WsConnectScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// <<< PUT YOUR READY-TO-USE WSS URL HERE >>>
const val WSS_URL_DEV = "wss://dev-cheercast-api.torilab.ai/ws/chat/b7a6a429-b3ce-4551-ba4d-ab1cba7bce21_b76a48f7-5daf-4be1-a530-ef5da2516e39/" // <-- replace with your link
const val WSS_URL_UAT = "wss://uat-cheercast-api.torilab.ai/ws/chat/0cdd72d7-2489-48e9-9239-c186de75f683_7b4664d3-da5f-4cdc-882e-1a5f2a89862c/"
const val WSS_URL = WSS_URL_DEV

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // Reuse the same talk session across screens
    private val talkSession by lazy { TalkSession() }
    private val chatVm: ChatViewModel by viewModels()
    private val sessionEventReducer by lazy { SessionEventReducer(
        chatViewModel = chatVm, talkSession = talkSession)
    }

    @Inject
    lateinit var jsonHelper: JsonHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isConnected by chatVm.isConnected.collectAsState()
                    Log.d(TAG, "MainActivity isConnected: $isConnected")

                    if (isConnected) {
                        WsChatScreen(
                            talkSession = talkSession,
                            onDisconnected = {
                                chatVm.appendOutgoing("Disconnected!")
                                chatVm.setConnected(false) },
                            jsonHelper,
                            messages = chatVm.messages,
                            transcripts = chatVm.transcripts,
                            onSend = { text ->
                                chatVm.appendOutgoing(text)
                            },
                            onClearTranscripts = {
                                chatVm.clearTranscripts()
                            },

                            onSendVideoMessage = { message, emotionCode, markDown ->
                                chatVm.sendVideoMessage(
                                    talkSession = talkSession,
                                    message = message,
                                    emotionCode = emotionCode,
                                    markDown = markDown
                                )
                            },
                            onGetSceneInfo = { payload, callback ->
                                chatVm.fetchSceneInfo(
                                    talkSession = talkSession,
                                    payload = payload,
                                    onResult = callback
                                )
                            },
                            onSceneSkipped = { nextVideoId, callback ->
                                chatVm.skipScene(
                                    talkSession = talkSession,
                                    nextVideoId = nextVideoId,
                                    onResult = callback
                                )
                            },
                            onScenePinned = { pinVideoId, callback ->
                                chatVm.pinScene(
                                    talkSession = talkSession,
                                    pinVideoId = pinVideoId,
                                    onResult = callback
                                )
                            },
                            onSceneUnpinned = { callback ->
                                chatVm.unpinScene(
                                    talkSession = talkSession,
                                    onResult = callback
                                )
                            },
                            onGetSchedule = { callback ->
                                chatVm.getSchedule(
                                    talkSession = talkSession,
                                    onResult = callback
                                )
                            },
                            onGetRecording = { callback ->
                                chatVm.getRecording(
                                    talkSession = talkSession,
                                    onResult = callback
                                )
                            },
                            onPublishCamera = { callback ->
                                chatVm.publishCamera(
                                    CameraFacing.FRONT, talkSession = talkSession, onResult = callback
                                )
                            },
                            onUnpublishCamera = {
                                chatVm.unpublishCamera(talkSession = talkSession)
                            }
                        )
                    } else {
                        WsConnectScreen(
                            wssUrl = WSS_URL,
                            talkSession = talkSession
                        )
                    }
                }
            }
        }

        subscribeLiveData()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop $this")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy $this")
        talkSession.disconnect()
    }

    private fun subscribeLiveData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    talkSession.observableSessionEvent().collect { socketEvent ->
                        Log.d(TAG, "Collected socketEvent: $socketEvent")
                        when (socketEvent) {

                            is SessionEvent.Connected -> {}

                            is SessionEvent.Disconnected -> {}

                            is SessionEvent.RetryConnectSuccess -> {}

                            is SessionEvent.RetryConnectFailed -> {}

                            is SessionEvent.TextMessage -> {
                                Log.d(TAG, "Received text message: ${socketEvent.msg.content} subId: ${socketEvent.msg.subscriptionId}")
                            }

                            is SessionEvent.VideoMessage -> {
                                Log.d(TAG, "Received video message: ${socketEvent.msg.content} subId: ${socketEvent.msg.subscriptionId}")
                            }

                            is SessionEvent.IsResponding -> {
                                Log.d(TAG, "Avatar is responding...")
                            }

                            is SessionEvent.GeneralError -> {
                                Log.e(TAG, "There's an error: ${socketEvent.error}")
                            }

                            is SessionEvent.StreamingStop -> {
                                Log.d(TAG, "Video streaming stopped!")
                            }

                            is SessionEvent.FaceEmotion -> {
                                Log.d(TAG, "Got face emotion: ${socketEvent.msg}")
                            }

                            is SessionEvent.TextEmotion -> {
                                Log.d(TAG, "Got text emotion: ${socketEvent.msg}")
                            }

                            is SessionEvent.RetryingConnect -> {}

                            is SessionEvent.UserTranscript -> {
                                Log.d(TAG, "Got user transcript: ${socketEvent.msg.text} isFinal: ${socketEvent.msg.isFinal} emotion: ${socketEvent.msg.voiceEmotion}")
                            }

                            is SessionEvent.AgentTranscript -> {
                                Log.d(TAG, "Got agent transcript: ${socketEvent.msg}")
                            }
                        }

                        sessionEventReducer.handle(socketEvent)
                    }
                }
            } 
        } 
    }
}
