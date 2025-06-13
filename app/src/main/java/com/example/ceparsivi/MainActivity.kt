package com.example.ceparsivi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    // XML'deki yeni empty layout'u ekliyoruz
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var recyclerViewFiles: RecyclerView
    private lateinit var fileAdapter: ArchivedFileAdapter
    private lateinit var toolbar: MaterialToolbar
    private val allFiles = mutableListOf<ArchivedFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles)
        // Yeni boş ekran layout'unu bağlıyoruz
        layoutEmpty = findViewById(R.id.layoutEmpty)

        setSupportActionBar(toolbar)
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadArchivedFiles()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        return true
    }

    // --- Diğer fonksiyonlar aynı kalıyor ---
    override fun onQueryTextSubmit(query: String?): Boolean = false
    override fun onQueryTextChange(newText: String?): Boolean {
        filterFiles(newText)
        return true
    }

    private fun setupRecyclerView() {
        fileAdapter = ArchivedFileAdapter(
            onItemClick = { file -> openFile(file) },
            // Uzun basıldığında artık yeni seçenekler menüsünü çağırıyoruz
            onItemLongClick = { file -> showFileOptionsDialog(file) }
        )
        recyclerViewFiles.adapter = fileAdapter
        recyclerViewFiles.layoutManager = LinearLayoutManager(this)
    }

    private fun loadArchivedFiles() {
        val archiveDir = File(filesDir, "arsiv")
        allFiles.clear()
        if (archiveDir.exists() && archiveDir.isDirectory) {
            val savedFiles = archiveDir.listFiles()?.filter { it.isFile }
            if (!savedFiles.isNullOrEmpty()) {
                val archivedFileList = savedFiles.map { file ->
                    val lastModifiedDate = Date(file.lastModified())
                    val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
                        .format(lastModifiedDate)
                    ArchivedFile(file.name, file.absolutePath, formattedDate)
                }.sortedByDescending { File(it.filePath).lastModified() }
                allFiles.addAll(archivedFileList)
            }
        }
        val searchView = toolbar.menu.findItem(R.id.action_search)?.actionView as? SearchView
        filterFiles(searchView?.query?.toString())
        updateUI(allFiles.isEmpty())
    }

    private fun filterFiles(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allFiles
        } else {
            allFiles.filter { it.fileName.contains(query, ignoreCase = true) }
        }
        fileAdapter.submitList(filteredList)
    }

    // updateUI fonksiyonunu yeni boş layout'u gösterecek şekilde güncelliyoruz
    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerViewFiles.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            recyclerViewFiles.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    // --- YENİ FONKSİYONLAR ---

    // Uzun basıldığında seçenekleri (Paylaş, Sil) gösteren diyalog
    private fun showFileOptionsDialog(file: ArchivedFile) {
        val options = arrayOf("Yeniden Paylaş", "Sil")
        AlertDialog.Builder(this)
            .setTitle(file.fileName)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> shareFile(file) // "Yeniden Paylaş" seçildi
                    1 -> showDeleteConfirmationDialog(file) // "Sil" seçildi
                }
            }
            .show()
    }

    // Arşivdeki bir dosyayı paylaşan fonksiyon
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
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Bu işlemi yapacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    // openFile ve deleteFile fonksiyonları aynı kalıyor...
    private fun openFile(file: ArchivedFile) {
        val fileToOpen = File(file.filePath)
        val authority = "${applicationContext.packageName}.provider"
        val fileUri = FileProvider.getUriForFile(this, authority, fileToOpen)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileToOpen.extension) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
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
            loadArchivedFiles()
        } else {
            Toast.makeText(this, "Hata: Dosya silinemedi.", Toast.LENGTH_SHORT).show()
        }
    }
}