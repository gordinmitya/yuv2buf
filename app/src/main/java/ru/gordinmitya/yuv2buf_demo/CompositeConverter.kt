package ru.gordinmitya.yuv2buf_demo

import android.graphics.Bitmap
import android.os.Handler
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleObserver

class ConversionResult(val method: String, val image: Bitmap, val colorTime: Long, val rotateTime: Long)
interface ImageConverter {
    fun getName(): String
    fun convert(image: ImageProxy): ConversionResult
}

class CompositeConverter(
    private val listener: Listener,
    private val handler: Handler,
    private vararg val functions: ImageConverter
) :
    ImageAnalysis.Analyzer, LifecycleObserver {

    override fun analyze(image: ImageProxy) {
        val results = functions.map { converter ->
            image.planes.forEach { plane -> plane.buffer.rewind() }
            converter.convert(image)
        }
        handler.post {
            val size =
                if (image.imageInfo.rotationDegrees > 0 && image.imageInfo.rotationDegrees % 90 == 0)
                    image.height to image.width
                else
                    image.width to image.height
            listener.onAnalyzed(size, results)
        }
        image.close()
    }

    interface Listener {
        fun onAnalyzed(size: Pair<Int, Int>, results: List<ConversionResult>)
    }
}