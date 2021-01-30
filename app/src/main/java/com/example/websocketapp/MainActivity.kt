package com.example.websocketapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        videoView = findViewById(R.id.videoView)
        val listener = object : WebSocketListener() {
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
        val request = Request.Builder().url("ws://192.168.0.6:3000").build()
        dispatchTakeVideoIntent()
        webSocket = OkHttpClient().newWebSocket(request, listener)
    }

    companion object {
        const val REQUEST_VIDEO_CAPTURE = 1
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            val videoUri: Uri? = intent?.data
            videoView.setVideoURI(videoUri)
            videoUri?.let {
                toByteStreamArray(it).asList()
                        .also {
                            val json = Gson().toJson(it, List::class.java)
                            Log.d("LOG_TAG---", "MainActivity#onActivityResult-75: $json")
                            val send = webSocket.send(json)
                            Log.d("LOG_TAG---", "MainActivity#onActivityResult-85: $send")
                        }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    fun toByteStreamArray(uri: Uri): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val inputStream = contentResolver.openInputStream(uri)

        val byteArray = ByteArray(1024)
        try {
            var n = 0
            while (n != -1) {
                if (inputStream != null) {
                    Log.d("LOG_TAG---", "MainActivity#toByteStreamArray-91: $n")
                    outputStream.write(byteArray, 0, n)
                    n = inputStream.read(byteArray)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace();
        } catch (e: IOException) {
            e.printStackTrace();
        }
        return outputStream.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "end")
    }
}
