package com.samet.proapp.model

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samet.proapp.database.DatabaseHelper
import com.samet.proapp.database.Group
import com.samet.proapp.R

class AddGroupActivity : AppCompatActivity() {

    private lateinit var editTextGroupName: EditText
    private lateinit var buttonSaveGroup: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_group)

        dbHelper = DatabaseHelper(this)

        initializeViews()
        setupSaveButton()
    }

    private fun initializeViews() {
        editTextGroupName = findViewById(R.id.editTextGroupName)
        buttonSaveGroup = findViewById(R.id.buttonSaveGroup)
    }

    private fun setupSaveButton() {
        buttonSaveGroup.setOnClickListener {
            val groupName = editTextGroupName.text.toString().trim()
            if (groupName.isNotEmpty()) {
                saveGroup(groupName)
            } else {
                Toast.makeText(this, "Lütfen bir grup adı girin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveGroup(name: String) {
        val newGroup = Group(name = name)
        val id = dbHelper.addGroup(newGroup)
        if (id != -1L) {
            Toast.makeText(this, "Grup başarıyla eklendi", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Grup eklenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
}