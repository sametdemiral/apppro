package com.samet.proapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.samet.proapp.database.DatabaseHelper
import com.samet.proapp.database.Group
import com.samet.proapp.uı.ViewReceiptsActivity
import com.samet.proapp.model.AddGroupActivity
import com.samet.proapp.model.CSVExportActivity
import com.samet.proapp.uı.BarcodeScannerActivity
import com.samet.proapp.uı.GroupDetailActivity

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewGroups: RecyclerView
    private lateinit var fabAddGroup: ExtendedFloatingActionButton
    private lateinit var fabExportCSV: ExtendedFloatingActionButton
    private lateinit var fabCameraButton: ExtendedFloatingActionButton
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: GroupAdapter
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        fabCameraButton = findViewById(R.id.fabCameraButton)


        csvexport()
        initializeViews()
        setupRecyclerView()
        setupAddGroupButton()
        setupCameraButton()
        loadGroups()
        setupSearch()
    }
    private fun setupSearch() {
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query ->
                    if (query.isNotEmpty()) {
                        val filteredGroups = dbHelper.searchGroups(query)
                        adapter.updateGroups(filteredGroups)
                    } else {
                        val allGroups = dbHelper.getAllGroups()
                        adapter.updateGroups(allGroups)
                    }
                }
                return true
            }
        })
    }

    private fun initializeViews() {
        recyclerViewGroups = findViewById(R.id.recyclerViewGroups)
        fabAddGroup = findViewById(R.id.fabAddGroup)
        fabCameraButton = findViewById(R.id.fabCameraButton)
    }

    private fun setupRecyclerView() {
        recyclerViewGroups.layoutManager = LinearLayoutManager(this)
        adapter = GroupAdapter(
            onItemClick = { group ->
                val intent = Intent(this, GroupDetailActivity::class.java)
                intent.putExtra("GROUP_ID", group.id)
                startActivity(intent)
            },
            onItemLongClick = { group ->
                showDeleteConfirmationDialog(group)
            }
        )
        recyclerViewGroups.adapter = adapter
    }

    private fun setupAddGroupButton() {
        fabAddGroup.setOnClickListener {
            val intent = Intent(this, AddGroupActivity::class.java)
            startActivityForResult(intent, ADD_GROUP_REQUEST_CODE)
        }
        fabAddGroup.setOnLongClickListener {
            val intent = Intent(this, ViewReceiptsActivity::class.java)
            startActivity(intent)
            true
        }
    }
    private fun csvexport() {
        fabCameraButton.setOnLongClickListener {
            val intent =Intent(this, CSVExportActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private fun setupCameraButton() {
        fabCameraButton.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun loadGroups() {
        val groups = dbHelper.getAllGroups()
        adapter.updateGroups(groups)
    }

    private fun showDeleteConfirmationDialog(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Grubu Sil")
            .setMessage("\"${group.name}\" grubunu silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteGroup(group)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun deleteGroup(group: Group) {
        if (dbHelper.deleteGroup(group.id)) {
            loadGroups() // Grupları yeniden yükle
            Toast.makeText(this, "\"${group.name}\" grubu silindi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Grup silinirken bir hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_GROUP_REQUEST_CODE && resultCode == RESULT_OK) {
            loadGroups()
        }
    }

    private inner class GroupAdapter(
        private val onItemClick: (Group) -> Unit,
        private val onItemLongClick: (Group) -> Unit
    ) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

        private var groups: List<Group> = listOf()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textViewGroupName: TextView = view.findViewById(R.id.textViewGroupName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val group = groups[position]
            holder.textViewGroupName.text = group.name
            holder.itemView.setOnClickListener { onItemClick(group) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick(group)
                true
            }
        }

        override fun getItemCount() = groups.size

        fun updateGroups(newGroups: List<Group>) {
            groups = newGroups
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val ADD_GROUP_REQUEST_CODE = 1
    }
}