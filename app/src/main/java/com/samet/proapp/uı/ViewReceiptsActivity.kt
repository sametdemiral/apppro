package com.samet.proapp.uı

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samet.proapp.R
import com.samet.proapp.database.Receipt
import com.samet.proapp.database.ReceiptAdapter
import com.samet.proapp.database.ReceiptItemDecoration
import com.samet.proapp.database.ReceiptManager
import com.samet.proapp.database.SaveReceiptActivity
import java.io.File
import java.util.*

class ViewReceiptsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ReceiptAdapter
    private var receiptsList = mutableListOf<Receipt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_receipts)

        // Toolbar'ı ayarla
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Fişlerim"

        initializeViews()
        loadReceipts()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.receiptsRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(ReceiptItemDecoration(16))

        // FAB'a tıklandığında yeni fiş ekleme ekranına git
        findViewById<FloatingActionButton>(R.id.fabAddReceipt).setOnClickListener {
            startActivity(Intent(this, SaveReceiptActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }
    }

    private fun loadReceipts() {
        receiptsList = ReceiptManager.loadReceipts(this).toMutableList()
        adapter = ReceiptAdapter(
            receiptsList,
            onItemClick = { position -> openReceiptDetail(position) },
            onDeleteClick = { position -> deleteReceipt(position) }
        )
        recyclerView.adapter = adapter
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (receiptsList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun openReceiptDetail(position: Int) {
        // Fotoğrafı tam ekran göster
        val receipt = receiptsList[position]
        val intent = Intent(this, FullScreenActivity::class.java)
        intent.putExtra("imagePath", receipt.imagePath)
        intent.putExtra("receiptName", receipt.name)
        intent.putExtra("receiptDate", receipt.date.time)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun deleteReceipt(position: Int) {
        // Onay dialogu göster
        MaterialAlertDialogBuilder(this)
            .setTitle("Fiş Silinecek")
            .setMessage("Bu fişi silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                val receipt = receiptsList[position]
                // Dosyayı sil
                File(receipt.imagePath).delete()
                // Listeden kaldır
                receiptsList.removeAt(position)
                // XML'i güncelle
                ReceiptManager.updateReceipts(this, receiptsList)
                // Adapter'ı güncelle
                adapter.notifyItemRemoved(position)
                updateEmptyView()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_receipts, menu)

        // Arama özelliğini ayarla
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterReceipts(newText)
                return true
            }
        })

        return true
    }

    private fun filterReceipts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            ReceiptManager.loadReceipts(this)
        } else {
            ReceiptManager.loadReceipts(this).filter {
                it.name.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            }
        }
        receiptsList.clear()
        receiptsList.addAll(filteredList)
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }

    override fun onResume() {
        super.onResume()
        loadReceipts() // Yeni fiş eklenmiş olabilir
    }
}