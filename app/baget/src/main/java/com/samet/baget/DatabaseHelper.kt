package com.samet.baget

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "UrunlerDB"
        private const val TABLE_URUNLER = "urunler"
        private const val KEY_BARKOD = "barkod"
        private const val KEY_AD = "ad"
        private const val KEY_FIYAT = "fiyat"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_URUNLER_TABLE = ("CREATE TABLE $TABLE_URUNLER("
                + "$KEY_BARKOD TEXT PRIMARY KEY,"
                + "$KEY_AD TEXT,"
                + "$KEY_FIYAT REAL" + ")")
        db.execSQL(CREATE_URUNLER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_URUNLER")
        onCreate(db)
    }

    fun urunEkle(barkod: String, ad: String, fiyat: Float): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_BARKOD, barkod)
        values.put(KEY_AD, ad)
        values.put(KEY_FIYAT, fiyat)
        val id = db.insert(TABLE_URUNLER, null, values)
        db.close()
        return id
    }

    fun urunBul(barkod: String): Urun? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_URUNLER, arrayOf(KEY_BARKOD, KEY_AD, KEY_FIYAT), "$KEY_BARKOD=?",
            arrayOf(barkod), null, null, null, null)
        var urun: Urun? = null
        if (cursor != null && cursor.moveToFirst()) {
            urun = Urun(cursor.getString(0), cursor.getString(1), cursor.getFloat(2))
            cursor.close()
        }
        db.close()
        return urun
    }
}