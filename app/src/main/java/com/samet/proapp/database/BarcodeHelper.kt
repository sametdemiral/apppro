package com.samet.proapp.database


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BarcodeHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "BarcodeDatabase.db"
        private const val TABLE_PRODUCTS = "barcode_products"
        private const val COLUMN_ID = "id"
        const val COLUMN_PRODUCT_CODE = "product_code"
        const val COLUMN_PRODUCT_NAME = "product_name"
        const val COLUMN_PRICE = "price"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_PRODUCTS_TABLE = ("CREATE TABLE $TABLE_PRODUCTS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_PRODUCT_CODE TEXT,"
                + "$COLUMN_PRODUCT_NAME TEXT,"
                + "$COLUMN_PRICE REAL"
                + ")")
        db.execSQL(CREATE_PRODUCTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    fun addProduct(productCode: String, productName: String, price: Double): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PRODUCT_CODE, productCode)
            put(COLUMN_PRODUCT_NAME, productName)
            put(COLUMN_PRICE, price)
        }
        val id = db.insert(TABLE_PRODUCTS, null, values)
        db.close()
        return id
    }

    fun updateProductPrice(productCode: String, newPrice: Double): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PRICE, newPrice)
        }
        val rowsAffected = db.update(TABLE_PRODUCTS, values, "$COLUMN_PRODUCT_CODE = ?", arrayOf(productCode))
        db.close()
        return rowsAffected
    }

    fun getAllProducts(): Cursor {
        val db = this.readableDatabase
        return db.query(
            TABLE_PRODUCTS,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun getProductByCode(productCode: String): Cursor {
        val db = this.readableDatabase
        return db.query(
            TABLE_PRODUCTS,
            null,
            "$COLUMN_PRODUCT_CODE = ?",
            arrayOf(productCode),
            null,
            null,
            null
        )
    }
}