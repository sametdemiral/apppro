// ReceiptAdapter.kt
package com.samet.proapp.database

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.samet.proapp.R
import com.samet.proapp.model.Receipt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReceiptAdapter(
    private val receipts: List<Receipt>,
    private val onItemClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.receiptImage)
        val nameText: TextView = view.findViewById(R.id.receiptName)
        val dateText: TextView = view.findViewById(R.id.receiptDate)
        val shareButton: MaterialButton = view.findViewById(R.id.shareButton)
        val deleteButton: MaterialButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val receipt = receipts[position]

        // update photo / fotoğrafı yükle
        Glide.with(holder.imageView.context)
            .load(File(receipt.imagePath))
            .centerCrop()
            .into(holder.imageView)

        // Fiş adını ayarla / add a receipt name
        holder.nameText.text = receipt.name

        // Tarihi formatla ve ayarla
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(receipt.date)

        // Tıklama işlemlerini ayarla
        holder.itemView.setOnClickListener { onItemClick(position) }
        holder.deleteButton.setOnClickListener { onDeleteClick(position) }

        // Paylaş butonu işlemi
        holder.shareButton.setOnClickListener {
            shareReceipt(holder.itemView.context, receipt)
        }
    }

    override fun getItemCount() = receipts.size

    private fun shareReceipt(context: android.content.Context, receipt: Receipt) {
        val imageFile = File(receipt.imagePath)
        if (!imageFile.exists()) return

        val imageUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, receipt.name)
            putExtra(android.content.Intent.EXTRA_TEXT, "Fiş: ${receipt.name}\nTarih: ${
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(receipt.date)
            }")
            type = "image/jpeg"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Fişi Paylaş"))
    }
}

// Öğeler arasına boşluk eklemek için bir ItemDecoration
class ReceiptItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing

        // İlk öğe için üst boşluk ekle
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing
        }
    }
}