package com.samet.proapp.uı

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.samet.proapp.database.Group
import com.samet.proapp.R
import com.samet.proapp.database.CSVHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var csvHelper: CSVHelper
    private lateinit var currentGroup: Group
    private var imageAnalyzer: ImageAnalysis? = null
    private var isDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        csvHelper = CSVHelper(this)
        currentGroup = intent.getParcelableExtra("GROUP") ?: throw IllegalStateException("Group data is required")

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        if (!isDialogShowing) {
                            barcodes.firstOrNull()?.rawValue?.let { value ->
                                isDialogShowing = true
                                runOnUiThread {
                                    showConfirmationDialog(value)
                                }
                            }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Kamera başlatılamadı.", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showConfirmationDialog(barcodeValue: String) {
        val input = EditText(this)
        input.hint = "Başlık girin"

        AlertDialog.Builder(this)
            .setTitle("Barkod Bulundu")
            .setMessage("Bu barkodu kaydetmek istiyor musunuz? Değer: $barcodeValue")
            .setView(input)
            .setPositiveButton("Evet") { _, _ ->
                val title = input.text.toString().takeIf { it.isNotBlank() } ?: "Başlıksız"
                csvHelper.saveBarcode(currentGroup, barcodeValue, title)
                Toast.makeText(this, "Barkod kaydedildi", Toast.LENGTH_SHORT).show()
                isDialogShowing = false
            }
            .setNegativeButton("Hayır") { _, _ ->
                isDialogShowing = false
            }
            .setOnCancelListener {
                isDialogShowing = false
            }
            .show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "İzinler verilmedi.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class BarcodeAnalyzer(private val barcodeListener: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            barcodeListener(barcodes)
                        }
                    }
                    .addOnFailureListener {
                        // Hata durumunda yapılacak işlemler
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }
}