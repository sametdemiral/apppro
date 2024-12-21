// FullScreenActivity.kt
package com.samet.proapp.uı

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.samet.proapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FullScreenActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fulscreen)

        // Tam ekran ayarları
        hideSystemUI()

        // Intent'ten verileri al
        val imagePath = intent.getStringExtra("imagePath") ?: return
        val receiptName = intent.getStringExtra("receiptName") ?: ""
        val receiptDate = Date(intent.getLongExtra("receiptDate", 0))

        photoView = findViewById(R.id.fullscreenPhotoView)

        // Fotoğrafı yükle
        Glide.with(this)
            .load(File(imagePath))
            .into(photoView)

        // Geri butonu
        findViewById<View>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        // Tarih ve isim gösterimi için textview'lar
        val dateText = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            .format(receiptDate)
    }

    private fun hideSystemUI() {
        // Tam ekran modu
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Ekranın sürekli açık kalmasını sağla
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}