package com.torilab.socket.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SocketMessageRequestTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(SocketMessageRequest::class.java)

    @Test
    fun `defaults keep chat mode enabled`() {
        val request = SocketMessageRequest()
        assertTrue(request.isChat)
    }

    @Test
    fun `moshi serialization preserves data`() {
        val request = SocketMessageRequest(
            id = "id-1",
            chatGroupId = "cg",
            language = "en",
            auth = "token",
            isRecording = true,
            data = "payload",
            isChat = false,
            voiceEmotion = 42
        )

        val json = adapter.toJson(request)
        val restored = adapter.fromJson(json)

        assertEquals(request, restored)
    }

    @Test
    fun `parcelable round trip works`() {
        val request = SocketMessageRequest(
            id = "parcel",
            chatGroupId = "g",
            language = "fr",
            auth = "auth",
            isRecording = false,
            data = "data",
            isChat = true,
            voiceEmotion = 7
        )

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = parcelableCreator(SocketMessageRequest::class.java).createFromParcel(parcel)
        parcel.recycle()

        requireNotNull(restored) { "Failed to unparcel SocketMessageRequest" }
        assertEquals(request, restored)
        assertNotSame("Creator must return a new instance", request, restored)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parcelableCreator(clazz: Class<T>): Parcelable.Creator<T> {
        val field = clazz.getDeclaredField("CREATOR")
        return field.get(null) as Parcelable.Creator<T>
    }
}
