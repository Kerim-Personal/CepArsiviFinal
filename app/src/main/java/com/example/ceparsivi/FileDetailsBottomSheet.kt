package com.example.ceparsivi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.ceparsivi.databinding.BottomSheetFileDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.CharacterIterator
import java.text.StringCharacterIterator

class FileDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFileDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var archivedFile: ArchivedFile

    // MainActivity'nin dinleyeceği arayüz
    interface FileDetailsListener {
        fun onShareClicked(file: ArchivedFile)
        fun onDeleteClicked(file: ArchivedFile)
    }

    var listener: FileDetailsListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            archivedFile = it.getParcelable("file")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewFileNameDetails.text = archivedFile.fileName
        binding.textViewCategory.text = "Kategori: ${archivedFile.category}"
        binding.textViewDate.text = "Tarih: ${archivedFile.dateAdded}"
        binding.textViewSize.text = "Boyut: ${formatBytes(archivedFile.size)}"

        binding.buttonShare.setOnClickListener {
            listener?.onShareClicked(archivedFile)
            dismiss()
        }

        binding.buttonDelete.setOnClickListener {
            listener?.onDeleteClicked(archivedFile)
            dismiss()
        }
    }

    // Dosya boyutunu okunabilir bir formata çevirir (KB, MB, GB)
    private fun formatBytes(bytes: Long): String {
        var a = bytes
        if (-1000 < a && a < 1000) {
            return "$a B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (a <= -999950 || a >= 999950) {
            a /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", a / 1000.0, ci.current())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(file: ArchivedFile): FileDetailsBottomSheet {
            val args = Bundle().apply {
                putParcelable("file", file)
            }
            return FileDetailsBottomSheet().apply {
                arguments = args
            }
        }
    }
}