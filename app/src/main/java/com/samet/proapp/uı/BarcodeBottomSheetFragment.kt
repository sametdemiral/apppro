package com.samet.proapp.uı

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.samet.proapp.R

class BarcodeBottomSheetFragment : BottomSheetDialogFragment() {

    private var originalBrightness: Int = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (canModifySettings()) {
            setupBrightnessControl()
        } else {
            Toast.makeText(context, "Parlaklık ayarı için izin gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_BARCODE_VALUE = "barcode_value"
        private const val MAX_BRIGHTNESS = 255

        fun newInstance(barcodeValue: String): BarcodeBottomSheetFragment {
            return BarcodeBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BARCODE_VALUE, barcodeValue)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setOnShowListener { dialog ->
                (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
                    BottomSheetBehavior.from(bottomSheet).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        skipCollapsed = true
                    }
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_barcode_bottom_sheet, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        checkWriteSettingsPermission()
    }
    
    private fun setupViews(view: View) {
        val barcodeValue = arguments?.getString(ARG_BARCODE_VALUE) ?: return

        view.findViewById<TextView>(R.id.textViewBarcodeValue).text = barcodeValue
        view.findViewById<TextView>(R.id.buttonCopyBarcode).setOnClickListener {
            copyToClipboard(barcodeValue)
        }
        generateBarcode(barcodeValue, view.findViewById(R.id.imageViewBarcode))
    }

    private fun checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(requireContext())) {
                showPermissionExplanationDialog()
            } else {
                setupBrightnessControl()
            }
        } else {
            setupBrightnessControl()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("İzin Gerekli")
            .setMessage("Ekran parlaklığını ayarlamak için izin gerekiyor. İzin vermek istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                requestPermissionLauncher.launch(intent)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun setupBrightnessControl() {
        val currentBrightness = try {
            Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            MAX_BRIGHTNESS / 2 // Varsayılan değer
        }

        originalBrightness = currentBrightness

        view?.findViewById<SeekBar>(R.id.brightnessSlider)?.apply {
            max = MAX_BRIGHTNESS
            progress = currentBrightness
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        updateBrightness(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun updateBrightness(brightness: Int) {
        if (!canModifySettings()) return
        try {
            Settings.System.putInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = brightness / MAX_BRIGHTNESS.toFloat()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Parlaklık ayarlanamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(barcodeValue: String) {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Barcode", barcodeValue)
        clipboardManager.setPrimaryClip(clip)
    }

    private fun generateBarcode(barcodeValue: String, imageView: ImageView) {
        try {
            val bitMatrix = MultiFormatWriter().encode(barcodeValue, BarcodeFormat.CODE_128, 300, 200)
            val bitmap = BarcodeEncoder().createBitmap(bitMatrix)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Barkod oluşturulamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canModifySettings(): Boolean {
        return isAdded &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Settings.System.canWrite(requireContext()) &&
                activity?.window != null
    }

    override fun onDetach() {
        updateBrightness(originalBrightness)
        super.onDetach()
    }
}