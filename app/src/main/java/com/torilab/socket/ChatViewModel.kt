package com.torilab.socket

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {
    // Compose observes this directly
    val messages = mutableStateListOf<AnnotatedString>()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun appendFromServer(text: String) = messages.add(buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF1565C0))) {
            append(text)
        }
    })
    fun appendOutgoing(text: String) = messages.add(buildAnnotatedString {
        append(text)
    })
    fun clear() = messages.clear()
}
