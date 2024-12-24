package com.samet.proapp.uı

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.samet.proapp.database.DatabaseHelper
import com.samet.proapp.model.Group
import com.samet.proapp.R
import com.samet.proapp.database.CSVHelper
import java.util.Locale

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var textViewGroupName: TextView
    private lateinit var fabOpenCamera: FloatingActionButton
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var recyclerViewBarcodes: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var csvHelper: CSVHelper
    private var currentGroup: Group? = null
    private lateinit var adapter: BarcodeAdapter
    private var originalBarcodes: MutableList<Triple<String, String, String>> = mutableListOf()
    private val REQUEST_CODE_VOICE = 100
    private var isVoiceInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        dbHelper = DatabaseHelper(this)
        csvHelper = CSVHelper(this)




        initializeViews()
        setupCameraButtonlong()
        setupGroupInfo()
        setupCameraButton()
        setupAddProductButton()
        setupRecyclerView()
        setupSearch()
        setupVoiceSearch()
        loadBarcodes()
    }

    private fun initializeViews() {
        textViewGroupName = findViewById(R.id.textViewGroupName)
        fabOpenCamera = findViewById(R.id.fabOpenCamera)
        fabAddProduct = findViewById(R.id.fabAddProduct)
        recyclerViewBarcodes = findViewById(R.id.recyclerViewBarcodes)
        searchEditText = findViewById(R.id.searchEditText)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isVoiceInput) {
                    filterBarcodes(s?.toString() ?: "")
                }
            }
        })
    }

    private fun setupVoiceSearch() {
        val btnVoiceSearch = findViewById<ImageButton>(R.id.btnVoiceSearch)
        btnVoiceSearch.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun filterBarcodes(query: String) {
        if (query.isEmpty()) {
            adapter.updateBarcodes(originalBarcodes)
            return
        }

        // Google'dan gelen metni önce küçük harfe çevirip normalize et
        val searchQuery = normalizeString(query.lowercase(Locale.ROOT))

        val filteredList = originalBarcodes.filter { (_, barcode, title) ->
            // Liste elemanlarını da normalize et
            val normalizedTitle = normalizeString(title.lowercase(Locale.ROOT))
            val normalizedBarcode = barcode.lowercase(Locale.ROOT)

            normalizedBarcode.contains(searchQuery) ||
                    normalizedTitle.contains(searchQuery) ||
                    levenshteinDistance(normalizedTitle, searchQuery) <= 2
        }

        adapter.updateBarcodes(filteredList)
    }

    // Türkçe karakterleri normalize eden yardımcı fonksiyon
    private fun normalizeString(text: String): String {
        return text.replace('ü', 'u')
            .replace('ö', 'o')
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ş', 's')
            .replace('ç', 'c')
    }

    // Levenshtein Distance algoritması - metin benzerliği için
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)

        for (i in 0..s2.length) {
            costs[i] = i
        }

        for (i in 1..s1.length) {
            var lastValue = i
            for (j in 1..s2.length) {
                val newValue = if (s1[i - 1] == s2[j - 1]) {
                    costs[j - 1]
                } else {
                    1 + minOf(
                        costs[j - 1],    // silme
                        costs[j],        // ekleme
                        lastValue        // değiştirme
                    )
                }
                costs[j - 1] = lastValue
                lastValue = newValue
            }
            costs[s2.length] = lastValue
        }

        return costs[s2.length]
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Lütfen aramak istediğiniz metni söyleyin")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE)
        } catch (e: Exception) {
            Toast.makeText(this, "Sesli arama desteklenmiyor", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VOICE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""

            isVoiceInput = true // TextWatcher'ı devre dışı bırak
            searchEditText.setText(spokenText)

            searchEditText.post {
                isVoiceInput = false // TextWatcher'ı tekrar etkinleştir
                filterBarcodes(spokenText) // Manuel olarak filtrelemeyi çağır
            }
        }
    }

    private fun setupGroupInfo() {
        val groupId = intent.getIntExtra("GROUP_ID", -1)
        if (groupId != -1) {
            currentGroup = dbHelper.getAllGroups().find { it.id == groupId }
            currentGroup?.let {
                textViewGroupName.text = it.name
            } ?: run {
                Toast.makeText(this, "Grup bulunamadı", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Geçersiz grup ID'si", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupCameraButton() {
        fabOpenCamera.setOnLongClickListener() {
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("GROUP", currentGroup)
            startActivity(intent)
            true
        }
    }
    private fun setupCameraButtonlong() {
        fabOpenCamera.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)

        }
    }

    private fun setupAddProductButton() {
        fabAddProduct.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("GROUP", currentGroup)
            startActivityForResult(intent, ADD_PRODUCT_REQUEST_CODE)
        }
    }



    private fun setupRecyclerView() {
        recyclerViewBarcodes.layoutManager = LinearLayoutManager(this)
        adapter = BarcodeAdapter(mutableListOf()) { barcodeValue ->
            showBarcodeBottomSheet(barcodeValue)
        }
        recyclerViewBarcodes.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val barcode = adapter.barcodes[position]

                when (direction) {
                    ItemTouchHelper.RIGHT -> showDeleteConfirmationDialog(barcode, position)
                    ItemTouchHelper.LEFT -> {
                        shareBarcode(barcode)
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerViewBarcodes)
    }

    private fun showDeleteConfirmationDialog(barcode: Triple<String, String, String>, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Barkodu Sil")
            .setMessage("Bu barkodu silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ -> deleteBarcode(barcode, position) }
            .setNegativeButton("Hayır") { _, _ -> adapter.notifyItemChanged(position) }
            .show()
    }

    private fun deleteBarcode(barcode: Triple<String, String, String>, position: Int) {
        currentGroup?.let { group ->
            if (csvHelper.deleteBarcodeFromCSV(group, barcode)) {
                adapter.barcodes.removeAt(position)
                originalBarcodes.remove(barcode)
                adapter.notifyItemRemoved(position)
                Snackbar.make(recyclerViewBarcodes, "Barkod silindi", Snackbar.LENGTH_LONG)
                    .setAction("Geri Al") {
                        csvHelper.saveBarcode(group, barcode.second, barcode.third)
                        adapter.barcodes.add(position, barcode)
                        originalBarcodes.add(barcode)
                        adapter.notifyItemInserted(position)
                    }
                    .show()
            } else {
                Toast.makeText(this, "Barkod silinirken bir hata oluştu", Toast.LENGTH_SHORT).show()
                adapter.notifyItemChanged(position)
            }
        }
    }

    private fun shareBarcode(barcode: Triple<String, String, String>) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Barkod: ${barcode.second}\nBaşlık: ${barcode.third}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Barkodu Paylaş"))
    }

    private fun loadBarcodes() {
        currentGroup?.let { group ->
            originalBarcodes.clear()
            csvHelper.getGroupCSVFiles(group).forEach { file ->
                originalBarcodes.addAll(csvHelper.getCSVContent(file))
            }
            adapter.updateBarcodes(originalBarcodes)
        }
    }

    private fun showBarcodeBottomSheet(barcodeValue: String) {
        val bottomSheetFragment = BarcodeBottomSheetFragment.newInstance(barcodeValue)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }



    override fun onResume() {
        super.onResume()
        loadBarcodes()
    }

    private class BarcodeAdapter(
        var barcodes: MutableList<Triple<String, String, String>>,
        private val onLongClickListener: (String) -> Unit
    ) : RecyclerView.Adapter<BarcodeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textViewTimestamp: TextView = view.findViewById(R.id.textViewTimestamp)
            val textViewBarcode: TextView = view.findViewById(R.id.textViewBarcode)
            val textViewTitle: TextView = view.findViewById(R.id.textViewTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_barcode, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (timestamp, barcode, title) = barcodes[position]
            holder.textViewTimestamp.text = timestamp
            holder.textViewBarcode.text = barcode
            holder.textViewTitle.text = title

            holder.itemView.setOnLongClickListener {
                onLongClickListener(barcode)
                true
            }
        }

        override fun getItemCount() = barcodes.size

        @SuppressLint("NotifyDataSetChanged")
        fun updateBarcodes(newBarcodes: List<Triple<String, String, String>>) {
            barcodes.clear()
            barcodes.addAll(newBarcodes)
            notifyDataSetChanged()
        }

    }


    companion object {
        private const val ADD_PRODUCT_REQUEST_CODE = 1001
    }
}