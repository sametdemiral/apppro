package com.samet.proapp.uı

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.samet.proapp.R
import com.samet.proapp.database.CSVHelper
import java.io.File
import java.util.concurrent.Executor

class CSVExportActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var exportButton: MaterialButton
    private lateinit var shareButton: MaterialButton
    private lateinit var csvHelper: CSVHelper
    private var selectedFiles: List<File> = listOf()

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val directoryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { exportCSVs(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csv_export)

        setupBiometricAuthentication()
    }

    private fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun setupBiometricAuthentication() {
        if (!canAuthenticateWithBiometrics()) {
            Toast.makeText(this, "Biyometrik kimlik doğrulama kullanılamıyor", Toast.LENGTH_LONG).show()
            initializeActivity()
            return
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Kimlik doğrulama hatası: $errString", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Kimlik doğrulama başarılı!", Toast.LENGTH_SHORT)
                        .show()
                    initializeActivity()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Kimlik doğrulama başarısız",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biyometrik kimlik doğrulama")
            .setSubtitle("Lütfen devam etmek için parmak izinizi kullanın. Samet DEMİRAL harici giriş yasaktır.")
            .setNegativeButtonText("İptal")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun initializeActivity() {
        recyclerView = findViewById(R.id.groupRecyclerView)
        exportButton = findViewById(R.id.exportButton)
        shareButton = findViewById(R.id.shareButton)

        csvHelper = CSVHelper(this)

        setupRecyclerView()
        setupExportButton()
        setupShareButton()
    }

    private fun setupRecyclerView() {
        val allCsvFiles = csvHelper.getAllCSVFiles()
        val adapter = CSVFileAdapter(allCsvFiles) { selectedFiles ->
            this.selectedFiles = selectedFiles
            exportButton.isEnabled = selectedFiles.isNotEmpty()
            shareButton.isEnabled = selectedFiles.isNotEmpty()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupExportButton() {
        exportButton.setOnClickListener {
            directoryPicker.launch(null)
        }
    }

    private fun setupShareButton() {
        shareButton.setOnClickListener {
            if (selectedFiles.isNotEmpty()) {
                shareSelectedCSVFiles(selectedFiles)
            } else {
                Toast.makeText(this, "Lütfen paylaşmak için dosya seçin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportCSVs(directoryUri: Uri) {
        val contentResolver = applicationContext.contentResolver
        var successCount = 0
        var failCount = 0

        selectedFiles.forEach { file ->
            val fileName = file.name
            try {
                Log.d("CSVExport", "Attempting to export file: $fileName")
                val newFileUri = DocumentsContract.createDocument(
                    contentResolver,
                    directoryUri,
                    "text/csv",
                    fileName
                )

                newFileUri?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            val bytesCopied = inputStream.copyTo(outputStream)
                            Log.d("CSVExport", "Copied $bytesCopied bytes for file: $fileName")
                        }
                    }
                    Log.d("CSVExport", "Successfully exported file: $fileName")
                    successCount++
                } ?: run {
                    Log.e("CSVExport", "Failed to create document for file: $fileName")
                    failCount++
                }
            } catch (e: Exception) {
                Log.e("CSVExport", "Error exporting file: $fileName", e)
                failCount++
            }
        }

        runOnUiThread {
            if (failCount == 0) {
                Toast.makeText(this, "$successCount CSV dosyası başarıyla dışa aktarıldı", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "$successCount dosya başarıyla dışa aktarıldı, $failCount dosya aktarılamadı", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareSelectedCSVFiles(files: List<File>) {
        if (files.isNotEmpty()) {
            val uris = files.map { file ->
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    file
                )
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "CSV Dosyalarını Paylaş"))
        } else {
            Toast.makeText(this, "Paylaşılacak CSV dosyası bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }
}

class CSVFileAdapter(
    private val files: List<File>,
    private val onSelectionChanged: (List<File>) -> Unit
) : RecyclerView.Adapter<CSVFileAdapter.ViewHolder>() {

    private val selectedFiles = mutableSetOf<File>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.fileName)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)

        fun bind(file: File) {
            fileName.text = file.name
            checkbox.isChecked = file in selectedFiles
            itemView.setOnClickListener {
                if (file in selectedFiles) {
                    selectedFiles.remove(file)
                } else {
                    selectedFiles.add(file)
                }
                checkbox.isChecked = file in selectedFiles
                onSelectionChanged(selectedFiles.toList())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_csv_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size
}