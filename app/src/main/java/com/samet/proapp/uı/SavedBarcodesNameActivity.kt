package com.samet.proapp.uı

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.samet.proapp.R
import java.util.Locale



class SavedBarcodesNameActivity : AppCompatActivity() {

    private lateinit var searchEditText: TextInputEditText
    private lateinit var btnVoiceSearch: ImageButton
    private lateinit var recyclerViewBarcodes: RecyclerView
    private lateinit var fabAddBarcode: FloatingActionButton
    private lateinit var adapter: BarcodesAdapter
    private var barcodeList = mutableListOf<SavedBarcodeItem>()
    private var filteredList = mutableListOf<SavedBarcodeItem>()
    private val REQUEST_CODE_VOICE = 100
    private var isVoiceInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_barcodes_name)

        initializeViews()
        setupRecyclerView()
        setupSearch()
        setupVoiceSearch()
        //setupAddButton()
        loadBarcodes()
    }

    private fun initializeViews() {
        searchEditText = findViewById(R.id.searchEditText)
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch)
        recyclerViewBarcodes = findViewById(R.id.recyclerViewBarcodes)
        fabAddBarcode = findViewById(R.id.fabAddBarcode)

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterBarcodes(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = BarcodesAdapter(filteredList) { barcode ->
            showBarcodeDetails(barcode)
        }
        recyclerViewBarcodes.layoutManager = LinearLayoutManager(this)
        recyclerViewBarcodes.adapter = adapter
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
        btnVoiceSearch.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Aramak istediğiniz ürünü söyleyin")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE)
            Toast.makeText(this, "Sizi dinliyorum...", Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Sesli arama kullanılamıyor", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Bir hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    private fun loadBarcodes() {
        try {
            // Global listeden veriyi al
            barcodeList.clear()
            barcodeList.addAll(productList)

            // Filtrelenmiş listeyi güncelle
            filteredList.clear()
            filteredList.addAll(barcodeList)

            // RecyclerView'ı güncelle
            adapter.notifyDataSetChanged()

            // Log
            Log.d("BarcodeList", "Toplam ${barcodeList.size} ürün yüklendi")
        } catch (e: Exception) {
            Log.e("BarcodeList", "Veri yüklenirken hata: ${e.message}")
            Toast.makeText(this, "Ürünler yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }
    private fun filterBarcodes(query: String) {
        val searchQuery = normalizeString(query.lowercase(Locale.ROOT))

        filteredList.clear()
        filteredList.addAll(barcodeList.filter { barcode ->
            val normalizedName = normalizeString(barcode.name.lowercase())
            val normalizedBarcode = barcode.barcode.lowercase()

            normalizedName.contains(searchQuery) ||
                    normalizedBarcode.contains(searchQuery)
        })
        adapter.notifyDataSetChanged()
    }

    private fun normalizeString(text: String): String {
        return text.replace('ü', 'u')
            .replace('ö', 'o')
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ş', 's')
            .replace('ç', 'c')
    }

    private fun showBarcodeDetails(barcode: SavedBarcodeItem) {
        // Barkod detay sayfasına yönlendir
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_VOICE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = result?.get(0) ?: ""

                    isVoiceInput = true
                    searchEditText.setText(spokenText)

                    searchEditText.post {
                        isVoiceInput = false
                        filterBarcodes(spokenText)
                    }
                }
            }
            REQUEST_ADD_BARCODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadBarcodes() // Yeni barkod eklendiğinde listeyi yenile
                }
            }
        }
    }

    private class BarcodesAdapter(
        private val barcodes: List<SavedBarcodeItem>,
        private val onItemClick: (SavedBarcodeItem) -> Unit
    ) : RecyclerView.Adapter<BarcodesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textViewName: TextView = view.findViewById(R.id.textViewName)
            val textViewBarcode: TextView = view.findViewById(R.id.textViewBarcode)
            val textViewTimestamp: TextView = view.findViewById(R.id.textViewTimestamp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_barcode, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val barcode = barcodes[position]
            holder.textViewName.text = barcode.name
            holder.textViewBarcode.text = barcode.barcode
            holder.textViewTimestamp.text = barcode.timestamp

            holder.itemView.setOnClickListener {
                onItemClick(barcode)
            }
        }

        override fun getItemCount() = barcodes.size
    }



    // Companion object'e eklenecek (sınıfın en altına)
    companion object {
        private const val REQUEST_ADD_BARCODE = 1001
        private const val REQUEST_CODE_VOICE = 100
        // productList'i static olarak tutalım ki diğer aktivitelerden erişilebilin
        @JvmStatic
        val productList = mutableListOf<SavedBarcodeItem>()
    }
}