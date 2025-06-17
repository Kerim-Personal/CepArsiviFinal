package com.example.ceparsivi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceparsivi.databinding.ItemFileBinding
import com.example.ceparsivi.databinding.ItemHeaderBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_FILE = 1

class ArchivedFileAdapter(
    private val onItemClick: (ArchivedFile) -> Unit,
    private val onItemLongClick: (ArchivedFile) -> Boolean // Geri dönüş tipi Boolean oldu
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(ListItemDiffCallback()) {

    // --- YENİ: Seçimle ilgili kodlar ---
    private val selectedItems = mutableSetOf<String>()
    var isSelectionMode = false

    fun toggleSelection(filePath: String) {
        if (selectedItems.contains(filePath)) {
            selectedItems.remove(filePath)
        } else {
            selectedItems.add(filePath)
        }
        notifyDataSetChanged() // Görünümü anında güncelle
    }

    fun getSelectedFileCount(): Int = selectedItems.size

    fun getSelectedFiles(allItems: List<ListItem>): List<ArchivedFile> {
        val files = allItems.filterIsInstance<ListItem.FileItem>().map { it.archivedFile }
        return files.filter { selectedItems.contains(it.filePath) }
    }

    fun clearSelections() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }
    // --- Seçimle ilgili kodların sonu ---


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HeaderItem -> VIEW_TYPE_HEADER
            is ListItem.FileItem -> VIEW_TYPE_FILE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FILE -> FileViewHolder(ItemFileBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ListItem.FileItem -> (holder as FileViewHolder).bind(item.archivedFile, onItemClick, onItemLongClick, selectedItems.contains(item.archivedFile.filePath))
        }
    }

    class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            file: ArchivedFile,
            onClick: (ArchivedFile) -> Unit,
            onLongClick: (ArchivedFile) -> Boolean,
            isSelected: Boolean // Seçim durumu eklendi
        ) {
            binding.textViewFileName.text = file.fileName
            binding.textViewFileDate.text = file.dateAdded
            binding.imageViewFileType.setImageResource(getFileIcon(file))

            // Görünümü seçim durumuna göre ayarla
            if (isSelected) {
                binding.root.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener { onClick(file) }
            itemView.setOnLongClickListener { onLongClick(file) }
        }

        private fun getFileIcon(file: ArchivedFile): Int {
            return when (file.category) {
                "Ofis Dosyaları" -> when (file.fileName.substringAfterLast('.', "").lowercase()) {
                    "pdf" -> R.drawable.ic_file_pdf
                    "doc", "docx" -> R.drawable.ic_file_doc
                    else -> R.drawable.ic_file_generic
                }
                "Resim Dosyaları" -> R.drawable.ic_file_image
                "Video Dosyaları" -> R.drawable.ic_file_video
                "Ses Dosyaları" -> R.drawable.ic_file_audio
                "Arşiv Dosyaları" -> R.drawable.ic_file_archive
                else -> R.drawable.ic_file_generic
            }
        }
    }

    class HeaderViewHolder(private val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ListItem.HeaderItem) {
            binding.textViewHeader.text = header.title
        }
    }
}

class ListItemDiffCallback : DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return when {
            oldItem is ListItem.FileItem && newItem is ListItem.FileItem ->
                oldItem.archivedFile.filePath == newItem.archivedFile.filePath
            oldItem is ListItem.HeaderItem && newItem is ListItem.HeaderItem ->
                oldItem.title == newItem.title
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem == newItem
    }
}