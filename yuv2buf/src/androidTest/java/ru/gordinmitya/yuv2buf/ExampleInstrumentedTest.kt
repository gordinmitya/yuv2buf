package ru.gordinmitya.yuv2buf

import android.graphics.ImageFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        val image = YuvCommon.make(ImageFormat.YUV_420_888, 640, 480, 640)
        Assert.assertEquals(Yuv.detectType(image), ImageFormat.YUV_420_888)

        val converted = Yuv.toBuffer(image, null)
        Assert.assertEquals(converted.type, ImageFormat.YUV_420_888)
        Assert.assertEquals(converted.buffer.capacity(), 640 * 480 * 3 / 2)
    }
}
