package com.samet.proapp.database

import android.animation.ObjectAnimator
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.ExecutorService

object BarcodeScannerState {
    lateinit var viewFinder: PreviewView
    lateinit var barcodeImageView: ImageView
    lateinit var barcodeTextView: TextInputEditText
    lateinit var flashButton: FloatingActionButton
    lateinit var generateButton: Button
    lateinit var deleteButton: ImageButton
    lateinit var qrSwitch: SwitchMaterial
    lateinit var cameraExecutor: ExecutorService
    lateinit var vibrator: Vibrator
    var flashEnabled = false
    var camera: Camera? = null
    var cameraProvider: ProcessCameraProvider? = null
    var imageAnalyzer: ImageAnalysis? = null
    var preview: Preview? = null
    var originalBrightness: Int = 0
    var isBarcodeOnly = false
    var lastScannedBarcode: String = ""
    var isCameraPaused = false
    lateinit var scannerLine: View
    lateinit var scannerAnimator: ObjectAnimator
    var isScanning = true
}