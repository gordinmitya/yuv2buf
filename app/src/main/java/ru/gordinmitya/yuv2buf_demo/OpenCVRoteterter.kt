package ru.gordinmitya.yuv2buf_demo

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import ru.gordinmitya.yuv2buf.Yuv
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


// Here I try to rotate image in YUV format because it's 2x smaller than RGB
class OpenCVRoteterter() : ImageConverter {
    override fun getName(): String = "OpenCV (YUV rotate)"

    private var reuseBuffer: ByteBuffer? = null

    override fun convert(image: ImageProxy): ConversionResult {
        val tik = System.currentTimeMillis()

        val converted = Yuv.toBuffer(image, reuseBuffer)
        reuseBuffer = converted.buffer

        val format = when (converted.type) {
            Yuv.Type.YUV_I420 -> Imgproc.COLOR_YUV2RGB_I420
            Yuv.Type.YUV_NV21 -> Imgproc.COLOR_YUV2RGB_NV21
        }

        val ySize = image.width * image.height
        val cSize = ySize / 4

        val yBuf = converted.buffer
        val uvBuf = converted.buffer.clipBuffer(ySize, cSize + cSize)

        val yMat = Mat(image.height, image.width, CvType.CV_8UC1, yBuf)
        val uvMat = Mat(image.height / 2, image.width / 2, CvType.CV_8UC2, uvBuf)

        val tRotateStart = System.currentTimeMillis()
        if (image.imageInfo.rotationDegrees != 0) {
            Core.rotate(yMat, yMat, image.imageInfo.rotationDegrees / 90 - 1)
            Core.rotate(uvMat, uvMat, image.imageInfo.rotationDegrees / 90 - 1)
        }
        val tRotateEnd = System.currentTimeMillis()

        val rgbMat = Mat(image.height, image.width, CvType.CV_8UC3)

        Imgproc.cvtColorTwoPlane(yMat, uvMat, rgbMat, format)

        val tok = System.currentTimeMillis()

        val bitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbMat, bitmap)

        // don't forget to call image.close() here
        // but as long as we have many converters
        // we'll do it in CompositeConverter
        // image.close()

        val colorTime = tRotateStart - tik + tok - tRotateEnd

        return ConversionResult(getName(), bitmap, colorTime, tRotateEnd - tRotateStart)
    }

    private fun ByteBuffer.clipBuffer(
        start: Int,
        size: Int
    ): ByteBuffer? {
        val duplicate = this.duplicate()
        duplicate.position(start)
        duplicate.limit(start + size)
        return duplicate.slice()
    }
}