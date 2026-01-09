package com.torilab.socket.ui.chat

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.castalk.socket.ITalkSession
import com.castalk.socket.data.model.camera.CameraView
import com.castalk.socket.data.model.camera.PublishCameraResult
import com.torilab.socket.R
import com.torilab.socket.call.VideoCallVisualState
import com.castalk.socket.ui.CameraPreviewRenderer
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChatActionRegistryTest {

    private val talkSession = mockk<ITalkSession>(relaxed = true)
    private val appContext = mockk<Context>(relaxed = true)
    private val videoCallVisualState = VideoCallVisualState()
    private val snackbarHostState = SnackbarHostState()
    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private fun buildContext(
        showCallControls: Boolean,
        isMicrophoneMuted: Boolean,
        publishedCameraView: CameraPreviewRenderer? = null,
        onPublishCamera: ((PublishCameraResult) -> Unit) -> Unit = { },
        onUnpublishCamera: () -> Unit = {},
        setPublishedCameraView: (CameraPreviewRenderer?) -> Unit = {},
        hasCameraPermission: () -> Boolean = { true },
        requestCameraPermission: ((Boolean) -> Unit) -> Unit = { _ -> },
        onCameraPermissionDenied: () -> Unit = {}
    ): ChatActionContext {
        val outgoingHolder = AtomicInteger(0)
        return ChatActionContext(
            talkSession = talkSession,
            outgoingText = "message-${outgoingHolder.incrementAndGet()}",
            onOutgoingTextChanged = {},
            onSend = {},
            onClearTranscripts = {},
            onSendVideoMessage = { _, _, _ -> },
            videoCallVisualState = videoCallVisualState,
            startVideoCallWithPermission = {},
            coroutineScope = coroutineScope,
            snackbarHostState = snackbarHostState,
            context = appContext,
            isMicrophoneMuted = isMicrophoneMuted,
            setIsMicrophoneMuted = {},
            isMicrophoneToggleInProgress = false,
            setIsMicrophoneToggleInProgress = {},
            onGetSceneInfo = { _, _ -> },
            onSceneSkipped = { _, _ -> },
            onScenePinned = { _, _ -> },
            onSceneUnpinned = { _ -> },
            onGetSchedule = { _ -> },
            onGetRecording = { _ -> },
            onPublishCamera = onPublishCamera,
            publishedCameraView = publishedCameraView,
            setPublishedCameraView = setPublishedCameraView,
            onUnpublishCamera = onUnpublishCamera,
            onDisconnected = {},
            showCallControls = showCallControls,
            hasCameraPermission = hasCameraPermission,
            requestCameraPermission = requestCameraPermission,
            onCameraPermissionDenied = onCameraPermissionDenied
        )
    }

    @Test
    fun `call control actions are hidden when renderer is absent`() {
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(showCallControls = false, isMicrophoneMuted = false)
        )

        assertFalse(
            specs.any {
                it.labelResId == R.string.btn_mute_microphone ||
                    it.labelResId == R.string.btn_unmute_microphone ||
                    it.labelResId == R.string.btn_get_scene ||
                    it.labelResId == R.string.btn_skip_scene ||
                    it.labelResId == R.string.btn_pin_scene ||
                    it.labelResId == R.string.btn_unpin_scene
            }
        )
    }

    @Test
    fun `microphone button reflects mute state when controls visible`() {
        val mutedSpecs = ChatActionRegistry.buildActionButtons(
            buildContext(showCallControls = true, isMicrophoneMuted = true)
        )
        assertTrue(mutedSpecs.any { it.labelResId == R.string.btn_unmute_microphone })

        val unmutedSpecs = ChatActionRegistry.buildActionButtons(
            buildContext(showCallControls = true, isMicrophoneMuted = false)
        )
        assertTrue(unmutedSpecs.any { it.labelResId == R.string.btn_mute_microphone })
    }

    @Test
    fun `unpin action becomes available when call controls visible`() {
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(showCallControls = true, isMicrophoneMuted = false)
        )

        assertTrue(specs.any { it.labelResId == R.string.btn_unpin_scene })
    }

    @Test
    fun `camera action publishes view when not visible`() {
        val previewRenderer = mockk<CameraPreviewRenderer>(relaxed = true)
        var capturedCallback: ((PublishCameraResult) -> Unit)? = null
        var publishedRenderer: CameraPreviewRenderer? = null
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(
                showCallControls = true,
                isMicrophoneMuted = false,
                publishedCameraView = null,
                onPublishCamera = { callback -> capturedCallback = callback },
                setPublishedCameraView = { publishedRenderer = it }
            )
        )

        val cameraSpec = specs.first { it.labelResId == R.string.btn_camera }
        cameraSpec.onClick.invoke()

        val resultCallback = capturedCallback ?: error("Camera callback was not captured")
        resultCallback.invoke(
            PublishCameraResult.Success(
                CameraView(cameraView = previewRenderer)
            )
        )

        assertSame(previewRenderer, publishedRenderer)
    }

    @Test
    fun `camera action unpublishes when view already visible`() {
        val previewRenderer = mockk<CameraPreviewRenderer>(relaxed = true)
        var unpublishCalls = 0
        var trackedRenderer: CameraPreviewRenderer? = previewRenderer
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(
                showCallControls = true,
                isMicrophoneMuted = false,
                publishedCameraView = previewRenderer,
                onPublishCamera = { fail("Should not attempt to publish when already visible") },
                onUnpublishCamera = { unpublishCalls++ },
                setPublishedCameraView = { trackedRenderer = it }
            )
        )

        val cameraSpec = specs.first { it.labelResId == R.string.btn_camera }
        cameraSpec.onClick.invoke()

        assertEquals(1, unpublishCalls)
        assertNull(trackedRenderer)
    }

    @Test
    fun `camera action requests permission before publishing`() {
        var permissionRequested = false
        var permissionCallback: ((Boolean) -> Unit)? = null
        var publishInvocations = 0
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(
                showCallControls = true,
                isMicrophoneMuted = false,
                onPublishCamera = { _ ->
                    publishInvocations++
                },
                hasCameraPermission = { false },
                requestCameraPermission = { callback ->
                    permissionRequested = true
                    permissionCallback = callback
                }
            )
        )

        val cameraSpec = specs.first { it.labelResId == R.string.btn_camera }
        cameraSpec.onClick.invoke()

        assertTrue(permissionRequested)
        assertEquals(0, publishInvocations)

        val requestCallback = permissionCallback ?: error("Permission callback not captured")
        requestCallback(true)

        assertEquals(1, publishInvocations)
    }

    @Test
    fun `camera action shows snackbar when permission denied`() {
        var permissionCallback: ((Boolean) -> Unit)? = null
        var snackbarCalls = 0
        var publishInvocations = 0
        val specs = ChatActionRegistry.buildActionButtons(
            buildContext(
                showCallControls = true,
                isMicrophoneMuted = false,
                onPublishCamera = { _ ->
                    publishInvocations++
                },
                hasCameraPermission = { false },
                requestCameraPermission = { callback -> permissionCallback = callback },
                onCameraPermissionDenied = { snackbarCalls++ }
            )
        )

        val cameraSpec = specs.first { it.labelResId == R.string.btn_camera }
        cameraSpec.onClick.invoke()

        val requestCallback = permissionCallback ?: error("Permission callback not captured")
        requestCallback(false)

        assertEquals(0, publishInvocations)
        assertEquals(1, snackbarCalls)
    }
}
