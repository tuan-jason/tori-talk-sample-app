package com.torilab.socket.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.torilab.socket.R

@Composable
fun BoxScope.AudioOnlyPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
    Text(
        text = stringResource(R.string.audio_only_mode_label),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp)
    )
}
