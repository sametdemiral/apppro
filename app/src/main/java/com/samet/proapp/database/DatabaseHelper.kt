package com.samet.proapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "GroupsDatabase.db"
        private const val TABLE_GROUPS = "groups"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_GROUPS (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_NAME TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        onCreate(db)
    }

    fun addGroup(group: Group): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, group.name)
        }
        return db.insert(TABLE_GROUPS, null, values)
    }

    fun getAllGroups(): List<Group> {
        val groupList = mutableListOf<Group>()
        val selectQuery = "SELECT * FROM $TABLE_GROUPS"
        val db = this.readableDatabase
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                    groupList.add(Group(id, name))
                } while (cursor.moveToNext())
            }
        }
        return groupList
    }

    fun deleteGroup(groupId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_GROUPS, "$KEY_ID = ?", arrayOf(groupId.toString()))
        return result > 0
    }
    fun updateGroup(group: Group): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, group.name)
        }
        return db.update(TABLE_GROUPS, values, "$KEY_ID = ?", arrayOf(group.id.toString()))
    }

    fun searchGroups(query: String): List<Group> {
        val groupList = mutableListOf<Group>()
        val selectQuery = "SELECT * FROM $TABLE_GROUPS WHERE $KEY_NAME LIKE ?"
        val db = this.readableDatabase
        db.rawQuery(selectQuery, arrayOf("%$query%")).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                    groupList.add(Group(id, name))
                } while (cursor.moveToNext())
            }
        }
        return groupList
    }
}