package com.torilab.socket.ui.chat

/**
 * Holds the logic for toggling between the DEV and UAT websocket URLs.
 */
internal class EnvironmentToggleState(
    private val devUrl: String,
    private val uatUrl: String,
    initialUrl: String = devUrl
) {

    var currentUrl: String = initialUrl
        private set

    val toggleButtonLabel: String
        get() = if (currentUrl == devUrl) "UAT" else "DEV"

    fun toggle(): String {
        currentUrl = if (currentUrl == devUrl) {
            uatUrl
        } else {
            devUrl
        }
        return currentUrl
    }
}
