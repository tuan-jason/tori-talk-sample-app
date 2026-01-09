package com.torilab.socket.ui.chat

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.castalk.socket.TalkSession
import com.castalk.socket.data.model.scene.PinSceneResult
import com.castalk.socket.data.model.scene.SkipSceneResult
import com.castalk.socket.data.model.scene.UnpinSceneResult
import com.squareup.moshi.Moshi
import com.torilab.android.common.JsonHelper

@Preview(
    name = "WsChatScreen – Light",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "WsChatScreen – Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_WsChatScreen() {
    MaterialTheme {
        WsChatScreen(
            talkSession = TalkSession(),
            onDisconnected = { /* no-op */ },
            jsonHelper = JsonHelper(Moshi.Builder().build()),
            messages = listOf(
                buildAnnotatedString { append("Hello 👋",
                    "This is a sample message in preview",
                    "Compose makes UI fun 🚀") }
            ),
            transcripts = emptyList(),
            onSend = { /* no-op */ },
            onClearTranscripts = { /* no-op */ },
            onSendVideoMessage = { msg, code, markDown -> /* no-op */ },
            onGetSceneInfo = { _, _ -> },
            onSceneSkipped = { _, callback ->
                callback(SkipSceneResult.Success(Unit))
            },
            onScenePinned = { _, callback ->
                callback(PinSceneResult.Success(payload = Unit))
            },
            onSceneUnpinned = { callback ->
                callback(UnpinSceneResult.Success(payload = Unit))
            },
            onGetSchedule = { _ ->
            },
            onGetRecording = { _ ->
            },
            onPublishCamera = {},
            onUnpublishCamera = {}
        )
    }
}
