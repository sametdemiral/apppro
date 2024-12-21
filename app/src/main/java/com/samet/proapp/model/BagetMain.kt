package com.samet.proapp.model

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samet.proapp.database.BarcodeHelper
import com.samet.proapp.database.ProductCursorAdapter
import com.samet.proapp.R
import com.samet.proapp.database.UpdateDatabaseActivity
import com.samet.proapp.uı.BarcodeActivity

class BagetMain : AppCompatActivity() {
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var buttonScan: Button
    private lateinit var buttonUpdateDb: Button
    private lateinit var barcodeHelper: BarcodeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.baget_main)

        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        buttonScan = findViewById(R.id.buttonScan)
        buttonUpdateDb = findViewById(R.id.buttonUpdateDb)
        barcodeHelper = BarcodeHelper(this)

        setupRecyclerView()

        buttonScan.setOnClickListener {
            startActivity(Intent(this, BarcodeActivity::class.java))
        }

        buttonUpdateDb.setOnClickListener {
            startActivity(Intent(this, UpdateDatabaseActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        val cursor = barcodeHelper.getAllProducts()
        val adapter = ProductCursorAdapter(cursor)
        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        recyclerViewProducts.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView() // Listeyi güncelle
    }
}