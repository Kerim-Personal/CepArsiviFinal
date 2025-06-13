package com.example.ceparsivi

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // XML'deki elemanlar için değişkenleri burada tanımlıyoruz.
    // Hatanın sebebi muhtemelen bu satırların eksik olmasıydı.
    private lateinit var recyclerViewFiles: RecyclerView
    private lateinit var textViewEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Değişkenleri XML'deki ID'ler ile burada bağlıyoruz.
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles)
        textViewEmpty = findViewById(R.id.textViewEmpty)
    }

    override fun onResume() {
        super.onResume()
        // Şimdilik listeyi boş gösteriyoruz.
        updateUI(isEmpty = true)
    }

    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerViewFiles.visibility = View.GONE
            textViewEmpty.visibility = View.VISIBLE
        } else {
            recyclerViewFiles.visibility = View.VISIBLE
            textViewEmpty.visibility = View.GONE
        }
    }
}