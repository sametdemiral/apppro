package com.samet.proapp.model

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samet.proapp.database.Group
import com.samet.proapp.R
import com.samet.proapp.database.CSVHelper

class AddProductActivity : AppCompatActivity() {

    private lateinit var editTextBarcode: EditText
    private lateinit var editTextTitle: EditText
    private lateinit var buttonSave: Button
    private lateinit var csvHelper: CSVHelper
    private lateinit var currentGroup: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        csvHelper = CSVHelper(this)
        currentGroup = intent.getParcelableExtra("GROUP") ?: throw IllegalStateException("Group data is required")

        initializeViews()
        setupSaveButton()
    }

    private fun initializeViews() {
        editTextBarcode = findViewById(R.id.editTextBarcode)
        editTextTitle = findViewById(R.id.editTextTitle)
        buttonSave = findViewById(R.id.buttonSave)
    }

    private fun setupSaveButton() {
        buttonSave.setOnClickListener {
            val barcode = editTextBarcode.text.toString().trim()
            val title = editTextTitle.text.toString().trim()

            if (barcode.isNotEmpty() && title.isNotEmpty()) {
                csvHelper.saveBarcode(currentGroup, barcode, title)
                Toast.makeText(this, "Ürün başarıyla eklendi", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            }
        }
    }
}