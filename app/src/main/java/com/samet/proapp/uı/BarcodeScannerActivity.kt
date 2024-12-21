package com.samet.proapp.uı

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.samet.proapp.database.ProductItem
import com.samet.proapp.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var barcodeImageView: ImageView
    private lateinit var barcodeTextView: TextInputEditText
    private lateinit var flashButton: FloatingActionButton
    private lateinit var generateButton: Button
    private lateinit var deleteButton: ImageButton
    private lateinit var qrSwitch: SwitchMaterial
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var vibrator: Vibrator
    private var flashEnabled = false
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var originalBrightness: Int = 0
    private var isBarcodeOnly = false
    private var lastScannedBarcode: String = ""
    private var isCameraPaused = false
    private lateinit var scannerLine: View
    private lateinit var scannerAnimator: ObjectAnimator
    private var isScanning = true
    private val productList = mutableListOf<ProductItem>()
    private var currentId = 0




    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)



        setupFullScreenAndTransparentStatusBar()
        initializeViews()
        setupVibrator()
        setupCamera()
        setupListeners()
        setupScannerAnimation()
        val flashButton = findViewById<ImageButton>(R.id.flashButton)
        val barcodeEditText = findViewById<EditText>(R.id.barcodeTextView)

        flashButton.setOnLongClickListener {
            val barcode = barcodeEditText.text.toString()
            if (barcode.isNotEmpty()) {
                showNameInputDialog(barcode)
            } else {
                Toast.makeText(this, "Lütfen önce barkod giriniz!", Toast.LENGTH_SHORT).show()
            }
            true
        }
        //updateScannerVisibility()
    }
    private fun showNameInputDialog(barcode: String) {
        val nameEditText = EditText(this).apply {
            hint = "Ürün adını giriniz"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(50, 50, 50, 50)
        }

        AlertDialog.Builder(this)
            .setTitle("Ürün Adı")
            .setView(nameEditText)
            .setPositiveButton("Ekle") { dialog, _ ->
                val name = nameEditText.text.toString()
                if (name.isNotEmpty()) {
                    val newProduct = ProductItem(
                        id = currentId++,
                        name = name,
                        barcode = barcode
                    )
                    addProductToDatabase(newProduct)
                } else {
                    Toast.makeText(this, "Ürün adı boş olamaz!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    private fun addProductToDatabase(product: ProductItem) {
        try {
            productList.add(product)
            Toast.makeText(this, "Ürün kaydedildi: ${product.name}", Toast.LENGTH_SHORT).show()

            // Eklenen ürünü logla
            Log.d("ProductAdd", """
            Yeni ürün eklendi:
            ID: ${product.id}
            İsim: ${product.name}
            Barkod: ${product.barcode}
            Tarih: ${product.timestamp}
        """.trimIndent())

        } catch (e: Exception) {
            Toast.makeText(this, "Ürün eklenemedi!", Toast.LENGTH_SHORT).show()
            Log.e("ProductAdd", "Hata: ${e.message}")
        }
    }



    private fun setupFullScreenAndTransparentStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupVibrator() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        originalBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupListeners() {
        flashButton.setOnClickListener { toggleFlash() }
        generateButton.setOnClickListener {
            if (generateBarcode()) {
                increaseBrightness()
                vibrateDevice()
                pauseCamera()
                turnOffFlash()
            }
        }
        barcodeImageView.setOnClickListener(View.OnClickListener
            {shareContent(barcodeTextView.text.toString())}
        )
        barcodeImageView.setOnLongClickListener {
            shareBarcode()
            true  // Long click'in tüketildiğini belirtmek için true dönüyoruz
        }
        deleteButton.setOnClickListener {
            clearBarcode()
            resetBrightness()
            resumeCamera()
        }
        deleteButton.setOnLongClickListener {
            lastScannedBarcode = ""
            vibrateDevice()
            Toast.makeText(this, "Son taranan barkod sıfırlandı", Toast.LENGTH_SHORT).show()
            true
        }
        generateButton.setOnLongClickListener {
            val intent = Intent(this, SavedBarcodesNameActivity::class.java)
            startActivity(intent)
            true
        }
        qrSwitch.setOnCheckedChangeListener { _, isChecked ->
            isBarcodeOnly = isChecked
            startCamera()
        }
    }
    private fun shareContent(content: String, subject: String = "Uygulama Paylaşımı") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(shareIntent, "Gönder"))

    }
    private fun shareBarcode() {
        val bitmap = getBitmapFromImageView(barcodeImageView)
        if (bitmap != null) {
            val uri = saveImageToCache(bitmap)
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Barkodu Paylaş"))
            }
        }
    }

    private fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
        imageView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri? {
        val imagesFolder = File(cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "shared_barcode.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return uri
    }






    private fun setupScannerAnimation() {
        scannerLine = findViewById(R.id.scannerLine)
        val viewFinder = findViewById<PreviewView>(R.id.viewFinder)

        viewFinder.post {
            scannerAnimator = ObjectAnimator.ofFloat(scannerLine, "translationY",
                0f, viewFinder.height.toFloat() - scannerLine.height)
            scannerAnimator.apply {
                duration = 1500 // 1.5 saniye
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            updateScannerVisibility()
        }
    }

    private fun updateScannerVisibility() {
        if (isScanning) {
            scannerLine.visibility = View.VISIBLE
            scannerAnimator.start()
        } else {
            scannerLine.visibility = View.INVISIBLE
            scannerAnimator.pause()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        barcodes.firstOrNull()?.let { barcode ->
                            if (!isBarcodeOnly || barcode.format != Barcode.FORMAT_QR_CODE) {
                                val barcodeValue = barcode.rawValue ?: ""
                                if (barcodeValue != lastScannedBarcode) {
                                    lastScannedBarcode = barcodeValue
                                    runOnUiThread {
                                        barcodeTextView.setText(barcodeValue)
                                        vibrateDevice()
                                    }
                                }
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateBarcode(): Boolean {
        val content = barcodeTextView.text.toString()
        if (content.isNotEmpty()) {
            try {
                val multiFormatWriter = MultiFormatWriter()
                val bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.CODE_128, 650, 200)
                val barcodeEncoder = BarcodeEncoder()
                val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
                barcodeImageView.setImageBitmap(bitmap)
                isScanning = false
                updateScannerVisibility()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Barkod oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Önce bir barkod tarayın veya metin girin", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun clearBarcode() {
        barcodeImageView.setImageDrawable(null)
        barcodeTextView.setText("")
        isScanning = true
        updateScannerVisibility()
    }

    private fun increaseBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1f
        window.attributes = layoutParams
    }

    private fun initializeViews(): Boolean {
        try {
            viewFinder = findViewById(R.id.viewFinder)
            barcodeImageView = findViewById(R.id.barcodeImageView)
            barcodeTextView = findViewById(R.id.barcodeTextView)
            flashButton = findViewById(R.id.flashButton)
            generateButton = findViewById(R.id.generateButton)
            deleteButton = findViewById(R.id.deleteButton)
            qrSwitch = findViewById(R.id.qrSwitch)
            scannerLine = findViewById(R.id.scannerLine)

            return viewFinder != null && barcodeImageView != null && barcodeTextView != null &&
                    flashButton != null && generateButton != null && deleteButton != null && qrSwitch != null
        } catch (e: Exception) {
            Log.e("BarcodeScannerActivity", "Error initializing views", e)
            return false
        }
    }

    private fun resetBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = originalBrightness / 255f
        window.attributes = layoutParams
    }

    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                flashEnabled = !flashEnabled
                cam.cameraControl.enableTorch(flashEnabled)
            } else {
                Toast.makeText(this, "Bu cihazda flaş birimi yok", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun turnOffFlash() {
        camera?.cameraControl?.enableTorch(false)
        flashEnabled = false
    }

    private fun pauseCamera() {
        if (!isCameraPaused) {
            cameraProvider?.unbind(preview)
            cameraProvider?.unbind(imageAnalyzer)
            viewFinder.visibility = View.INVISIBLE
            isCameraPaused = true
        }
    }

    private fun resumeCamera() {
        if (isCameraPaused) {
            viewFinder.visibility = View.VISIBLE
            startCamera()
            isCameraPaused = false
        }
    }

    private fun vibrateDevice() {
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Kamera izni verilmedi.", Toast.LENGTH_SHORT).show()
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
                        barcodeListener(barcodes)
                    }
                    .addOnFailureListener {
                        // TODO: Handle any errors (e.g., log or show a message)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}