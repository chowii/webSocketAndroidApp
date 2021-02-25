package com.example.websocketapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.websocketapp.camera2.Camera2BasicFragment
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket

    private lateinit var videoView: VideoView
    private lateinit var surfaceView: SurfaceView
    private lateinit var camera2Fragment: Camera2BasicFragment
    private val compsiteDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        videoView = findViewById(R.id.videoView)
        surfaceView = findViewById(R.id.surfaceView)
        val request = Request.Builder().url("ws://192.168.0.6:3000").build()
        camera2Fragment = Camera2BasicFragment.newInstance()
        val listener = SampleWebSocketListener(object :
            SampleWebSocketListener.WebSocketMessageListener<ImageData> {
            override fun onMessage(t: ImageData) {
                camera2Fragment.showImage(t)
            }
            override fun onClosing() {
                camera2Fragment.onSocketClosed()
            }
        })
        webSocket = OkHttpClient().newWebSocket(request, listener)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, camera2Fragment)
            .commit()
        listener.flowableSocketMessage
//            .debounce(100, TimeUnit.MILLISECONDS)
            .doAfterNext {
                Log.d("LOG_TAG---", "MainActivity#flowableSocketMessage-58: Thread-> ${Thread.currentThread().name}")
            }
//            .subscribeOn(Schedulers.computation())
//            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Log.e("LOG_TAG---", "MainActivity#flowableSocketMessage-63: Thread-> ${Thread.currentThread().name}", it)
                ImageData(ByteArray(6000).toList())
            }
            .subscribe {
//                Log.d("LOG_TAG---", "MainActivity#flowableSocketMessage-67: showImage ${Thread.currentThread().name}")
                camera2Fragment.showImage(it)
            }
            .also {
                compsiteDisposable.add(it)
            }

        camera2Fragment.flowableImageByte
            .doAfterNext {
                System.gc()
//                Log.d("LOG_TAG---", "MainActivity#flowableImageByte-77: Thread-> ${Thread.currentThread().name}")
            }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
//                Log.d("LOG_TAG---", "MainActivity#flowableImageByte-81: Thread-> ${Thread.currentThread().name}")
                null
            }
            .subscribe { bitmap ->
                Log.d("LOG_TAG---", "MainActivity#flowableImageByte-86: sendSocketMessage ${Thread.currentThread().name}")
                Matrix().apply {
                    postRotate(-90F)
                }.let {
                    Bitmap.createBitmap(bitmap, 0,0, bitmap.width, bitmap.height, it, true)
                }.let {
                    ByteArrayOutputStream().apply {
                        it.compress(Bitmap.CompressFormat.JPEG, 50, this)
                    }
                }.also {
                    sendSocketMessage(it.toByteArray())
                }
            }
            .also {
                compsiteDisposable.add(it)
            }

    }

    companion object {
        @JvmField
        var TAG: String = this::class.java.name
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

    fun sendSocketMessage(bytes: ByteArray) {
        val imageData = ImageData(bytes.toList())
        val json = Gson().toJson(imageData, ImageData::class.java)
        val isSuccess = webSocket.send(json)
        Log.d("LOG_TAG---", "MainActivity#sendSocketMessage-116: isSuccess=$isSuccess")
//        Log.d("LOG_TAG---", "MainActivity#sendSocketMessage-116: " + Thread.currentThread().name)
//        Log.d("LOG_TAG---", "MainActivity#sendSocketMessage-116: ${imageData.byteData.size} ${imageData.time} $isSuccess ${webSocket.queueSize()}")
    }
}

data class ImageData(val byteData: List<Byte>, val time: Long = System.currentTimeMillis())