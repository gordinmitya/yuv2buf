package ru.gordinmitya.yuv2buf_demo

import android.graphics.Bitmap
import android.os.Handler
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleObserver

sealed class ConversionResult(val method: String) {
    class Success(
        method: String,
        val image: Bitmap,
        val colorTime: Long,
        val rotateTime: Long
    ) : ConversionResult(method)

    class Failed(method: String) : ConversionResult(method)
}

interface ImageConverter {
    fun getName(): String
    fun convert(image: ImageProxy): ConversionResult
}

class CompositeConverter(
    private val listener: Listener,
    private val handler: Handler,
    private vararg val functions: ImageConverter
) : ImageAnalysis.Analyzer, LifecycleObserver {

    override fun analyze(image: ImageProxy) {
        val results: List<ConversionResult> = functions.map { converter ->
            image.planes.forEach { plane -> plane.buffer.rewind() }
            try {
                converter.convert(image)
            } catch (e: RuntimeException) {
                ConversionResult.Failed(converter.getName())
            }
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
