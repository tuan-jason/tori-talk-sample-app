package com.torilab.socket.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.castalk.socket.CallOption
import com.castalk.socket.ITalkSession
import com.castalk.socket.StartVideoCallResult
import com.castalk.socket.data.model.camera.PublishCameraResult
import com.castalk.socket.data.model.recording.GetRecordingResult
import com.castalk.socket.data.model.scene.GetScheduleResult
import com.castalk.socket.data.model.scene.PinSceneResult
import com.castalk.socket.data.model.scene.SkipSceneResult
import com.castalk.socket.data.model.scene.UnpinSceneResult
import com.castalk.socket.ui.CameraPreviewRenderer
import com.torilab.android.common.JsonHelper
import com.torilab.android.common.scene.SceneInfo
import com.torilab.socket.R
import com.torilab.socket.call.VideoCallVisualState
import com.torilab.socket.model.SocketMessageRequest
import com.torilab.socket.ui.chat.components.AudioOnlyPlaceholder
import com.torilab.socket.ui.chat.components.TranscriptOverlay
import java.util.UUID
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main chat surface showing call controls and action buttons.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun WsChatScreen(
    talkSession: ITalkSession,
    onDisconnected: () -> Unit,
    jsonHelper: JsonHelper,
    messages: List<AnnotatedString>,
    transcripts: List<String>,
    onSend: (String) -> Unit,
    onClearTranscripts: () -> Unit,
    onSendVideoMessage: (String, Int?, Boolean?) -> Unit,
    onGetSceneInfo: (payload: String?, callback: (Result<SceneInfo>) -> Unit) -> Unit,
    onSceneSkipped: (nextVideoId: String?, callback: (SkipSceneResult) -> Unit) -> Unit,
    onScenePinned: (pinVideoId: String?, callback: (PinSceneResult) -> Unit) -> Unit,
    onSceneUnpinned: (callback: (UnpinSceneResult) -> Unit) -> Unit,
    onGetSchedule: (callback: (GetScheduleResult) -> Unit) -> Unit,
    onGetRecording: (callback: (GetRecordingResult) -> Unit) -> Unit,
    onPublishCamera: (callback: (PublishCameraResult) -> Unit) -> Unit,
    onUnpublishCamera: () -> Unit
) {

    val TAG = "WsChatScreen"
    // Minimal chat UI (keeps your existing look & feel)
    var outgoing by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val videoCallVisualState = remember { VideoCallVisualState() }
    val videoRenderer = videoCallVisualState.videoRenderer
    val isAudioOnlyPlaceholderVisible = videoCallVisualState.isAudioOnlyPlaceholderVisible
    var isMicrophoneMuted by rememberSaveable(talkSession) {
        mutableStateOf(talkSession.isMicrophoneMuted() ?: false)
    }
    var isMicrophoneToggleInProgress by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var cameraPreviewRenderer by remember { mutableStateOf<CameraPreviewRenderer?>(null) }
    val cameraPreviewOverlayState = rememberCameraPreviewOverlayState()
    LaunchedEffect(videoRenderer) {
        if (videoRenderer == null) {
            cameraPreviewRenderer = null
        }
    }
    LaunchedEffect(cameraPreviewRenderer) {
        if (cameraPreviewRenderer == null) {
            cameraPreviewOverlayState.reset()
        }
    }
    val videoCallOptions = CallOption(
        lastVoiceCode = "252",
        manualVoiceSwap = true,
        lastRoomName = "",
        enableMicrophone = false,
        enableCamera = false,
        enableVideo = true
    )
    val performStartVideoCall: () -> Unit = {
        Log.d(TAG, "Calling startVideoCall with session: $talkSession")
        videoCallVisualState.onStartVideoCallRequested()
        talkSession.startVideoCall(
            options = videoCallOptions
        ) { result ->
            videoCallVisualState.onStartVideoCallResultHandled()
            when (result) {
                is StartVideoCallResult.Success -> {
                    val callData = result.callData
                    Log.i(TAG, "startVideoCall success: ${callData.videoView}")
                    videoCallVisualState.onCallStarted(callData.videoView)
                    onSend.invoke("LiveKit video renderer is ready.")
                }
                is StartVideoCallResult.Failure -> {
                    val error = result.error
                    Log.e(TAG, "startVideoCall failed code: ${error.code} message: ${error.message}")
                    onSend.invoke("Start Call exception: ${error.message}")
                    talkSession.stopVideoCall()
                    when (error.code) {
                        -4435 -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = error.message
                                )
                            }
                        }
                        else -> videoCallVisualState.onCallFailed()
                    }
                }
            }
        }
    }
    val openAppSettings: () -> Unit = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    val showPermissionDeniedSnackbar: () -> Unit = {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.permission_call_required),
                actionLabel = context.getString(R.string.permission_settings_action),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                openAppSettings()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performStartVideoCall()
        } else {
            showPermissionDeniedSnackbar()
        }
    }
    var pendingPublishCameraPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val publishCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = pendingPublishCameraPermissionCallback
        pendingPublishCameraPermissionCallback = null
        callback?.invoke(granted)
    }
    val startVideoCall: () -> Unit = {
        if (videoCallOptions.enableCamera) {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCameraPermission) {
                performStartVideoCall()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            performStartVideoCall()
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVideoCall()
        } else {
            showPermissionDeniedSnackbar()
        }
    }

    LaunchedEffect(videoRenderer) {
        if (videoRenderer == null) {
            isMicrophoneMuted = false
            isMicrophoneToggleInProgress = false
        } else {
            talkSession.isMicrophoneMuted()?.let { isMicrophoneMuted = it }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 8.dp, end = 8.dp)
            ) {

                val showCallControls = videoRenderer != null || isAudioOnlyPlaceholderVisible
                val startVideoCallWithPermission = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        startVideoCall()
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                val hasPublishCameraPermission = {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                }
                val requestPublishCameraPermission: ((Boolean) -> Unit) -> Unit = { callback ->
                    pendingPublishCameraPermissionCallback = callback
                    publishCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                val actionContext = ChatActionContext(
                    talkSession = talkSession,
                    outgoingText = outgoing,
                    onOutgoingTextChanged = { outgoing = it },
                    onSend = onSend,
                    onClearTranscripts = onClearTranscripts,
                    onSendVideoMessage = onSendVideoMessage,
                    videoCallVisualState = videoCallVisualState,
                    startVideoCallWithPermission = startVideoCallWithPermission,
                    coroutineScope = coroutineScope,
                    snackbarHostState = snackbarHostState,
                    context = context,
                    isMicrophoneMuted = isMicrophoneMuted,
                    setIsMicrophoneMuted = { isMicrophoneMuted = it },
                    isMicrophoneToggleInProgress = isMicrophoneToggleInProgress,
                    setIsMicrophoneToggleInProgress = { isMicrophoneToggleInProgress = it },
                    onGetSceneInfo = onGetSceneInfo,
                    onSceneSkipped = onSceneSkipped,
                    onScenePinned = onScenePinned,
                    onSceneUnpinned = onSceneUnpinned,
                    onGetSchedule = onGetSchedule,
                    onGetRecording = onGetRecording,
                    onPublishCamera = onPublishCamera,
                    publishedCameraView = cameraPreviewRenderer,
                    setPublishedCameraView = { renderer -> cameraPreviewRenderer = renderer },
                    onUnpublishCamera = onUnpublishCamera,
                    onDisconnected = onDisconnected,
                    showCallControls = showCallControls,
                    hasCameraPermission = hasPublishCameraPermission,
                    requestCameraPermission = requestPublishCameraPermission,
                    onCameraPermissionDenied = showPermissionDeniedSnackbar
                )
                val actionButtons = ChatActionRegistry.buildActionButtons(actionContext)

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(ActionButtonTokens.buttonSpacing),
            verticalArrangement = Arrangement.spacedBy(ActionButtonTokens.buttonSpacing),
        ) {
            actionButtons.forEachIndexed { index, actionButton ->
                Button(
                    onClick = actionButton.onClick,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionButtonTokens.colorFor(index),
                        contentColor = Color.White
                    ),
                    enabled = actionButton.enabled,
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = ActionButtonTokens.minButtonWidth,
                            minHeight = ActionButtonTokens.buttonHeight
                        )
                ) {
                    Text(
                        text = stringResource(actionButton.labelResId),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 3
                    )
                }
            }
        }

                val callRendererModifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 16.dp)

                    if (videoRenderer != null) {
                        val renderer = videoRenderer
                        Log.d(TAG, "Adding video rendered to screen: $renderer")
                        Box(
                            modifier = callRendererModifier
                        ) {
                            // Video renderer at the bottom
                            AndroidView(
                                factory = {
                                    (renderer.parent as? ViewGroup)?.removeView(renderer)
                                    renderer
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            TranscriptOverlay(transcripts)

                            val overlayRenderer = cameraPreviewRenderer
                            Log.v(TAG, "Got cameraPreviewRender: $cameraPreviewRenderer")
                            if (overlayRenderer != null) {
                                val overlayView = overlayRenderer as? View
                                if (overlayView != null) {
                                    key(overlayRenderer) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .offset {
                                                    IntOffset(
                                                        cameraPreviewOverlayState.offset.x.roundToInt(),
                                                        cameraPreviewOverlayState.offset.y.roundToInt()
                                                    )
                                                }
                                                .pointerInput(overlayRenderer) {
                                                    detectDragGestures { change, dragAmount ->
                                                        @Suppress("DEPRECATION")
                                                        change.consumeAllChanges()
                                                        cameraPreviewOverlayState.dragBy(dragAmount)
                                                    }
                                                }
                                        ) {
                                            AndroidView(
                                                factory = {
                                                    (overlayView.parent as? ViewGroup)?.removeView(overlayView)
                                                    overlayView
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Camera preview renderer is not a View: $overlayRenderer")
                                }
                            }
                        }
                } else if (isAudioOnlyPlaceholderVisible) {
                    Box(
                        modifier = callRendererModifier
                    ) {
                        AudioOnlyPlaceholder()
                        TranscriptOverlay(transcripts)
                    }
                } else {
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

            if (videoCallVisualState.isStartVideoCallInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
