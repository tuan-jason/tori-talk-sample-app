package com.torilab.socket.ui.chat

import android.content.Context
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.castalk.socket.ITalkSession
import com.castalk.socket.data.model.camera.PublishCameraResult
import com.castalk.socket.data.model.recording.GetRecordingResult
import com.castalk.socket.data.model.scene.GetScheduleResult
import com.castalk.socket.data.model.scene.PinSceneResult
import com.castalk.socket.data.model.scene.SkipSceneResult
import com.castalk.socket.data.model.scene.UnpinSceneResult
import com.castalk.socket.ui.CameraPreviewRenderer
import com.torilab.android.common.scene.SceneInfo
import com.torilab.socket.R
import com.torilab.socket.call.VideoCallVisualState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class ChatActionContext(
    val talkSession: ITalkSession,
    val outgoingText: String,
    val onOutgoingTextChanged: (String) -> Unit,
    val onSend: (String) -> Unit,
    val onClearTranscripts: () -> Unit,
    val onSendVideoMessage: (String, Int?, Boolean?) -> Unit,
    val videoCallVisualState: VideoCallVisualState,
    val startVideoCallWithPermission: () -> Unit,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val context: Context,
    val isMicrophoneMuted: Boolean,
    val setIsMicrophoneMuted: (Boolean) -> Unit,
    val isMicrophoneToggleInProgress: Boolean,
    val setIsMicrophoneToggleInProgress: (Boolean) -> Unit,
    val onGetSceneInfo: (String?, (Result<SceneInfo>) -> Unit) -> Unit,
    val onSceneSkipped: (String?, (SkipSceneResult) -> Unit) -> Unit,
    val onScenePinned: (String?, (PinSceneResult) -> Unit) -> Unit,
    val onSceneUnpinned: ((UnpinSceneResult) -> Unit) -> Unit,
    val onGetSchedule: ((GetScheduleResult) -> Unit) -> Unit,
    val onGetRecording: ((GetRecordingResult) -> Unit) -> Unit,
    val onPublishCamera: ((PublishCameraResult) -> Unit) -> Unit,
    val publishedCameraView: CameraPreviewRenderer?,
    val setPublishedCameraView: (CameraPreviewRenderer?) -> Unit,
    val onUnpublishCamera: () -> Unit,
    val onDisconnected: () -> Unit,
    val showCallControls: Boolean,
    val hasCameraPermission: () -> Boolean,
    val requestCameraPermission: ((Boolean) -> Unit) -> Unit,
    val onCameraPermissionDenied: () -> Unit
)

internal object ChatActionRegistry {

    private val primaryActions: List<ChatAction> = listOf(
        DisconnectAction,
        StartVideoCallAction,
        StopVideoCallAction,
        InterruptTalkingAction,
        GreetingAction,
        SwapFaceAction,
        SwapVoiceAction,
        SendChatMessageAction,
        SendVideoMessageAction
    )

    private val callSpecificActions: List<ChatAction> = listOf(
        MicrophoneToggleAction,
        SceneInfoAction,
        SkipSceneAction,
        PinSceneAction,
        UnpinSceneAction,
        GetScheduleAction,
        GetRecordingAction,
        CameraAction
    )

    fun buildActionButtons(context: ChatActionContext): List<ActionButtonSpec> {
        val actions = buildList {
            addAll(primaryActions)
            if (context.showCallControls) {
                addAll(callSpecificActions)
            }
        }
        return actions.mapNotNull { it.create(context) }
    }
}

private sealed interface ChatAction {
    fun create(context: ChatActionContext): ActionButtonSpec?
}

private object DisconnectAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_disconnect) {
            context.talkSession.disconnect()
            context.onDisconnected()
        }
}

private object StartVideoCallAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_video_call) {
            context.startVideoCallWithPermission()
        }
}

private object StopVideoCallAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_stop_video_call) {
            Log.d(TAG, "Calling stopVideoCall with session: ${context.talkSession}")
            context.talkSession.stopVideoCall()
            context.videoCallVisualState.onCallStopped()
            context.onClearTranscripts()
            context.onSend("Video call stopped.")
        }
}

private object InterruptTalkingAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_interrupt_talking) {
            Log.d(TAG, "Calling interruptTalking with session: ${context.talkSession}")
            context.talkSession.interruptTalking()
            context.onSend("Sent Interrupt talking event.")
        }
}

private object GreetingAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_greeting) {
            Log.d(TAG, "Calling greet with session: ${context.talkSession}")
            context.talkSession.greet { result ->
                result.onSuccess { response ->
                    Log.i(TAG, "greet success subscriptionId: ${response.result.subscriptionId}")
                    context.onSend("Greeted with subscriptionId: ${response.result.subscriptionId}")
                }.onFailure { e ->
                    Log.e(TAG, "greet failed: ${e.message}")
                    context.onSend("greet exception: ${e.message}")
                }
            }
            context.onSend("Ask for greetings from avatar.")
        }
}

