package com.samet.proapp.database

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.samet.proapp.R
import com.samet.proapp.uı.BarcodeActivity

class UpdateDatabaseActivity : AppCompatActivity() {

    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var editTextProductCode: EditText
    private lateinit var editTextNewPrice: EditText
    private lateinit var buttonUpdate: Button
    private lateinit var barcodeHelper: BarcodeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        editTextProductCode = findViewById(R.id.editTextProductCode)
        editTextNewPrice = findViewById(R.id.editTextNewPrice)
        buttonUpdate = findViewById(R.id.buttonUpdate)
        barcodeHelper = BarcodeHelper(this)

        setupRecyclerView()

        buttonUpdate.setOnClickListener {
            updateProduct()
        }
        buttonUpdate.setOnLongClickListener {
            startActivity(Intent(this, BarcodeActivity::class.java))
            true
        }
    }

    private fun setupRecyclerView() {
        val cursor = barcodeHelper.getAllProducts()
        val adapter = ProductCursorAdapter(cursor)
        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        recyclerViewProducts.adapter = adapter
    }

    private fun updateProduct() {
        val productCode = editTextProductCode.text.toString()
        val newPriceStr = editTextNewPrice.text.toString()

        if (productCode.isNotEmpty() && newPriceStr.isNotEmpty()) {
            val newPrice = newPriceStr.toDoubleOrNull()
            if (newPrice != null) {
                val updatedRows = barcodeHelper.updateProductPrice(productCode, newPrice)
                if (updatedRows > 0) {
                    Toast.makeText(this, "Ürün fiyatı güncellendi", Toast.LENGTH_LONG).show()
                    setupRecyclerView() // Listeyi yenile
                    editTextProductCode.text.clear()
                    editTextNewPrice.text.clear()
                    // Ana ekrana dön
                    finish()
                } else {
                    Toast.makeText(this, "Ürün bulunamadı", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Geçersiz fiyat formatı", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_LONG).show()
        }
    }
}

class ProductCursorAdapter(private var cursor: Cursor) :
    RecyclerView.Adapter<ProductCursorAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProductName: TextView = view.findViewById(R.id.textViewProductName)
        val textViewProductPrice: TextView = view.findViewById(R.id.textViewProductPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_baget, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        cursor.moveToPosition(position)
        val productCode = cursor.getString(cursor.getColumnIndexOrThrow(BarcodeHelper.COLUMN_PRODUCT_CODE))
        val productName = cursor.getString(cursor.getColumnIndexOrThrow(BarcodeHelper.COLUMN_PRODUCT_NAME))
        val price = cursor.getDouble(cursor.getColumnIndexOrThrow(BarcodeHelper.COLUMN_PRICE))

        holder.textViewProductName.text = "$productCode - $productName"
        holder.textViewProductPrice.text = "$price TL/kg"
    }

    override fun getItemCount() = cursor.count

    fun swapCursor(newCursor: Cursor): Cursor {
        if (cursor != newCursor) {
            val oldCursor = cursor
            cursor = newCursor
            notifyDataSetChanged()
            return oldCursor
        }
        return cursor
    }
}