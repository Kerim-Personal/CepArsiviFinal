package com.example.ceparsivi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SaveFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gelen Intent'in bir "SEND" eylemi olup olmadığını ve veri tipini kontrol et
        if (intent?.action == Intent.ACTION_SEND && intent.type != null) {
            // Modern, tip-güvenli metot ile dosyanın URI'ını al
            val fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)

            if (fileUri != null) {
                // Şimdilik sadece URI'ı logcat'e yazdır ve kullanıcıya mesaj göster
                Log.d("SaveFileActivity", "Dosya URI'ı alındı: $fileUri")
                Toast.makeText(this, "Dosya alındı, kaydedilecek...", Toast.LENGTH_SHORT).show()

                // TODO: DURAK 3'te burada dosyayı kaydetme ve isimlendirme ekranını açacağız.

            } else {
                Log.e("SaveFileActivity", "Paylaşılan içerikten dosya URI'ı alınamadı.")
                Toast.makeText(this, "Dosya alınamadı.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("SaveFileActivity", "Geçersiz Intent alındı.")
        }

        // İşlem bittikten sonra bu geçici Activity'yi her zaman kapat
        finish()
    }
}