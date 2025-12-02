package com.torilab.socket

import com.castalk.socket.data.model.SessionEvent

class SessionEventReducer(
    private val chatViewModel: ChatViewModel
) {

    fun handle(event: SessionEvent) {
        when (event) {
            is SessionEvent.Connected -> {
                chatViewModel.setConnected(true)
                chatViewModel.appendFromServer("SocketConnected")
            }

            is SessionEvent.Disconnected -> {
                chatViewModel.setConnected(false)
                chatViewModel.appendFromServer("SocketDisconnected")
            }

            is SessionEvent.RetryConnectSuccess -> {
                chatViewModel.setConnected(true)
                chatViewModel.appendFromServer("SocketRetryConnectSuccess")
            }

            is SessionEvent.RetryConnectFailed -> {
                chatViewModel.setConnected(false)
                chatViewModel.appendFromServer("SocketRetryConnectFailed")
            }

            is SessionEvent.TextMessage -> chatViewModel.appendFromServer(event.msg.content)

            is SessionEvent.VideoMessage -> chatViewModel.appendFromServer(event.msg.content)

            is SessionEvent.IsResponding -> chatViewModel.appendFromServer("Responding....")

            is SessionEvent.GeneralError -> chatViewModel.appendFromServer("There's an error: ${event.error}")

            is SessionEvent.StreamingStop -> chatViewModel.appendFromServer("Video streaming stopped.")

            is SessionEvent.FaceEmotion -> chatViewModel.appendFromServer("Got face emotion: ${event.msg}")

            is SessionEvent.TextEmotion -> chatViewModel.appendFromServer("Got text emotion: ${event.msg}")

            SessionEvent.RetryingConnect -> Unit
        }
    }
}
