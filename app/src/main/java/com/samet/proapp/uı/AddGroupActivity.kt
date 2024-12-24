package com.samet.proapp.uı

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.samet.proapp.database.DatabaseHelper
import com.samet.proapp.model.Group
import com.samet.proapp.R

class AddGroupActivity(view: View) : AppCompatActivity() {

    private lateinit var editTextGroupName: EditText
    private lateinit var buttonSaveGroup: Button
    private lateinit var dbHelper: DatabaseHelper
    val rootView = findViewById<View>(android.R.id.content)

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
                Snackbar.make(rootView, "Lütfen bir grup adı girin", Snackbar.ANIMATION_MODE_SLIDE).show()
                //Toast.makeText(this, "Lütfen bir grup adı girin", Toast.LENGTH_SHORT).show()
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
            Snackbar.make(rootView,"Hata!",Snackbar.ANIMATION_MODE_SLIDE).show()
            //Toast.makeText(this, "Grup eklenirken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
}