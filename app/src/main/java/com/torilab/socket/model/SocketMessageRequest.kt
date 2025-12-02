package com.torilab.socket.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class SocketMessageRequest(

    @Json(name = "id")
    var id: String? = null,

    @Json(name = "chat_group_id")
    var chatGroupId: String? = null,

    @Json(name = "language")
    var language: String? = null,

    @Json(name = "auth")
    var auth: String? = null,

    @Json(name = "is_recording")
    var isRecording: Boolean? = null,

    @Json(name = "data")
    var data: String? = null,

    @Json(name = "is_chat")
    var isChat: Boolean = true,

    @Json(name = "user_emotion_code")
    var voiceEmotion: Int? = null

) : Parcelable