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
import com.castalk.socket.extensions.JsonHelper
import com.torilab.socket.ui.chat.WsChatScreen
import com.torilab.socket.ui.chat.WsConnectScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// <<< PUT YOUR READY-TO-USE WSS URL HERE >>>
private const val WSS_URL = "wss://dev-cheercast-api.torilab.ai/ws/chat/e3fe620a-fd35-4517-a96c-dd6f52fb30e1_025e8e33-40cb-4067-9ac5-7e7e043f7012/" // <-- replace with your link

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // Reuse the same talk session across screens
    private val talkSession by lazy { TalkSession() }
    private val chatVm: ChatViewModel by viewModels()
    private val sessionEventReducer by lazy { SessionEventReducer(chatVm) }

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
                            onSend = { text ->
                                chatVm.appendOutgoing(text)
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

                            is SessionEvent.Connected -> {
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.Disconnected -> {
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.RetryConnectSuccess -> {
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.RetryConnectFailed -> {
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.TextMessage -> {
                                Log.d(TAG, "Received text message: ${socketEvent.msg.content} subId: ${socketEvent.msg.subscriptionId}")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.VideoMessage -> {
                                Log.d(TAG, "Received video message: ${socketEvent.msg.content} subId: ${socketEvent.msg.subscriptionId}")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.IsResponding -> {
                                Log.d(TAG, "Avatar is responding...")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.GeneralError -> {
                                Log.e(TAG, "There's an error: ${socketEvent.error}")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.StreamingStop -> {
                                Log.d(TAG, "Video streaming stopped!")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.FaceEmotion -> {
                                Log.d(TAG, "Got face emotion: ${socketEvent.msg}")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.TextEmotion -> {
                                Log.d(TAG, "Got text emotion: ${socketEvent.msg}")
                                sessionEventReducer.handle(socketEvent)
                            }

                            is SessionEvent.RetryingConnect -> {}
                        }
                    }
                }
            } 
        } 
    }
}
