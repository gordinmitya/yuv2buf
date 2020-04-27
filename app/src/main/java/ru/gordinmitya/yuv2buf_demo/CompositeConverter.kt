package ru.gordinmitya.yuv2buf_demo

import android.graphics.Bitmap
import android.os.Handler
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleObserver

class ConversionResult(val method: String, val image: Bitmap, val time: Long)
interface ImageConverter {
    fun getName(): String
    fun convert(image: ImageProxy): Bitmap
}

class CompositeConverter(
    private val listener: Listener,
    private val handler: Handler,
    private vararg val functions: ImageConverter
) :
    ImageAnalysis.Analyzer, LifecycleObserver {

    override fun analyze(image: ImageProxy) {
        val results = functions.map {
            val start = System.currentTimeMillis()
            val bitmap = it.convert(image)
            val end = System.currentTimeMillis()
            ConversionResult(it.getName(), bitmap, end - start)
        }
        handler.post {
            listener.onAnalyzed(results)
        }
        image.close()
    }

    interface Listener {
        fun onAnalyzed(results: List<ConversionResult>)
    }
}