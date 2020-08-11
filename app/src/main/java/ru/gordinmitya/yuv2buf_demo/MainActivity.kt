package ru.gordinmitya.yuv2buf_demo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_converted.view.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), CompositeConverter.Listener {
    val movingAverageSize = 64

    private lateinit var analysisExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageAnalyzer: CompositeConverter
    private lateinit var resultAverages: Array<Pair<MovingAverage, MovingAverage>>
    private lateinit var resultViews: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
            return@Array layoutInflater.inflate(R.layout.item_converted, list_results, false)
        }
        resultViews.forEach { list_results.addView(it) }
        imageAnalyzer = CompositeConverter(this, Handler(), *converters)
    }

    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            preview_view.post { bindCameraX(cameraProvider) }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("SetTextI18n")
    override fun onAnalyzed(size: Pair<Int, Int>, results: List<ConversionResult>) {
        fun Double.format() = String.format("%.2f", this)

        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        text_size.text = "${size.first}x${size.second}"
        results.forEachIndexed { index, result ->
            val average = resultAverages[index]
            average.first.add(result.colorTime)
            average.second.add(result.rotateTime)
            resultViews[index].let {
                it.image.setImageBitmap(result.image)
                it.text_name.text = result.method
                it.text_time.text = "clr ${result.colorTime}ms\n" +
                        "avg clr ${average.first.avg().format()}\n" +
                        "rot ${result.rotateTime}ms\n" +
                        "avg rot ${average.second.avg().format()}\n" +
                        "total ${(average.first.avg() + average.second.avg()).format()}"
            }
        }
    }

    private fun bindCameraX(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(preview_view.display.rotation)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor, imageAnalyzer)
            }
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
        preview.setSurfaceProvider(preview_view.createSurfaceProvider(camera.cameraInfo))
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
            // TODO message
            Toast.makeText(this, "As you wish ¯\\_(ツ)_/¯", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        initCamera()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
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
