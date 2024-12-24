// SaveReceiptActivity.kt
package com.samet.proapp.uı

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.view.View
import android.widget.*
import com.samet.proapp.R
import com.samet.proapp.ZoomableImageView
import com.samet.proapp.model.Receipt
import com.samet.proapp.database.ReceiptManager

class SaveReceiptActivity : AppCompatActivity() {
    private lateinit var photoImageView: ZoomableImageView
    private lateinit var captureButton: Button
    private lateinit var saveButton: Button
    private lateinit var nameInput: EditText
    private lateinit var loadingProgressBar: ProgressBar
    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_receipt)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        photoImageView = findViewById(R.id.receiptImageView)
        captureButton = findViewById(R.id.captureButton)
        saveButton = findViewById(R.id.saveButton)
        nameInput = findViewById(R.id.nameInput)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Başlangıçta kaydet butonu ve input alanını gizle
        saveButton.visibility = View.GONE
        nameInput.visibility = View.GONE
    }

    private fun setupListeners() {
        captureButton.setOnClickListener {
            startCamera()
        }

        saveButton.setOnClickListener {
            if (validateAndSave()) {
                showSavingAnimation()
            }
        }
    }

    private fun startCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                photoFile?.also {
                    photoUri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        it
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(intent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "RECEIPT_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            loadingProgressBar.visibility = View.VISIBLE
            Thread {
                val bitmap = processImage()
                runOnUiThread {
                    showImageWithAnimation(bitmap)
                    loadingProgressBar.visibility = View.GONE
                    revealSaveControls()
                }
            }.start()
        }
    }

    private fun processImage(): Bitmap {
        // Görüntüyü yüksek kalitede yükle
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, options)

        // Görüntü boyutunu hesapla
        val photoW: Int = options.outWidth
        val photoH: Int = options.outHeight
        val targetW = photoImageView.width
        val targetH = photoImageView.height

        val scaleFactor = Math.min(
            photoW / targetW,
            photoH / targetH
        )

        options.apply {
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        var bitmap = BitmapFactory.decodeFile(currentPhotoPath, options)

        // Görüntüyü düzelt
        val matrix = Matrix()
        matrix.postRotate(90f)
        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        return bitmap
    }

    private fun showImageWithAnimation(bitmap: Bitmap) {
        photoImageView.setImageBitmap(bitmap)
        photoImageView.alpha = 0f
        photoImageView.scaleX = 0.7f
        photoImageView.scaleY = 0.7f

        photoImageView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun revealSaveControls() {
        nameInput.visibility = View.VISIBLE
        saveButton.visibility = View.VISIBLE

        nameInput.alpha = 0f
        saveButton.alpha = 0f

        nameInput.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        saveButton.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .start()
    }

    private fun validateAndSave(): Boolean {
        val name = nameInput.text.toString()
        if (name.isEmpty()) {
            nameInput.error = "Lütfen bir isim girin"
            return false
        }
        return true
    }

    private fun showSavingAnimation() {
        loadingProgressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false

        Thread {
            // Kaydetme işlemi
            val receipt = Receipt(
                nameInput.text.toString(),
                currentPhotoPath!!,
                Date()
            )
            ReceiptManager.saveReceipt(this, receipt)

            runOnUiThread {
                loadingProgressBar.visibility = View.GONE
                showSuccessAndFinish()
            }
        }.start()
    }

    private fun showSuccessAndFinish() {
        Toast.makeText(this, "Fiş başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

