package com.tfi.tecladoseguro

import android.content.Context
import android.content.SharedPreferences

class GestorConfiguracion(context: Context) {

    // Creamos un archivo de preferencias privado y seguro, solo accesible por nuestra app
    private val prefs: SharedPreferences = context.getSharedPreferences("ConfiguracionTecladoTFI", Context.MODE_PRIVATE)

    // --- 1. CONTACTO PRINCIPAL ---
    fun guardarNumeroPrincipal(numero: String) {
        prefs.edit().putString("numero_principal", numero).apply()
    }

    fun obtenerNumeroPrincipal(): String {
        return prefs.getString("numero_principal", "") ?: ""
    }

    // --- 2. CONTACTO DE RESPALDO ---
    fun guardarNumeroRespaldo(numero: String) {
        prefs.edit().putString("numero_respaldo", numero).apply()
    }

    fun obtenerNumeroRespaldo(): String {
        return prefs.getString("numero_respaldo", "") ?: ""
    }

    // --- 3. TAMAÑO DEL TECLADO ---
    // Guardamos el tamaño, por defecto será "Normal" si el usuario aún no configuró nada
    fun guardarTamanoTeclado(tamano: String) {
        prefs.edit().putString("tamano_teclado", tamano).apply()
    }

    fun obtenerTamanoTeclado(): String {
        return prefs.getString("tamano_teclado", "Normal") ?: "Normal"
    }
}