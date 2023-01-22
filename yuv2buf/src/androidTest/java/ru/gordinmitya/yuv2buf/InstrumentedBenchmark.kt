package ru.gordinmitya.yuv2buf

import android.graphics.ImageFormat
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.gordinmitya.yuv2buf.MLKit_BitmapUtils.Image_Plane

inline fun <R> measure(block: () -> R): Pair<R, Long> {
    val tik = SystemClock.elapsedRealtimeNanos()
    val result = block()
    val tok = SystemClock.elapsedRealtimeNanos()
    return Pair(result, tok - tik)
}

@RunWith(AndroidJUnit4::class)
class InstrumentedBenchmark {
    private val iterations = 100

    private val width = 1280
    private val height = 720
    private val additionalRowStride = 1536

    private fun baseline(image: Yuv.ImageWrapper): Double {
        val reuseBuffer = Yuv.toBuffer(image, null).buffer
        val timings = ArrayList<Long>(iterations)
        repeat(iterations) { _ ->
            val (converted, time) = measure {
                Yuv.toBuffer(image, reuseBuffer)
            }
            assertTrue(YuvCommon.checkOutput(converted.type, converted.buffer, width, height))
            timings.add(time)
        }
        return timings.average()
    }

    @Test
    fun baseline_nv21_noRowStride() {
        val image = YuvCommon.make(ImageFormat.NV21, width, height, width)
        val avg = baseline(image)
        Log.d("baseline_nv21_noRowStride",  "$avg ns")
    }

    @Test
    fun baseline_nv21_withRowStride() {
        val image = YuvCommon.make(ImageFormat.NV21, width, height, additionalRowStride)
        val avg = baseline(image)
        Log.d("baseline_nv21_withRowStride",  "$avg ns")
    }

    @Test
    fun baseline_yuv420_noRowStride() {
        val image = YuvCommon.make(ImageFormat.YUV_420_888, width, height, width)
        val avg = baseline(image)
        Log.d("baseline_yuv420_noRowStride",  "$avg ns")
    }

    @Test
    fun baseline_yuv420_withRowStride() {
        val image = YuvCommon.make(ImageFormat.YUV_420_888, width, height, additionalRowStride)
        val avg = baseline(image)
        Log.d("baseline_yuv420_withRowStride",  "$avg ns")
    }

    private fun mlkit(image: Yuv.ImageWrapper): Double {
        val timings = ArrayList<Long>(iterations)
        val planes = arrayOf(
            Image_Plane(image.y.buffer, image.y.rowStride, image.y.pixelStride),
            Image_Plane(image.u.buffer, image.u.rowStride, image.u.pixelStride),
            Image_Plane(image.v.buffer, image.v.rowStride, image.v.pixelStride),
        )
        repeat(iterations) { _ ->
            val (buffer, time) = measure {
                MLKit_BitmapUtils.yuv420ThreePlanesToNV21(planes, image.width, image.height)
            }
            assertTrue(YuvCommon.checkOutput(ImageFormat.NV21, buffer, width, height))
            timings.add(time)
        }
        return timings.average()
    }

    @Test
    fun mlkit_nv21_noRowStride() {
        val image = YuvCommon.make(ImageFormat.NV21, width, height, width)
        val avg = mlkit(image)
        Log.d("mlkit_nv21_noRowStride",  "$avg ns")
    }

    @Test
    fun mlkit_nv21_withRowStride() {
        val image = YuvCommon.make(ImageFormat.NV21, width, height, additionalRowStride)
        val avg = mlkit(image)
        Log.d("mlkit_nv21_withRowStride",  "$avg ns")
    }

    @Test
    fun mlkit_yuv420_noRowStride() {
        val image = YuvCommon.make(ImageFormat.YUV_420_888, width, height, width)
        val avg = mlkit(image)
        Log.d("mlkit_yuv420_noRowStride",  "$avg ns")
    }

    @Test
    fun mlkit_yuv420_withRowStride() {
        val image = YuvCommon.make(ImageFormat.YUV_420_888, width, height, additionalRowStride)
        val avg = mlkit(image)
        Log.d("mlkit_yuv420_withRowStride",  "$avg ns")
    }
}
