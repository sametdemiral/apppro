package com.samet.proapp.uı

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.samet.proapp.database.BarcodeHelper
import com.samet.proapp.R

class BarcodeActivity : AppCompatActivity() {

    private lateinit var editTextProductName: EditText
    private lateinit var buttonScan: Button
    private lateinit var textViewScannedData: TextView
    private lateinit var barcodeHelper: BarcodeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)

        editTextProductName = findViewById(R.id.editTextProductName)
        buttonScan = findViewById(R.id.buttonScan)
        textViewScannedData = findViewById(R.id.textViewScannedData)
        barcodeHelper = BarcodeHelper(this)

        buttonScan.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("QR Kodu Tarayın")
            integrator.initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "İptal edildi", Toast.LENGTH_LONG).show()
            } else {
                processQRData(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processQRData(qrData: String) {
        val parts = qrData.split("-")
        if (parts.size == 3) {
            val productCode = parts[1]
            val price = parts[2].replace(",", ".").toDoubleOrNull()

            if (price != null) {
                val productName = editTextProductName.text.toString()
                if (productName.isNotEmpty()) {
                    val id = barcodeHelper.addProduct(productCode, productName, price)
                    if (id > -1) {
                        Toast.makeText(this, "Ürün başarıyla eklendi", Toast.LENGTH_LONG).show()
                        textViewScannedData.text = "Eklenen Ürün: $productCode - $productName - $price TL/kg"
                        editTextProductName.text.clear()
                        // Ana ekrana dön
                        finish()
                    } else {
                        Toast.makeText(this, "Ürün eklenirken hata oluştu", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Lütfen ürün adını girin", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Geçersiz fiyat formatı", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Geçersiz QR kod formatı", Toast.LENGTH_LONG).show()
        }
    }
}