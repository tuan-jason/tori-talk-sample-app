package com.torilab.socket.ui.chat

import androidx.annotation.StringRes

data class ActionButtonSpec(
    @field:StringRes val labelResId: Int,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)
