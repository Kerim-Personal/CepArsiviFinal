package com.example.ceparsivi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ceparsivi.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileAdapter: ArchivedFileAdapter
    private val allListItems = mutableListOf<ListItem>()

    // --- YENİ: Kategori sıralamasını tanımlıyoruz ---
    private val categoryOrder = listOf(
        "Ofis Dosyaları",
        "Resim Dosyaları",
        "Video Dosyaları",
        "Ses Dosyaları",
        "Arşiv Dosyaları",
        "Diğer Dosyalar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadAndCategorizeFiles()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadAndCategorizeFiles()
    }

    // --- GÜNCELLENDİ: Yeni kategoriler ve uzantılar eklendi ---
    private fun getFileCategory(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            // Ofis Dosyaları
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx" -> "Ofis Dosyaları"
            // Resim Dosyaları
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "Resim Dosyaları"
            // Video Dosyaları
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "Video Dosyaları"
            // Ses Dosyaları
            "mp3", "wav", "m4a", "aac", "flac", "ogg" -> "Ses Dosyaları"
            // Arşiv Dosyaları
            "zip", "rar", "7z", "tar", "gz" -> "Arşiv Dosyaları"
            // Diğer
            else -> "Diğer Dosyalar"
        }
    }

    private fun loadAndCategorizeFiles() {
        val archiveDir = File(filesDir, "arsiv")
        val savedFiles = mutableListOf<ArchivedFile>()

        if (archiveDir.exists() && archiveDir.isDirectory) {
            archiveDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                val lastModifiedDate = Date(file.lastModified())
                val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(lastModifiedDate)
                val category = getFileCategory(file.name)
                savedFiles.add(ArchivedFile(file.name, file.absolutePath, formattedDate, category))
            }
        }

        // --- GÜNCELLENDİ: Dosyaları özel kategori sırasına göre sırala ---
        val categoryOrderMap = categoryOrder.withIndex().associate { it.value to it.index }
        val sortedFiles = savedFiles.sortedWith(
            compareBy(
                { categoryOrderMap[it.category] ?: categoryOrder.size }, // Önce tanımlı kategori sırasına göre
                { -File(it.filePath).lastModified() } // Sonra eklenme tarihine göre
            )
        )

        allListItems.clear()
        var currentCategory = ""
        if (sortedFiles.isNotEmpty()) {
            sortedFiles.forEach { file ->
                if (file.category != currentCategory) {
                    currentCategory = file.category
                    allListItems.add(ListItem.HeaderItem(currentCategory))
                }
                allListItems.add(ListItem.FileItem(file))
            }
        }

        val searchView = binding.toolbar.menu.findItem(R.id.action_search)?.actionView as? SearchView
        filterList(searchView?.query?.toString())
        updateUI(allListItems.isEmpty())
    }

    private fun filterList(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allListItems
        } else {
            val filteredItems = mutableListOf<ListItem>()
            val filesOnly = allListItems.filterIsInstance<ListItem.FileItem>()
                .filter { it.archivedFile.fileName.contains(query, ignoreCase = true) }

            val categoryOrderMap = categoryOrder.withIndex().associate { it.value to it.index }
            val sortedFiles = filesOnly.sortedWith(
                compareBy { categoryOrderMap[it.archivedFile.category] ?: categoryOrder.size }
            )

            var currentCategory = ""
            sortedFiles.forEach { fileItem ->
                if (fileItem.archivedFile.category != currentCategory) {
                    currentCategory = fileItem.archivedFile.category
                    filteredItems.add(ListItem.HeaderItem(currentCategory))
                }
                filteredItems.add(fileItem)
            }
            filteredItems
        }
        fileAdapter.submitList(filteredList)
        updateUI(filteredList.isEmpty())
    }

    // Diğer fonksiyonlar (onCreateOptionsMenu, openFile, shareFile vb.) değişmeden kalır...
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        filterList(newText)
        return true
    }

    private fun setupRecyclerView() {
        fileAdapter = ArchivedFileAdapter(
            onItemClick = { file -> openFile(file) },
            onItemLongClick = { file -> showFileOptionsDialog(file) }
        )
        binding.recyclerViewFiles.adapter = fileAdapter
        binding.recyclerViewFiles.layoutManager = LinearLayoutManager(this)
    }

    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewFiles.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerViewFiles.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showFileOptionsDialog(file: ArchivedFile) {
        val options = arrayOf("Yeniden Paylaş", "Sil")
        AlertDialog.Builder(this)
            .setTitle(file.fileName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareFile(file)
                    1 -> showDeleteConfirmationDialog(file)
                }
            }
            .show()
    }

    private fun shareFile(file: ArchivedFile) {
        val fileToShare = File(file.filePath)
        if (!fileToShare.exists()) {
            Toast.makeText(this, "Hata: Dosya bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        val authority = "${applicationContext.packageName}.provider"
        val fileUri = FileProvider.getUriForFile(this, authority, fileToShare)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileToShare.extension) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Dosyayı şununla paylaş:"))
            Log.d("MainActivity", "${file.fileName} paylaşılıyor.")
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Bu işlemi yapacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: ArchivedFile) {
        val fileToOpen = File(file.filePath)
        val authority = "${applicationContext.packageName}.provider"
        val fileUri = FileProvider.getUriForFile(this, authority, fileToOpen)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileToOpen.extension) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Bu dosya türünü açacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(file: ArchivedFile) {
        AlertDialog.Builder(this)
            .setTitle("Dosyayı Sil")
            .setMessage("'${file.fileName}' dosyasını kalıcı olarak silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet, Sil") { _, _ -> deleteFileFromStorage(file) }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteFileFromStorage(file: ArchivedFile) {
        val fileToDelete = File(file.filePath)
        if (fileToDelete.exists() && fileToDelete.delete()) {
            Toast.makeText(this, "'${file.fileName}' silindi.", Toast.LENGTH_SHORT).show()
            loadAndCategorizeFiles()
        } else {
            Toast.makeText(this, "Hata: Dosya silinemedi.", Toast.LENGTH_SHORT).show()
        }
    }
}