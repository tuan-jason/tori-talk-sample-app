package com.torilab.socket.ui.chat

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.castalk.socket.TalkSession
import com.torilab.socket.R
import com.torilab.socket.WSS_URL_DEV
import com.torilab.socket.WSS_URL_UAT

@Composable
fun WsConnectScreen(
    wssUrl: String,
    talkSession: TalkSession
) {
    var url by rememberSaveable { mutableStateOf(wssUrl) } // keep your input if you like
    var isConnecting by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun doConnect() {
        error = null
        isConnecting = true

        // If your SocketManager exposes callbacks/flows, hook them here.
        // Below we simulate a quick success path (replace with your real callback).
        val effectiveUrl = url.trim().ifEmpty { wssUrl }   // <— fallback to default
        Log.i("WsConnectScreen", "effectiveUrl: $effectiveUrl")
        talkSession.connect(socketUrl = effectiveUrl, customHeaders = emptyMap())
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                placeholder = { Text("Enter wss url") },
                singleLine = true,
                enabled = !isConnecting,
                isError = error != null,
                supportingText = { if (error != null) Text(error!!) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            val environmentToggleState = remember(url) {
                EnvironmentToggleState(
                    devUrl = WSS_URL_DEV,
                    uatUrl = WSS_URL_UAT,
                    initialUrl = url
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        url = environmentToggleState.toggle()
                        error = null
                    },
                    enabled = !isConnecting,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF6954B7)
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 88.dp)
                ) {
                    Text(environmentToggleState.toggleButtonLabel)
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { doConnect() },
                    enabled = !isConnecting,              // <— was: url.isNotBlank() && !isConnecting
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6954B7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 120.dp)
                ) {
                    Text(stringResource(R.string.btn_connect))
                }
            }
        }

        if (isConnecting) {
            Dialog(onDismissRequest = { /* block */ }) {
                Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 6.dp) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.status_connecting))
                    }
                }
            }
        }
    }
}
