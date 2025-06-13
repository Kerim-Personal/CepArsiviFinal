package com.example.ceparsivi

// Kayıtlı bir dosyanın adını ve tam yolunu saklayan basit bir veri sınıfı.
data class ArchivedFile(
    val fileName: String,
    val filePath: String,
    val savedDate: String
)