package com.torilab.socket.ui.chat

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.castalk.socket.TalkSession
import com.castalk.socket.extensions.JsonHelper
import com.torilab.socket.R
import com.torilab.socket.model.SocketMessageRequest
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WsChatScreen(
    talkSession: TalkSession,
    onDisconnected: () -> Unit,
    jsonHelper: JsonHelper,
    messages: List<AnnotatedString>,                 // <— take it from outside
    onSend: (String) -> Unit                // <— callback to append/send
) {

    val TAG = "WsChatScreen"
    // Minimal chat UI (keeps your existing look & feel)
    var outgoing by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp)) {

        val buttonColor = Color(0xFF6954B7)
        // Top: Disconnect button
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    talkSession.disconnect()
                    onDisconnected()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
            ) { Text(stringResource(R.string.btn_disconnect)) }

            Button( // ⬅️ Added new button
                onClick = {
                    Log.d(TAG, "Calling startVideoCall with session: $talkSession")
                    talkSession.startVideoCall(lastVoiceCode = "252", manualVoiceSwap = true, lastRoomName = "") { result ->
                        result.onSuccess { callInfo ->
                            Log.i(TAG, "startVideoCall success wss: ${callInfo.result?.wssUrl}" +
                                    "\ntoken: ${callInfo.result?.token}")
                            onSend.invoke("wss URL: ${callInfo.result?.wssUrl}\n" +
                                        "token: ${callInfo.result?.token}"
                            )
                        }.onFailure { e ->
                            Log.e(TAG, "startVideoCall failed: ${e.message}")
                            onSend.invoke("Start Call exception: ${e.message}")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_video_call)) }

            Button( // ⬅️ Added new button
                onClick = {
                    Log.d(TAG, "Calling stopVideoCall with session: $talkSession")
                    talkSession.stopVideoCall()
                    onSend.invoke("Video call stopped.")
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_stop_video_call)) }

            Button( // ⬅️ Added new button
                onClick = {
                    Log.d(TAG, "Calling interruptTalking with session: $talkSession")
                    talkSession.interruptTalking()
                    onSend.invoke("Sent Interrupt talking event.")
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_interrupt_talking)) }

            Button(
                onClick = {
                    Log.d(TAG, "Calling greet with session: $talkSession")
                    talkSession.greet { result ->
                        result.onSuccess { response ->
                            Log.i(TAG, "greet success subscriptionId: ${response.result.subscriptionId}")
                            onSend.invoke("Greeted with subscriptionId: ${response.result.subscriptionId}")
                        }.onFailure { e ->
                            Log.e(TAG, "greet failed: ${e.message}")
                            onSend.invoke("greet exception: ${e.message}")
                        }
                    }
                    onSend.invoke("Ask for greetings from avatar.")
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_greeting)) }

            Button( // ⬅️ Added new button
                onClick = {
                    val newFace = "expo_presentation_luis_feceswap1"
                    Log.d(TAG, "Calling swapFace with session: $talkSession")
                    onSend.invoke("Swapping face with code $newFace")
                    talkSession.swapFace(newFace = newFace) { result ->
                        result.onSuccess { faceInfo ->
                            Log.i(TAG, "swapFace success prevFace: ${faceInfo.result.previousFace}" +
                                    "\ncurFace: ${faceInfo.result.currentFace}")
                            onSend.invoke("prevFace: ${faceInfo.result.previousFace}\n" +
                                        "curFace: ${faceInfo.result.currentFace}"
                            )
                        }.onFailure { e ->
                            Log.e(TAG, "swapFace failed: ${e.message}")
                            onSend.invoke("swapFace exception: ${e.message}")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_swap_face)) }

            Button( // ⬅️ Added new button
                onClick = {
                    Log.d(TAG, "Calling swapVoice with session: $talkSession")
                    val newVoiceCode = "252"
                    onSend.invoke("Swapping voice with code: $newVoiceCode")
                    talkSession.swapVoice(newVoice = newVoiceCode) { result ->
                        result.onSuccess { voiceInfo ->
                            Log.i(TAG,
                                "swapVoice success ID: ${voiceInfo.id} code: $newVoiceCode"
                            )
                            onSend.invoke("Swapped to voice code: $newVoiceCode")
                        }.onFailure { e ->
                            Log.e(TAG, "swapVoice failed: ${e.message}")
                            onSend.invoke("swapVoice exception: ${e.message}")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_swap_voice)) }

            Button( // ⬅️ Added new button
                onClick = {
                    val message = outgoing.trim()
                    outgoing = ""
                    Log.d(TAG, "Calling sendChatMessage with session: $talkSession")
                    talkSession.sendChatMessage(message = message, emotionCode = 5, markDown = false) { result ->
                        result.onSuccess { response ->
                            Log.i(TAG,
                                "sendChatMessage success subscriptionId: ${response.result.subscriptionId}"
                            )
                            onSend.invoke("subscriptionId: ${response.result.subscriptionId} Message: $message")
                        }.onFailure { e ->
                            Log.e(TAG, "sendChatMessage failed: ${e.message}")
                            onSend.invoke("sendChatMessage exception: ${e.message}")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_send_chat_message)) }

            Button( // ⬅️ Added new button
                onClick = {
                    val message = outgoing.trim()
                    outgoing = ""
                    Log.d(TAG, "Calling sendVideoMessage with session: $talkSession")
                    talkSession.sendVideoMessage(message = message, emotionCode = 5, markDown = false) { result ->
                        result.onSuccess { response ->
                            Log.i(TAG, "sendVideoMessage success subscriptionId: ${response.result.subscriptionId}")
                            onSend.invoke("subscriptionId: ${response.result.subscriptionId} Message: $message")
                        }.onFailure { e ->
                            Log.e(TAG, "sendVideoMessage failed: ${e.message}")
                            onSend.invoke("sendVideoMessage exception: ${e.message}")
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(start = 8.dp)
            ) { Text(stringResource(R.string.btn_send_video_message)) }
        }

        // Center: message log
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(index = messages.lastIndex)
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 20.dp),
            state = listState
        ) {
            items(messages) { line ->
                Spacer(Modifier.height(16.dp))
                Log.d(TAG, "Rendering text: $line")
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Bottom: input + Send
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            OutlinedTextField(
                value = outgoing,
                onValueChange = { outgoing = it },
                placeholder = { Text(stringResource(R.string.input_text_message_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            val canSend = outgoing.isNotBlank()
            Button(
                onClick = {
                    val text = outgoing.trim()
                    if (text.isNotEmpty()) {
                        val socketMessageRequest = SocketMessageRequest()
                        socketMessageRequest.id = UUID.randomUUID().toString()
                        socketMessageRequest.isRecording = false

                        socketMessageRequest.data = outgoing
                        socketMessageRequest.isChat = true

                        talkSession.sendMessage(
                            jsonHelper.toJson(socketMessageRequest, SocketMessageRequest::class.java))
                        outgoing = ""

                    }
                },
                enabled = canSend,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6954B7),
                    contentColor = Color.White
                ),
                modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
            ) { Text(stringResource(R.string.btn_send_custom_message)) }
        }
    }
}
