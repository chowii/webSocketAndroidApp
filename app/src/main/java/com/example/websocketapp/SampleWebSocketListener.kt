package com.example.websocketapp

import android.util.Log
import com.google.gson.Gson
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SampleWebSocketListener(
    private val messageListener: WebSocketMessageListener<ImageData>
) : WebSocketListener() {

    private val publishSubject = PublishSubject.create<ImageData>()

    val flowableSocketMessage: Flowable<ImageData> = publishSubject
        .subscribeOn(Schedulers.newThread())
        .toFlowable(BackpressureStrategy.LATEST)

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("LOG_TAG---", "MainActivity#onClosed-16: ")
        messageListener.onClosing()
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("LOG_TAG---", "MainActivity#onClosing-21: $webSocket $code $reason")
        webSocket.close(code, reason)
        publishSubject.onComplete()
        messageListener.onClosing()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("LOG_TAG---", "MainActivity#onFailure-26: $webSocket $t $response", t)
//        messageListener.onClosing()
        publishSubject.onError(t)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
//        Log.d("LOG_TAG---", "MainActivity#onMessage-30: $webSocket $text")
        val imageData = Gson().fromJson(text, ImageData::class.java)
        publishSubject.onNext(imageData)
        messageListener.onMessage(imageData)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//        Log.d("LOG_TAG---", "MainActivity#onMessage-34: $webSocket $bytes")
        onMessage(webSocket, "" + bytes.hex())
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("LOG_TAG---", "MainActivity#onOpen-39: $webSocket, $response")
        webSocket.send("test")
    }

    interface WebSocketMessageListener<T> {
        fun onClosing()
        fun onMessage(t: T)
    }
}