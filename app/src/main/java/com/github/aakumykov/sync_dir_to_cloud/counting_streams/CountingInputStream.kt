package com.github.aakumykov.sync_dir_to_cloud.counting_streams

import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

class CountingInputStream(
    private val inputStream: InputStream,
    private val readingCallback: ReadingCallback
) : InputStream() {

    private var readedBytesCount: Long = 0

    @Throws(IOException::class)
    override fun read(): Int {
        return inputStream.read().let { justReadByte ->
            summarizeAndCallBack(1)
            justReadByte
        }
    }

    override fun read(b: ByteArray?): Int {
        return super.read(b).let { justReadCount ->
            summarizeAndCallBack(justReadCount)
            justReadCount
        }
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return super.read(b, off, len).let { justReadCount ->
            summarizeAndCallBack(justReadCount)
            justReadCount
        }
    }


    private fun summarizeAndCallBack(count: Int) {
        readedBytesCount += count
        readingCallback.onReadCountChanged(readedBytesCount)
    }


    fun interface ReadingCallback {
        fun onReadCountChanged(count: Long)
    }
}