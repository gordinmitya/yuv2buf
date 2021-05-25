package ru.gordinmitya.yuv2buf_demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.renderscript.*
import androidx.camera.core.ImageProxy
import ru.gordinmitya.yuv2buf.Yuv
import java.nio.ByteBuffer


class RenderScriptConverter(context: Context) : ImageConverter {
    val rs = RenderScript.create(context)
    val intrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGB_888(rs))

    private var reuseBuffer: ByteBuffer? = null

    var input: Allocation? = null
    var bytes: ByteArray = ByteArray(0)
    var out: Allocation? = null

    override fun getName(): String = "RenderScript"

    override fun convert(image: ImageProxy): ConversionResult {
        val tik = System.currentTimeMillis()

        val converted = Yuv.toBuffer(image, reuseBuffer)
        reuseBuffer = converted.buffer

        if (input == null
            || input!!.type.x != image.width
            || input!!.type.y != image.height
            || input!!.type.yuv != converted.type.format
            || bytes.size != converted.buffer.capacity()
        ) {
            val yuvFormat = when (converted.type) {
                Yuv.Type.YUV_I420 -> ImageFormat.YUV_420_888
                Yuv.Type.YUV_NV21 -> ImageFormat.NV21
            }
            val yuvType: Type.Builder = Type.Builder(rs, Element.U8(rs))
                .setX(image.width)
                .setY(image.height)
                .setYuvFormat(yuvFormat)
            input = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
            bytes = ByteArray(converted.buffer.capacity())
            val rgbaType: Type.Builder = Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(image.width)
                .setY(image.height)
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        }

        converted.buffer.get(bytes)
        input!!.copyFrom(bytes)
        intrinsic.setInput(input)

        intrinsic.forEach(out)

        val tokColor = System.currentTimeMillis()

        var bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        out!!.copyTo(bitmap)

        if (image.imageInfo.rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.setRotate(image.imageInfo.rotationDegrees.toFloat())
            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                false
            )
        }

        val tokRotate = System.currentTimeMillis()


        // don't forget to call image.close() here
        // but as long as we have many converters
        // we'll do it in CompositeConverter
        // image.close()

        return ConversionResult.Success(getName(), bitmap, tokColor - tik, tokRotate - tokColor)
    }
}
