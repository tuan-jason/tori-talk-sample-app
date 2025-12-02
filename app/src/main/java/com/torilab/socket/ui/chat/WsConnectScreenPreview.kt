package com.torilab.socket.ui.chat


import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.castalk.socket.TalkSession

@Preview(
    name = "WsConnectScreen – Light",
    showBackground = true,
    widthDp = 360
)
@Preview(
    name = "WsConnectScreen – Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun Preview_WsConnectScreen() {
    MaterialTheme {
        WsConnectScreen(
            wssUrl = "wss://example.com/socket",
            talkSession = TalkSession()
        )
    }
}