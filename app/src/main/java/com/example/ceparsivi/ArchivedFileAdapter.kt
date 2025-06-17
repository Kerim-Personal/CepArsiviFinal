package com.example.ceparsivi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ceparsivi.databinding.ItemFileBinding
import com.example.ceparsivi.databinding.ItemHeaderBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_FILE = 1

class ArchivedFileAdapter(
    private val onItemClick: (ArchivedFile) -> Unit,
    private val onItemLongClick: (ArchivedFile) -> Unit
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(ListItemDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HeaderItem -> VIEW_TYPE_HEADER
            is ListItem.FileItem -> VIEW_TYPE_FILE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_FILE -> {
                val binding = ItemFileBinding.inflate(inflater, parent, false)
                FileViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ListItem.FileItem -> (holder as FileViewHolder).bind(item.archivedFile, onItemClick, onItemLongClick)
        }
    }

    class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: ArchivedFile, onClick: (ArchivedFile) -> Unit, onLongClick: (ArchivedFile) -> Unit) {
            binding.textViewFileName.text = file.fileName
            binding.textViewFileDate.text = file.dateAdded
            binding.imageViewFileType.setImageResource(getFileIcon(file))

            itemView.setOnClickListener { onClick(file) }
            itemView.setOnLongClickListener {
                onLongClick(file)
                true
            }
        }

        // --- GÜNCELLENDİ: Yeni ikonlar eklendi ---
        private fun getFileIcon(file: ArchivedFile): Int {
            return when (file.category) {
                "Ofis Dosyaları" -> {
                    when (file.fileName.substringAfterLast('.', "").lowercase()) {
                        "pdf" -> R.drawable.ic_file_pdf
                        "doc", "docx" -> R.drawable.ic_file_doc
                        else -> R.drawable.ic_file_generic
                    }
                }
                "Resim Dosyaları" -> R.drawable.ic_file_image
                "Video Dosyaları" -> R.drawable.ic_file_video     // YENİ
                "Ses Dosyaları" -> R.drawable.ic_file_audio       // YENİ
                "Arşiv Dosyaları" -> R.drawable.ic_file_archive   // YENİ
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