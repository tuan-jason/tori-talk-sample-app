package com.torilab.socket.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.TranscriptOverlay(transcripts: List<String>) {
    if (transcripts.isEmpty()) {
        return
    }
    val transcriptListState = rememberLazyListState()
    LaunchedEffect(transcripts.size) {
        transcriptListState.animateScrollToItem(index = transcripts.lastIndex)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart)
            .padding(16.dp),
        state = transcriptListState
    ) {
        items(transcripts) { transcript ->
            Text(
                text = transcript,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
