package com.example.websocketapp.camera2

import android.media.MediaDataSource
import android.util.Log


class ByteArrayDataSource : MediaDataSource() {

    private var byteBuffer: ByteArray? = null
        set(value) {
//            Log.d("LOG_TAG---", "ByteArrayDataSource#-11: ")
            field = value
        }
        get() {
            Log.d("LOG_TAG---", "ByteArrayDataSource#-15: ")
            return field
        }

    override fun close() {
        Log.d("LOG_TAG---", "ByteArrayDataSource#close-8: close")
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        var s = size
        if (byteBuffer != null) {

            synchronized(byteBuffer!!) {
                Log.d("LOG_TAG---", "ByteArrayDataSource#readAt-19: ")
                val length = byteBuffer!!.size
                if (position >= length) {
                    return -1
                }
                if (position + size > length) {
                    s -= (position.toInt() + size) - length;
                }
                System.arraycopy(byteBuffer, position.toInt(), buffer, offset, size)
                return size
            }
        }
        return 200632
    }

    override fun getSize(): Long {
        val bufferSize = byteBuffer?.size?.toLong() ?: 200632L
        Log.d("LOG_TAG---", "ByteArrayDataSource#getSize-32: $bufferSize")
        return bufferSize
    }

    fun setByteData(toByteArray: ByteArray) {
        byteBuffer = toByteArray
    }
}