private object SwapFaceAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_swap_face) {
            val newFace = "expo_presentation_luis_feceswap1"
            Log.d(TAG, "Calling swapFace with session: ${context.talkSession}")
            context.onSend("Swapping face with code $newFace")
            context.talkSession.swapFace(newFace = newFace) { result ->
                result.onSuccess { faceInfo ->
                    Log.i(
                        TAG,
                        "swapFace success prevFace: ${faceInfo.result.previousFace}\n" +
                            "curFace: ${faceInfo.result.currentFace}"
                    )
                    context.onSend(
                        "prevFace: ${faceInfo.result.previousFace}\n" +
                            "curFace: ${faceInfo.result.currentFace}"
                    )
                }.onFailure { e ->
                    Log.e(TAG, "swapFace failed: ${e.message}")
                    context.onSend("swapFace exception: ${e.message}")
                }
            }
        }
}

private object SwapVoiceAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_swap_voice) {
            Log.d(TAG, "Calling swapVoice with session: ${context.talkSession}")
            val newVoiceCode = "252"
            context.onSend("Swapping voice with code: $newVoiceCode")
            context.talkSession.swapVoice(newVoice = newVoiceCode) { result ->
                result.onSuccess { voiceInfo ->
                    Log.i(TAG, "swapVoice success ID: ${voiceInfo.id} code: $newVoiceCode")
                    context.onSend("Swapped to voice code: $newVoiceCode")
                }.onFailure { e ->
                    Log.e(TAG, "swapVoice failed: ${e.message}")
                    context.onSend("swapVoice exception: ${e.message}")
                }
            }
        }
}

private object SendChatMessageAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_send_chat_message) {
            val message = context.outgoingText.trim()
            context.onOutgoingTextChanged("")
            Log.d(TAG, "Calling sendChatMessage with session: ${context.talkSession}")
            context.talkSession.sendChatMessage(message = message, emotionCode = 5, markDown = false) { result ->
                result.onSuccess { response ->
                    Log.i(
                        TAG,
                        "sendChatMessage success subscriptionId: ${response.result.subscriptionId}"
                    )
                    context.onSend("subscriptionId: ${response.result.subscriptionId} Message: $message")
                }.onFailure { e ->
                    Log.e(TAG, "sendChatMessage failed: ${e.message}")
                    context.onSend("sendChatMessage exception: ${e.message}")
                }
            }
        }
}

private object SendVideoMessageAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_send_video_message) {
            val message = context.outgoingText.trim()
            context.onOutgoingTextChanged("")
            Log.d(TAG, "Calling sendVideoMessage with session: ${context.talkSession}")
            context.onSendVideoMessage(message, 5, false)
        }
}

private object MicrophoneToggleAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec {
        val labelRes = if (context.isMicrophoneMuted) {
            R.string.btn_unmute_microphone
        } else {
            R.string.btn_mute_microphone
        }
        return ActionButtonSpec(
            labelResId = labelRes,
            enabled = !context.isMicrophoneToggleInProgress
        ) {
            if (context.isMicrophoneToggleInProgress) {
                return@ActionButtonSpec
            }
            val currentlyMuted = context.isMicrophoneMuted
            context.setIsMicrophoneToggleInProgress(true)
            val callback: (Boolean) -> Unit = { success ->
                context.coroutineScope.launch {
                    context.setIsMicrophoneToggleInProgress(false)
                    if (success) {
                        context.setIsMicrophoneMuted(!currentlyMuted)
                    } else {
                        context.snackbarHostState.showSnackbar(
                            message = context.context.getString(R.string.msg_microphone_toggle_failed)
                        )
                    }
                }
            }
            if (currentlyMuted) {
                context.talkSession.unmuteMicrophone(callback)
            } else {
                context.talkSession.muteMicrophone(callback)
            }
        }
    }
}

private object SceneInfoAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_get_scene) {
            context.onGetSceneInfo(
                "{}"
            ) { result ->
                context.coroutineScope.launch {
                    result.onSuccess { info ->
                        context.snackbarHostState.showSnackbar(info.toString())
                    }.onFailure { error ->
                        context.snackbarHostState.showSnackbar(
                            context.context.getString(
                                R.string.msg_scene_info_failed,
                                error.message ?: context.context.getString(R.string.msg_unknown_error)
                            )
                        )
                    }
                }
            }
        }
}

private object SkipSceneAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_skip_scene) {
            context.onSceneSkipped(
                ""
            ) { outcome ->
                context.coroutineScope.launch {
                    when (outcome) {
                        is SkipSceneResult.Success -> {
                            Log.i(TAG, "Skip scene success. Result: ${outcome.payload}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(R.string.msg_scene_skip_success)
                            )
                        }
                        is SkipSceneResult.Failure -> {
                            val error = outcome.error
                            Log.e(TAG, "Skip scene failed. code: ${error.code} message: ${error.message}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(
                                    R.string.msg_scene_skip_failed,
                                    error.message
                                )
                            )
                        }
                    }
                }
            }
        }
}

