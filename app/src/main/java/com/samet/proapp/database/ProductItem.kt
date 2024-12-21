package com.samet.proapp.database

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProductItem(
    val id: Int,
    val name: String,
    val barcode: String,
    val timestamp: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
)