package ru.gordinmitya.yuv2buf_demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.ImageProxy
import com.taobao.android.mnn.MNNForwardType
import com.taobao.android.mnn.MNNHelperNative
import com.taobao.android.mnn.MNNImageProcess
import com.taobao.android.mnn.MNNNetInstance
import ru.gordinmitya.yuv2buf.Yuv
import java.nio.ByteBuffer


class MNNConverter(context: Context) : ImageConverter {
    override fun getName(): String = "MNN"

    private var reuseBuffer: ByteBuffer? = null
    private var session: MNNNetInstance.Session
    private var inputTensor: MNNNetInstance.Session.Tensor
    private var outputTensor: MNNNetInstance.Session.Tensor

    init {
        System.loadLibrary("mnncore")
        val file = AssetUtil.copyFileToCache(context, "empty.mnn")
        val net = MNNNetInstance.createFromFile(file.absolutePath)
        val config = MNNNetInstance.Config().also {
            it.forwardType = MNNForwardType.FORWARD_CPU.type
        }
        session = net!!.createSession(config)
        inputTensor = session.getInput(null)
        outputTensor = session.getOutput(null)
    }

    override fun convert(image: ImageProxy): ConversionResult {
        val tik = System.currentTimeMillis()

        val converted = Yuv.toBuffer(image, reuseBuffer)
        reuseBuffer = converted.buffer

        if (inputTensor.dimensions[2] != image.width || inputTensor.dimensions[3] != image.height) {
            inputTensor.reshape(intArrayOf(1, 3, image.width, image.height))
            session.reshape()
        }

        val format = when (converted.type) {
            Yuv.Type.YUV_I420 -> MNNImageProcess.Format.YUV_420
            Yuv.Type.YUV_NV21 -> MNNImageProcess.Format.YUV_NV21
        }

        val config = MNNImageProcess.Config().also {
            it.source = format
            it.dest = MNNImageProcess.Format.RGB
        }

        val trueSize = if (image.imageInfo.rotationDegrees % 180 == 0)
            Size(image.width, image.height)
        else
            Size(image.height, image.width)

        val matrix = Matrix().also {
            it.setScale(1f / image.width, 1f / image.height)
            it.postRotate(1f * image.imageInfo.rotationDegrees, 0.5f, 0.5f)
            it.postScale(1f * trueSize.width, 1f * trueSize.height)
            it.invert(it)
        }
        MNNImageProcess.convertBuffer(
            converted.buffer,
            image.width,
            image.height,
            inputTensor,
            config,
            matrix
        )
        val data = inputTensor.floatData

        val tok = System.currentTimeMillis()

        val pixels = MNNHelperNative.nativeConvertMaskToPixelsMultiChannels(data)
        val bitmap =
            Bitmap.createBitmap(pixels, trueSize.width, trueSize.height, Bitmap.Config.ARGB_8888)

        // don't forget to call image.close() here
        // but as long as we have many converters
        // we'll do it in CompositeConverter
        // image.close()

        return ConversionResult.Success(getName(), bitmap, tok - tik, 0)
    }
}