private object PinSceneAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_pin_scene) {
            context.onScenePinned(
                null
            ) { outcome ->
                context.coroutineScope.launch {
                    when (outcome) {
                        is PinSceneResult.Success -> {
                            Log.i(TAG, "Pin scene success. Result: ${outcome.payload}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(R.string.msg_scene_pin_success)
                            )
                        }
                        is PinSceneResult.Failure -> {
                            val error = outcome.error
                            Log.e(TAG, "Pin scene failed. code: ${error.code} message: ${error.message}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(
                                    R.string.msg_scene_pin_failed,
                                    error.message
                                )
                            )
                        }
                    }
                }
            }
        }
}

private object UnpinSceneAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_unpin_scene) {
            context.onSceneUnpinned { outcome ->
                context.coroutineScope.launch {
                    when (outcome) {
                        is UnpinSceneResult.Success -> {
                            Log.i(TAG, "Unpin scene success. Result: ${outcome.payload}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(R.string.msg_scene_unpin_success)
                            )
                        }
                        is UnpinSceneResult.Failure -> {
                            val error = outcome.error
                            Log.e(TAG, "Unpin scene failed. code: ${error.code} message: ${error.message}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(
                                    R.string.msg_scene_unpin_failed,
                                    error.message
                                )
                            )
                        }
                    }
                }
            }
        }
}

private object GetScheduleAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_get_schedule) {
            context.onGetSchedule { outcome ->
                context.coroutineScope.launch {
                    when (outcome) {
                        is GetScheduleResult.Success -> {
                            Log.i(TAG, "Get schedule success. Result: ${outcome.schedule}")
                            context.snackbarHostState.showSnackbar(
                                outcome.schedule.toString()
                            )
                        }
                        is GetScheduleResult.Failure -> {
                            val error = outcome.error
                            Log.e(TAG, "Get schedule failed. code: ${error.code} message: ${error.message}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(
                                    R.string.msg_get_schedule_failed,
                                    error.message
                                )
                            )
                        }
                    }
                }
            }
        }
}

private object  GetRecordingAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_get_recording) {
            context.onGetRecording { outcome ->
                context.coroutineScope.launch {
                    when (outcome) {
                        is GetRecordingResult.Success -> {
                            Log.i(TAG, "Get recording success. Result: ${outcome.recording}")
                            context.snackbarHostState.showSnackbar(
                                outcome.recording.toString()
                            )
                        }
                        is GetRecordingResult.Failure -> {
                            val error = outcome.error
                            Log.e(TAG, "Get recording failed. code: ${error.code} message: ${error.message}")
                            context.snackbarHostState.showSnackbar(
                                context.context.getString(
                                    R.string.msg_get_recording_failed,
                                    error.message
                                )
                            )
                        }
                    }
                }
            }
        }
}

private object  CameraAction : ChatAction {
    override fun create(context: ChatActionContext): ActionButtonSpec =
        ActionButtonSpec(R.string.btn_camera) {
            val existingCameraView = context.publishedCameraView
            if (existingCameraView != null) {
                Log.i(TAG, "Camera preview already visible. Removing existing renderer.")
                context.setPublishedCameraView(null)
                context.onUnpublishCamera()
                return@ActionButtonSpec
            }

            val publishCamera: () -> Unit = {
                context.onPublishCamera { result ->
                    context.coroutineScope.launch {
                        when (result) {
                            is PublishCameraResult.Success -> {
                                val cameraRenderer = result.cameraView.cameraView
                                Log.i(TAG, "Publish camera result: $cameraRenderer")
                                if (cameraRenderer != null) {
                                    context.setPublishedCameraView(cameraRenderer)
                                } else {
                                    Log.e(TAG, "Publish camera succeeded with null view.")
                                    context.snackbarHostState.showSnackbar(
                                        context.context.getString(
                                            R.string.msg_publish_camera_failed,
                                            context.context.getString(R.string.msg_unknown_error)
                                        )
                                    )
                                }
                            }
                            is PublishCameraResult.Failure -> {
                                val error = result.error
                                Log.e(TAG, "Publish camera failed. Message: ${error.message}")
                                context.snackbarHostState.showSnackbar(
                                    context.context.getString(
                                        R.string.msg_publish_camera_failed,
                                        error.message ?: context.context.getString(R.string.msg_unknown_error)
                                    )
                                )
                            }

                            is PublishCameraResult.Unpublished -> {}
                        }
                    }
                }
            }

            if (context.hasCameraPermission()) {
                publishCamera()
            } else {
                context.requestCameraPermission { granted ->
                    if (granted) {
                        publishCamera()
                    } else {
                        context.onCameraPermissionDenied()
                    }
                }
            }
        }
}

private const val TAG = "WsChatScreen"
