package ru.gordinmitya.yuv2buf_demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.View
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

    private lateinit var analysisExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageAnalyzer: CompositeConverter
    private lateinit var resultViews: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        analysisExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            preview_view.post { bindCameraX(cameraProvider) }
        }, ContextCompat.getMainExecutor(this))
        val converters = arrayOf(RenderScriptConverter(this))
        resultViews = Array<View>(converters.size) {
            return@Array layoutInflater.inflate(R.layout.item_converted, list_results)
        }
        imageAnalyzer = CompositeConverter(this, Handler(), *converters)
    }

    @SuppressLint("SetTextI18n")
    override fun onAnalyzed(results: List<ConversionResult>) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        results.forEachIndexed { index, result ->
            resultViews[index].let {
                it.image.setImageBitmap(result.image)
                it.text_name.text = result.method
                it.text_time.text = "${result.time}ms"
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
}
