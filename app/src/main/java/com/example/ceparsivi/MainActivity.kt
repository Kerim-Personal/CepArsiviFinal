package com.example.ceparsivi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ceparsivi.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener, ActionMode.Callback, FileDetailsBottomSheet.FileDetailsListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileAdapter: ArchivedFileAdapter
    private var actionMode: ActionMode? = null
    // --- DEĞİŞİKLİK --- Sıralama enum'ını daha anlaşılır yaptım.
    private var currentSortOrder = SortOrder.DATE_DESC

    private var currentViewMode = ViewMode.LIST

    private enum class SortOrder {
        DATE_DESC,
        NAME_ASC,
        NAME_DESC,
        SIZE_ASC,
        SIZE_DESC
    }

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

    // --- YENİLİK --- onResume'da direkt olarak Coroutine başlatıyoruz.
    override fun onResume() {
        super.onResume()
        // Dosya listesini arayüzü kilitlemeden, arka planda güncelle.
        lifecycleScope.launch {
            updateFullList()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch {
            updateFullList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val viewToggleItem = menu.findItem(R.id.action_toggle_view)
        viewToggleItem.setIcon(if (currentViewMode == ViewMode.LIST) R.drawable.ic_view_grid else R.drawable.ic_view_list)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_toggle_view -> {
                toggleViewMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleViewMode() {
        currentViewMode = if (currentViewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        fileAdapter.viewMode = currentViewMode
        setupLayoutManager()
        invalidateOptionsMenu() // Menü ikonunu güncellemek için
    }

    private fun setupLayoutManager() {
        if (currentViewMode == ViewMode.LIST) {
            binding.recyclerViewFiles.layoutManager = LinearLayoutManager(this)
        } else {
            val gridLayoutManager = GridLayoutManager(this, 3)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // Başlıklar tam genişlik, dosya öğeleri 1 birim genişlik kaplar.
                    return if (position < fileAdapter.currentList.size && fileAdapter.getItemViewType(position) == 0) 3 else 1
                }
            }
            binding.recyclerViewFiles.layoutManager = gridLayoutManager
        }
        // Layout değişikliğinden sonra adaptörün haberdar edilmesi önemlidir.
        binding.recyclerViewFiles.adapter = fileAdapter
    }


    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Tarihe Göre (Yeniden Eskiye)",
            "İsme Göre (A-Z)",
            "İsme Göre (Z-A)",
            "Boyuta Göre (Küçükten Büyüğe)",
            "Boyuta Göre (Büyükten Küçüğe)"
        )
        val currentSelection = currentSortOrder.ordinal
        AlertDialog.Builder(this)
            .setTitle("Sıralama Ölçütü")
            .setSingleChoiceItems(sortOptions, currentSelection) { dialog, which ->
                currentSortOrder = SortOrder.entries[which]
                // --- YENİLİK --- Listeyi yine Coroutine ile güncelliyoruz.
                lifecycleScope.launch {
                    updateFullList()
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // --- YENİLİK --- Bu fonksiyon artık 'suspend' yani bir yardımcı fonksiyon.
    private suspend fun updateFullList() {
        // Diskten okuma (ağır iş)
        val allFiles = readFilesFromDisk()

        // Sıralama ve filtreleme (hızlı işler, ama coroutine içinde kalması sorun değil)
        val comparator = when (currentSortOrder) {
            SortOrder.NAME_ASC -> compareBy<ArchivedFile> { it.fileName.lowercase() }
            SortOrder.NAME_DESC -> compareByDescending<ArchivedFile> { it.fileName.lowercase() }
            SortOrder.SIZE_ASC -> compareBy<ArchivedFile> { it.size }
            SortOrder.SIZE_DESC -> compareByDescending<ArchivedFile> { it.size }
            SortOrder.DATE_DESC -> compareByDescending<ArchivedFile> { File(it.filePath).lastModified() }
        }

        val categoryOrderMap = categoryOrder.withIndex().associate { it.value to it.index }
        val sortedFiles = allFiles.sortedWith(
            compareBy<ArchivedFile> { categoryOrderMap[it.category] ?: categoryOrder.size }
                .then(comparator)
        )

        val query = (binding.toolbar.menu.findItem(R.id.action_search)?.actionView as? SearchView)?.query?.toString()
        val filteredFiles = if (query.isNullOrBlank()) {
            sortedFiles
        } else {
            sortedFiles.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        val finalList = buildListWithHeaders(filteredFiles)
        fileAdapter.submitList(finalList)
        updateUI(finalList.isEmpty())
    }

    // --- YENİLİK --- Bu fonksiyon da artık 'suspend' ve işini IO thread'inde yapıyor.
    private suspend fun readFilesFromDisk(): List<ArchivedFile> {
        return withContext(Dispatchers.IO) { // Ağır işi arka plana taşıdık
            val archiveDir = File(filesDir, "arsiv")
            val savedFiles = mutableListOf<ArchivedFile>()
            if (archiveDir.exists() && archiveDir.isDirectory) {
                archiveDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    val lastModifiedDate = Date(file.lastModified())
                    // Tarih formatlama UI ile ilgili olduğu için ana threade bırakılabilir ama burada kalması da sorun değil
                    val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(lastModifiedDate)
                    val category = getFileCategory(file.name)
                    savedFiles.add(ArchivedFile(file.name, file.absolutePath, formattedDate, category, file.length()))
                }
            }
            savedFiles
        }
    }

    private fun buildListWithHeaders(files: List<ArchivedFile>): List<ListItem> {
        val listWithHeaders = mutableListOf<ListItem>()
        if(files.isEmpty()) return listWithHeaders

        var currentCategory = files.first().category
        listWithHeaders.add(ListItem.HeaderItem(currentCategory))

        files.forEach { file ->
            if (file.category != currentCategory) {
                currentCategory = file.category
                listWithHeaders.add(ListItem.HeaderItem(currentCategory))
            }
            listWithHeaders.add(ListItem.FileItem(file))
        }
        return listWithHeaders
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        // --- YENİLİK --- Arama yaparken de arayüz donmasın.
        lifecycleScope.launch {
            updateFullList()
        }
        return true
    }

    // --- (SETUP RECYCLERVIEW, TOGGLESELECTION, ACTIONMODE METODLARI SİZİN KODUNUZLA AYNI, DEĞİŞİKLİK YOK) ---
    // ... Bu metodları olduğu gibi bırakabilirsiniz ...
    private fun setupRecyclerView() {
        fileAdapter = ArchivedFileAdapter(
            onItemClick = { file ->
                if (fileAdapter.isSelectionMode) {
                    toggleSelection(file)
                } else {
                    openFile(file)
                }
            },
            onItemLongClick = { file ->
                if (!fileAdapter.isSelectionMode) {
                    actionMode = startSupportActionMode(this@MainActivity)
                    fileAdapter.isSelectionMode = true
                    toggleSelection(file)
                }
                true
            }
        )
        binding.recyclerViewFiles.adapter = fileAdapter
        setupLayoutManager()
    }

    private fun toggleSelection(file: ArchivedFile) {
        fileAdapter.toggleSelection(file.filePath)
        val count = fileAdapter.getSelectedFileCount()
        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.title = "$count öğe seçildi"
            actionMode?.invalidate()
        }
    }
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.contextual_action_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val selectedCount = fileAdapter.getSelectedFileCount()
        menu.findItem(R.id.action_details)?.isVisible = selectedCount == 1
        menu.findItem(R.id.action_share)?.isVisible = selectedCount > 0
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedFiles = fileAdapter.getSelectedFiles(fileAdapter.currentList)

        return when (item.itemId) {
            R.id.action_delete -> {
                showMultiDeleteConfirmationDialog(selectedFiles)
                true
            }
            R.id.action_share -> {
                shareFiles(selectedFiles)
                mode.finish()
                true
            }
            R.id.action_details -> {
                if (selectedFiles.isNotEmpty()) {
                    showFileDetails(selectedFiles.first())
                }
                mode.finish()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        fileAdapter.clearSelections()
        actionMode = null
    }
    // --- (YUKARIDAKİ METODLARDA DEĞİŞİKLİK YOK) ---


    private fun showMultiDeleteConfirmationDialog(filesToDelete: List<ArchivedFile>) {
        AlertDialog.Builder(this)
            .setTitle("${filesToDelete.size} Dosyayı Sil")
            .setMessage("Seçilen dosyaları kalıcı olarak silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet, Sil") { _, _ ->
                // --- YENİLİK --- Silme işlemini de arka planda yapıyoruz.
                lifecycleScope.launch {
                    deleteFiles(filesToDelete)
                    actionMode?.finish()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showFileDetails(file: ArchivedFile) {
        val bottomSheet = FileDetailsBottomSheet.newInstance(file)
        bottomSheet.listener = this
        bottomSheet.show(supportFragmentManager, "FileDetailsBottomSheet")
    }

    // --- MÜKEMMEL --- Bu fonksiyonu zaten modern ve doğru bir şekilde yazmışsınız!
    private fun shareFiles(files: List<ArchivedFile>) {
        // FileProvider kullanımı zaten doğru. Harika iş!
        if (files.isEmpty()) return

        val uris = ArrayList<Uri>()
        files.forEach { file ->
            val fileToShare = File(file.filePath)
            if (fileToShare.exists()) {
                val authority = "${applicationContext.packageName}.provider"
                uris.add(FileProvider.getUriForFile(this, authority, fileToShare))
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(this, "Paylaşılacak dosya bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            type = "*/*" // Genel MIME türü
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Dosyaları şununla paylaş:"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Bu işlemi yapacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onShareClicked(file: ArchivedFile) {
        shareFiles(listOf(file))
    }

    override fun onDeleteClicked(file: ArchivedFile) {
        showMultiDeleteConfirmationDialog(listOf(file))
    }


    // --- YENİLİK --- Dosya silme de bir I/O (disk) işlemidir. Bunu da 'suspend' yapıyoruz.
    private suspend fun deleteFiles(files: List<ArchivedFile>) {
        var deletedCount = 0
        withContext(Dispatchers.IO) { // Arka plan thread'ine geçtik
            files.forEach {
                val fileToDelete = File(it.filePath)
                if (fileToDelete.exists() && fileToDelete.delete()) {
                    deletedCount++
                }
            }
        }
        // withContext bloğu bittikten sonra otomatik olarak ana thread'e döneriz.
        Toast.makeText(this, "$deletedCount dosya silindi.", Toast.LENGTH_SHORT).show()
        // Silme sonrası listeyi yeniden yükle
        updateFullList()
    }


    // --- (getfilecategory, onquerytextsubmit, updateuı, openfile METODLARI SİZİN KODUNUZLA AYNI, DEĞİŞİKLİK YOK) ---
    // ... Bu metodları olduğu gibi bırakabilirsiniz ...
    private fun getFileCategory(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt" -> "Ofis Dosyaları"
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "Resim Dosyaları"
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "Video Dosyaları"
            "mp3", "wav", "m4a", "aac", "flac", "ogg" -> "Ses Dosyaları"
            "zip", "rar", "7z", "tar", "gz" -> "Arşiv Dosyaları"
            else -> "Diğer Dosyalar"
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewFiles.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerViewFiles.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun openFile(file: ArchivedFile) {
        // Bu fonksiyon da FileProvider kullandığı için zaten doğru ve güvenli.
        val fileToOpen = File(file.filePath)
        if(!fileToOpen.exists()) {
            Toast.makeText(this, "Hata: Dosya bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }
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
    // --- (YUKARIDAKİ METODLARDA DEĞİŞİKLİK YOK) ---

}