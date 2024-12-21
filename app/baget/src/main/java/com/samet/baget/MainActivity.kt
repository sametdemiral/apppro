// MainActivity.kt
package com.samet.baget

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        val barkodEditText = findViewById<EditText>(R.id.barkodEditText)
        val adEditText = findViewById<EditText>(R.id.adEditText)
        val fiyatEditText = findViewById<EditText>(R.id.fiyatEditText)
        val ekleButton = findViewById<Button>(R.id.ekleButton)
        val araButton = findViewById<Button>(R.id.araButton)
        val sonucTextView = findViewById<TextView>(R.id.sonucTextView)

        ekleButton.setOnClickListener {
            val barkod = barkodEditText.text.toString()
            val ad = adEditText.text.toString()
            val fiyatStr = fiyatEditText.text.toString()

            // Fiyatı güvenli bir şekilde Float'a dönüştürme
            val fiyat = try {
                fiyatStr.toFloat()
            } catch (e: NumberFormatException) {
                sonucTextView.text = "Geçersiz fiyat girişi. Lütfen sayı giriniz."
                return@setOnClickListener
            }

            if (barkod.isBlank() || ad.isBlank()) {
                sonucTextView.text = "Barkod ve ürün adı boş olamaz."
                return@setOnClickListener
            }

            val id = dbHelper.urunEkle(barkod, ad, fiyat)
            if (id != -1L) {
                sonucTextView.text = "Ürün eklendi: $ad"
                // Giriş alanlarını temizleme
                barkodEditText.text.clear()
                adEditText.text.clear()
                fiyatEditText.text.clear()
            } else {
                sonucTextView.text = "Ürün eklenirken hata oluştu"
            }
        }

        araButton.setOnClickListener {
            val barkod = barkodEditText.text.toString()
            if (barkod.isBlank()) {
                sonucTextView.text = "Lütfen bir barkod girin."
                return@setOnClickListener
            }

            val urun = dbHelper.urunBul(barkod)

            if (urun != null) {
                sonucTextView.text = "Bulunan ürün: ${urun.ad}, Fiyat: ${urun.kgFiyati} TL/kg"
            } else {
                sonucTextView.text = "Ürün bulunamadı"
            }
        }
    }
}