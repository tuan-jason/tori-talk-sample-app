package com.torilab.socket.ui.chat

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.castalk.socket.TalkSession
import com.castalk.socket.extensions.JsonHelper
import com.squareup.moshi.Moshi

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
            onSend = { /* no-op */ }
        )
    }
}