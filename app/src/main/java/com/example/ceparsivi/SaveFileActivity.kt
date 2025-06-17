package com.example.ceparsivi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SaveFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gelen paylaşım intent'ini kontrol et
        if (intent?.action == Intent.ACTION_SEND && intent.type != null) {
            val fileUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (fileUri != null) {
                // Dosya adını alıp kaydetme diyaloğunu göster
                val originalFileName = getFileName(fileUri)
                showSaveDialog(fileUri, originalFileName)
            } else {
                // Uri alınamazsa kullanıcıyı bilgilendir ve aktiviteyi kapat
                Toast.makeText(this, "Dosya alınamadı.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // Geçerli bir intent değilse direkt kapat
            finish()
        }
    }

    private fun showSaveDialog(fileUri: Uri, originalFileName: String) {
        val fileExtension = originalFileName.substringAfterLast('.', "")
        val fileNameWithoutExtension = originalFileName.substringBeforeLast('.', originalFileName)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(50, 40, 50, 0)
        }

        val editTextFileName = EditText(this).apply {
            setText(fileNameWithoutExtension)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val textViewExtension = TextView(this).apply {
            text = if (fileExtension.isNotEmpty()) ".$fileExtension" else ""
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 8 }
        }

        layout.addView(editTextFileName)
        if (fileExtension.isNotEmpty()) {
            layout.addView(textViewExtension)
        }

        AlertDialog.Builder(this)
            .setTitle("Dosyaya Bir İsim Ver")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                val newBaseName = editTextFileName.text.toString().trim()
                if (newBaseName.isNotBlank()) {
                    val newName = if (fileExtension.isNotEmpty()) "$newBaseName.$fileExtension" else newBaseName

                    // --- YENİLİK: Kopyalama işlemini Coroutine ile başlatıyoruz ---
                    // Bu sayede UI (kullanıcı arayüzü) donmayacak.
                    lifecycleScope.launch {
                        val success = copyFileToInternalStorage(fileUri, newName)
                        if (success) {
                            Toast.makeText(this@SaveFileActivity, "'$newName' adıyla kaydedildi!", Toast.LENGTH_LONG).show()
                            // İşlem başarılı, MainActivity'ye sonuç gönder
                            setResult(Activity.RESULT_OK)
                        } else {
                            Toast.makeText(this@SaveFileActivity, "Hata: Dosya kaydedilemedi.", Toast.LENGTH_LONG).show()
                            // İşlem başarısız
                            setResult(Activity.RESULT_CANCELED)
                        }
                        // Her durumda aktiviteyi kapat
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Lütfen geçerli bir isim girin.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                // Diyalog iptal edilirse aktiviteyi kapat
                finish()
            }
            .show()
    }

    // --- YENİLİK 1: Fonksiyonu 'suspend' olarak işaretledik ---
    // Bu, onun uzun sürebilecek bir iş yaptığını ve bir Coroutine içinde
    // çağrılması gerektiğini belirtir.
    private suspend fun copyFileToInternalStorage(uri: Uri, newName: String): Boolean {
        // --- YENİLİK 2: withContext(Dispatchers.IO) bloğu ---
        // Bu blok içindeki kodlar, dosya işlemleri için optimize edilmiş
        // bir arka plan thread'inde (yardımcı çalışanda) çalışır.
        return withContext(Dispatchers.IO) {
            try {
                // Klasör adını "archived_files" olarak değiştirdim, diğer kodlarla tutarlı olsun diye.
                val outputDir = File(filesDir, "archived_files")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                // Aynı isimde dosya varsa üzerine yazmak yerine yeni bir isim bul.
                // Bu fonksiyonu MainActivity'den alıp buraya ekledim.
                val uniqueName = findUniqueFileName(outputDir, newName)
                val outputFile = File(outputDir, uniqueName)

                // contentResolver'dan InputStream al
                val inputStream: InputStream? = contentResolver.openInputStream(uri)

                // --- YENİLİK 3: 'use' blokları ---
                // Bu bloklar, içindeki işlem bitince veya hata olunca
                // inputStream ve outputStream'i OTOMATİK olarak kapatır.
                // Bu, kaynak sızıntılarını %100 önler.
                inputStream?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // inputStream null ise kopyalama yapılamaz, başarısız kabul et
                inputStream != null
            } catch (e: Exception) {
                // Hata oluşursa logla ve false döndür
                e.printStackTrace()
                false
            }
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
            if (cut != null && cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result ?: "isimsiz_dosya"
    }

    // Bu fonksiyon aynı isimde dosya varsa sonuna (1), (2) gibi ekler ekler.
    // MainActivity'den alıp buraya taşıdım ki bu sınıfta da kullanılabilsin.
    private fun findUniqueFileName(directory: File, fileName: String): String {
        var newFile = File(directory, fileName)
        if (!newFile.exists()) {
            return fileName
        }

        val nameWithoutExtension = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        var counter = 1
        while (newFile.exists()) {
            val newName = if (extension.isNotEmpty()) {
                "$nameWithoutExtension ($counter).$extension"
            } else {
                "$fileName ($counter)"
            }
            newFile = File(directory, newName)
            counter++
        }
        return newFile.name
    }
}