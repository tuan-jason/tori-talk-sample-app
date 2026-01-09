package com.torilab.socket.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class JsonHelper (private val moshi: Moshi) {

    fun <T : Any> toJson(obj: T?, clazz: Class<T>): String {
        return if (null == obj) {
            ""
        } else {

            val adapter = moshi.adapter(clazz)
            adapter.toJson(obj)
        }
    }

    fun <T : Any> toObject(strJson: String?, clazz: Class<T>): T? {
        return if (!strJson.isNullOrEmpty()) {
            val adapter = moshi.adapter(clazz)
            adapter.fromJson(strJson)
        } else {
            null
        }
    }

    fun <T : Any> toArray(strJson: String?, clazz: Class<T>): List<T>? {
        return if (!strJson.isNullOrEmpty()) {
            val type = Types.newParameterizedType(MutableList::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            adapter.fromJson(strJson)
        } else {
            null
        }
    }
}