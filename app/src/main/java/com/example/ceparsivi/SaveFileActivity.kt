package com.example.ceparsivi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class SaveFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type != null) {

            // --- DEĞİŞİKLİK BURADA ---
            // Cihazın Android sürümünü kontrol ediyoruz
            val fileUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 (API 33) ve üstü için yeni ve güvenli metot
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                // Eski versiyonlar için déprecated (eski) metot.
                // @Suppress ile uyarıyı gizliyoruz çünkü bu uyumluluk için zorunlu.
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            // --- DEĞİŞİKLİK SONA ERDİ ---

            if (fileUri != null) {
                val originalFileName = getFileName(fileUri)
                showSaveDialog(fileUri, originalFileName)
            } else {
                Toast.makeText(this, "Dosya alınamadı.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun showSaveDialog(fileUri: Uri, originalFileName: String) {
        val editText = EditText(this)
        editText.setText(originalFileName)

        AlertDialog.Builder(this)
            .setTitle("Dosyaya Bir İsim Ver")
            .setView(editText)
            .setPositiveButton("Kaydet") { dialog, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    copyFileToInternalStorage(fileUri, newName)
                } else {
                    Toast.makeText(this, "Lütfen geçerli bir isim girin.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.cancel()
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun copyFileToInternalStorage(uri: Uri, newName: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputDir = File(filesDir, "arsiv")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val outputFile = File(outputDir, newName)
            val outputStream = FileOutputStream(outputFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("SaveFileActivity", "Dosya başarıyla kaydedildi: ${outputFile.absolutePath}")
            Toast.makeText(this, "'$newName' adıyla kaydedildi!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("SaveFileActivity", "Dosya kopyalanamadı", e)
            Toast.makeText(this, "Hata: Dosya kaydedilemedi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if(columnIndex >= 0) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result.substring(cut + 1)
                }
            }
        }
        return result ?: "isimsiz_dosya"
    }
}