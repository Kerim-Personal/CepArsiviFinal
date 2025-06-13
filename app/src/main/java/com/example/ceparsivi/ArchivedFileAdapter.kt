package com.example.ceparsivi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ArchivedFileAdapter(
    private val onItemClick: (ArchivedFile) -> Unit,
    private val onItemLongClick: (ArchivedFile) -> Unit
) : ListAdapter<ArchivedFile, ArchivedFileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileNameTextView: TextView = itemView.findViewById(R.id.textViewFileName)
        private val fileDateTextView: TextView = itemView.findViewById(R.id.textViewFileDate)
        private val fileTypeImageView: ImageView = itemView.findViewById(R.id.imageViewFileType)

        fun bind(
            file: ArchivedFile,
            onItemClick: (ArchivedFile) -> Unit,
            onItemLongClick: (ArchivedFile) -> Unit
        ) {
            fileNameTextView.text = file.fileName
            // Bu satır artık hata vermeyecek çünkü 'savedDate' alanı artık mevcut.
            fileDateTextView.text = file.savedDate

            fileTypeImageView.setImageResource(when {
                file.fileName.endsWith(".pdf", true) -> R.drawable.ic_file_pdf
                file.fileName.endsWith(".jpg", true) ||
                        file.fileName.endsWith(".jpeg", true) ||
                        file.fileName.endsWith(".png", true) -> R.drawable.ic_file_image
                file.fileName.endsWith(".doc", true) ||
                        file.fileName.endsWith(".docx", true) -> R.drawable.ic_file_doc
                else -> R.drawable.ic_file_generic
            })

            itemView.setOnClickListener { onItemClick(file) }
            itemView.setOnLongClickListener {
                onItemLongClick(file)
                true
            }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<ArchivedFile>() {
        override fun areItemsTheSame(oldItem: ArchivedFile, newItem: ArchivedFile): Boolean {
            return oldItem.filePath == newItem.filePath
        }
        override fun areContentsTheSame(oldItem: ArchivedFile, newItem: ArchivedFile): Boolean {
            return oldItem == newItem
        }
    }
}