package com.example.ceparsivi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceparsivi.databinding.ItemFileBinding
import com.example.ceparsivi.databinding.ItemFileGridBinding
import com.example.ceparsivi.databinding.ItemHeaderBinding

// Görünüm Tipleri
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_FILE_LIST = 1
private const val VIEW_TYPE_FILE_GRID = 2

// Görünüm Modu
enum class ViewMode {
    LIST, GRID
}

class ArchivedFileAdapter(
    private val onItemClick: (ArchivedFile) -> Unit,
    private val onItemLongClick: (ArchivedFile) -> Boolean
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(ListItemDiffCallback()) { // Hatanın olduğu satır

    var viewMode: ViewMode = ViewMode.LIST
    private val selectedItems = mutableSetOf<String>()
    var isSelectionMode = false

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HeaderItem -> VIEW_TYPE_HEADER
            is ListItem.FileItem -> if (viewMode == ViewMode.LIST) VIEW_TYPE_FILE_LIST else VIEW_TYPE_FILE_GRID
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FILE_LIST -> ListFileViewHolder(ItemFileBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FILE_GRID -> GridFileViewHolder(ItemFileGridBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isSelected = when (val item = getItem(position)) {
            is ListItem.FileItem -> selectedItems.contains(item.archivedFile.filePath)
            else -> false
        }

        when (holder) {
            is HeaderViewHolder -> holder.bind((getItem(position) as ListItem.HeaderItem))
            is ListFileViewHolder -> holder.bind((getItem(position) as ListItem.FileItem).archivedFile, onItemClick, onItemLongClick, isSelected)
            is GridFileViewHolder -> holder.bind((getItem(position) as ListItem.FileItem).archivedFile, onItemClick, onItemLongClick, isSelected)
        }
    }

    class ListFileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: ArchivedFile, onClick: (ArchivedFile) -> Unit, onLongClick: (ArchivedFile) -> Boolean, isSelected: Boolean) {
            binding.textViewFileName.text = file.fileName
            binding.textViewFileDate.text = file.dateAdded
            binding.imageViewFileType.setImageResource(getFileIcon(file))
            binding.root.setBackgroundColor(if (isSelected) ContextCompat.getColor(itemView.context, android.R.color.darker_gray) else Color.TRANSPARENT)
            itemView.setOnClickListener { onClick(file) }
            itemView.setOnLongClickListener { onLongClick(file) }
        }
    }

    class GridFileViewHolder(private val binding: ItemFileGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: ArchivedFile, onClick: (ArchivedFile) -> Unit, onLongClick: (ArchivedFile) -> Boolean, isSelected: Boolean) {
            binding.textViewFileNameGrid.text = file.fileName
            binding.imageViewFileTypeGrid.setImageResource(getFileIcon(file))
            binding.root.strokeWidth = if (isSelected) 8 else 0
            binding.root.strokeColor = if (isSelected) ContextCompat.getColor(itemView.context, R.color.black) else Color.TRANSPARENT
            itemView.setOnClickListener { onClick(file) }
            itemView.setOnLongClickListener { onLongClick(file) }
        }
    }

    companion object {
        fun getFileIcon(file: ArchivedFile): Int {
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

    fun toggleSelection(filePath: String) {
        if (selectedItems.contains(filePath)) selectedItems.remove(filePath) else selectedItems.add(filePath)
        notifyDataSetChanged()
    }

    fun getSelectedFileCount(): Int = selectedItems.size

    fun getSelectedFiles(allItems: List<ListItem>): List<ArchivedFile> {
        return allItems.filterIsInstance<ListItem.FileItem>()
            .map { it.archivedFile }
            .filter { selectedItems.contains(it.filePath) }
    }

    fun clearSelections() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    class HeaderViewHolder(private val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ListItem.HeaderItem) {
            binding.textViewHeader.text = header.title
        }
    }
}

// --- EKSİK OLAN VE HATAYA SEBEP OLAN SINIF BURASI ---
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