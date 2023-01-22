package ru.gordinmitya.yuv2buf_demo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.common.util.concurrent.ListenableFuture
import ru.gordinmitya.yuv2buf_demo.databinding.ActivityMainBinding
import ru.gordinmitya.yuv2buf_demo.databinding.ItemConvertedBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), CompositeConverter.Listener {
    private val movingAverageSize = 64

    private lateinit var analysisExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var compositeConverter: CompositeConverter
    private lateinit var resultAverages: Array<Pair<MovingAverage, MovingAverage>>
    private lateinit var resultViews: Array<ItemConvertedBinding>

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (allPermissionsGranted(this))
            initCamera()
        else
            requestPermissions()

        analysisExecutor = Executors.newSingleThreadExecutor()
        val converters = arrayOf(
            OpenCVConverter(),
            OpenCVRoteterter(),
            RenderScriptConverter(this),
            MNNConverter(this)
        )
        resultAverages = Array(converters.size) {
            MovingAverage(movingAverageSize) to MovingAverage(movingAverageSize)
        }
        resultViews = Array(converters.size) {
            return@Array ItemConvertedBinding.inflate(layoutInflater, binding.listResults, false)
        }
        resultViews.forEach { binding.listResults.addView(it.root) }
        compositeConverter = CompositeConverter(this, Handler(), *converters)
    }

    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            binding.previewView.post { bindCameraX(cameraProvider) }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    override fun onAnalyzed(results: List<ConversionResult>) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        results.forEachIndexed { index, result ->
            when (result) {
                is ConversionResult.Success -> updateSuccess(index, result)
                is ConversionResult.Failed -> updateFailed(index, result)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSuccess(index: Int, result: ConversionResult.Success) {
        fun Double.format() = String.format("%.2f", this)

        val average = resultAverages[index]
        average.first.add(result.colorTime)
        average.second.add(result.rotateTime)
        resultViews[index].let {
            it.image.setImageBitmap(result.image)
            it.textName.text = result.method
            it.textTime.text = "clr ${result.colorTime}ms\n" +
                    "avg clr ${average.first.avg().format()}\n" +
                    "rot ${result.rotateTime}ms\n" +
                    "avg rot ${average.second.avg().format()}\n" +
                    "total ${(average.first.avg() + average.second.avg()).format()}"
        }
    }

    private fun updateFailed(index: Int, result: ConversionResult.Failed) {
        resultViews[index].let {
            it.image.setImageBitmap(null)
            it.textName.text = result.method
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateInfo(image: ImageProxy) = binding.textFrameInfo.post {
        val imageFormat = IMAGE_FORMATS.getOrElse(image.format) { "unknown format" }
        binding.textFrameInfo.text = "${image.width}x${image.height} $imageFormat\n" +
                "rotation ${image.imageInfo.rotationDegrees}\n" +
                "cropRect ${image.cropRect.toShortString()}\n" +
                image.planes.mapIndexed { index, plane ->
                    "plane $index: rowStride=${plane.rowStride} pixelStride=${plane.pixelStride}\n"
                }.joinToString("")
    }

    private fun bindCameraX(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor) { image ->
                    updateInfo(image)
                    compositeConverter.analyze(image)
                }
            }
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (!allPermissionsGranted(this)) {
            Toast.makeText(this, "As you wish ¯\\_(ツ)_/¯", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        initCamera()
    }

    private fun requestPermissions() {
        requestPermissions(
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                showHelp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHelp() {
        val alertDialog = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_help_outline)
            .setMessage(R.string.help_message)
            .create()
        alertDialog.show()

        val textView = alertDialog.findViewById<TextView>(android.R.id.message)!!
        Linkify.addLinks(textView, Linkify.WEB_URLS)
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

private val IMAGE_FORMATS: Map<Int, String> by lazy {
    val map = mutableMapOf(
        ImageFormat.JPEG to "JPEG",
        ImageFormat.YUV_420_888 to "YUV_420_888",
        ImageFormat.RAW_SENSOR to "RAW_SENSOR",
        ImageFormat.YUV_422_888 to "YUV_422_888",
        ImageFormat.YUV_444_888 to "YUV_444_888",
        ImageFormat.FLEX_RGB_888 to "FLEX_RGB_888",
        ImageFormat.FLEX_RGBA_8888 to "FLEX_RGBA_8888",
        ImageFormat.RAW_PRIVATE to "RAW_PRIVATE",
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        map[ImageFormat.HEIC] = "HEIC"
    }
    map
}
