package com.example.websocketapp

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SampleWebSocketListener : WebSocketListener() {
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("LOG_TAG---", "MainActivity#onClosed-16: ")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("LOG_TAG---", "MainActivity#onClosing-21: $webSocket $code $reason")
        webSocket.close(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d("LOG_TAG---", "MainActivity#onFailure-26: $webSocket $t $response")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("LOG_TAG---", "MainActivity#onMessage-30: $webSocket $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("LOG_TAG---", "MainActivity#onMessage-34: $webSocket $bytes")
        onMessage(webSocket, "" + bytes.hex())
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("LOG_TAG---", "MainActivity#onOpen-39: $webSocket, $response")
        webSocket.send("test")
    }
}