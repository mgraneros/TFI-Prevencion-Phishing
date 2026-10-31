package com.tfi.tecladoseguro

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorPrivacidad(context: Context) {

    // 1. Instanciamos la base de datos y el DAO
    private val dao = AppDatabase.getDatabase(context).logFraudeDao()

    // 2. Definimos un hilo en segundo plano (IO) para no congelar el teclado
    private val scope = CoroutineScope(Dispatchers.IO)

    fun procesarTexto(texto: String) {
        // Ejecutamos tu Motor DLP
        val resultados = MotorDlp.escanearTexto(texto)

        val tarjetas = resultados["tarjetas_validas"] ?: emptyList()
        val cbus = resultados["cbu_detectados"] ?: emptyList()
        val phishing = resultados["palabras_sospechosas"] ?: emptyList()

        // Si detectamos datos sensibles o un intento claro de phishing
        if (tarjetas.isNotEmpty() || cbus.isNotEmpty() || phishing.size >= 2) {

            // 3. Lanzamos el guardado en segundo plano
            scope.launch {
                val log = LogFraude(
                    timestamp = System.currentTimeMillis(),
                    tipoDatoDetectado = determinarTipoRiesgo(tarjetas, cbus, phishing),
                    textoOfuscado = ofuscarDatosSensibles(texto),
                    nivelRiesgo = if (phishing.isNotEmpty() && (tarjetas.isNotEmpty() || cbus.isNotEmpty())) "Crítico" else "Alto"
                )
                dao.insertarLog(log)
            }
        }
    }

    // --- FUNCIONES AUXILIARES DE PRIVACIDAD ---

    private fun determinarTipoRiesgo(tarjetas: List<String>, cbus: List<String>, phishing: List<String>): String {
        val tipos = mutableListOf<String>()
        if (tarjetas.isNotEmpty()) tipos.add("Tarjeta")
        if (cbus.isNotEmpty()) tipos.add("CBU")
        if (phishing.isNotEmpty()) tipos.add("Ingeniería Social")
        return tipos.joinToString(" + ")
    }

    private fun ofuscarDatosSensibles(texto: String): String {
        // Privacy-by-design: Reemplazamos los números reales antes de tocar la base de datos local
        return texto.replace(Regex("\\b\\d{13,19}\\b"), "[TARJETA OFUSCADA]")
            .replace(Regex("\\b\\d{22}\\b"), "[CBU OFUSCADO]")
            .replace(Regex("\\b[1-9]\\d{6,7}\\b"), "[DNI OFUSCADO]")
    }
}