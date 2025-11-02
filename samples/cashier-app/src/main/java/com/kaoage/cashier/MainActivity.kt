package com.kaoage.cashier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.kaoage.sdk.bestshot.BestShotEvaluator
import com.kaoage.sdk.core.BestShotSignal
import com.kaoage.sdk.core.FaceInsightsAnalyzer
import com.kaoage.sdk.core.FaceInsightsResult
import com.kaoage.sdk.core.ModelAssetManager
import com.kaoage.sdk.core.SessionConfig
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var bestShotText: TextView
    private lateinit var resetButton: Button

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isAnalyzing = AtomicBoolean(false)
    private val sessionConfig = SessionConfig(
        minFaceConfidence = 0.5f,
        minFaceSizeRatio = 0.05f,
        cooldownMillis = 2000
    )
    private val bestShotEvaluator = BestShotEvaluator()
    private val faceAnalyzer = FaceInsightsAnalyzer()

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE)
        resultText = findViewById(R.id.resultText)
        bestShotText = findViewById(R.id.bestShotText)
        resetButton = findViewById(R.id.resetBestShot)

        resetButton.setOnClickListener {
            bestShotEvaluator.reset()
            bestShotText.text = ""
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ModelAssetManager.copyModelToCache(applicationContext)
                Log.d(TAG, "MobileNet model ready for inference")
            } catch (ioe: IOException) {
                Log.e(TAG, "Model provisioning failed", ioe)
                withContext(Dispatchers.Main) {
                    resultText.text = getString(R.string.status_model_missing)
                    resetButton.visibility = View.GONE
                }
            }
        }

        ensureCameraPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
                resultText.text = getString(R.string.permission_camera_rationale)
            }
        }
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdown()
        faceAnalyzer.close()
        super.onDestroy()
    }

    private fun ensureCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
            else -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            bindCameraUseCases(provider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(provider: ProcessCameraProvider) {
        provider.unbindAll()

        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .apply { setSurfaceProvider(previewView.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(cameraExecutor, ::analyzeFrame) }

        try {
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind camera use cases", t)
            resultText.text = getString(R.string.status_no_face)
        }
    }

    @ExperimentalGetImage
    private fun analyzeFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        if (!isAnalyzing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val timestamp = System.currentTimeMillis()

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val result: FaceInsightsResult? = faceAnalyzer.analyze(inputImage, sessionConfig, timestamp)
                val bestShot: BestShotSignal? = result?.let { bestShotEvaluator.evaluate(it, sessionConfig) }
                withContext(Dispatchers.Main) {
                    updateUi(result, bestShot)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Frame analysis error", t)
            } finally {
                isAnalyzing.set(false)
                imageProxy.close()
            }
        }
    }

    private fun updateUi(result: FaceInsightsResult?, bestShot: BestShotSignal?) {
        if (result == null) {
            resultText.text = getString(R.string.status_no_face)
            bestShotText.text = ""
            return
        }

        val facePercent = result.faceConfidence * 100f
        val text = getString(
            R.string.status_detection_template,
            facePercent,
            result.ageBracket.name,
            result.gender.name
        )
        resultText.text = text

        bestShotText.text = when {
            bestShot != null -> getString(
                R.string.status_bestshot_triggered,
                bestShot.trigger.name,
                bestShot.qualityScore * 100f
            )
            result.bestShotEligible -> getString(
                R.string.status_bestshot_pending,
                result.bestShotReasons.joinToString()
            )
            else -> ""
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val TAG = "CashierMain"
    }
}
