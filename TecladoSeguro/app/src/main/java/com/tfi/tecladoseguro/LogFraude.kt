package com.tfi.tecladoseguro

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs_fraude")
data class LogFraude(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val tipoDatoDetectado: String, // Ej: "Tarjeta + CVV"
    val textoOfuscado: String,     // Ej: "**** **** **** 4242"
    val nivelRiesgo: String        // Ej: "Alto"
)