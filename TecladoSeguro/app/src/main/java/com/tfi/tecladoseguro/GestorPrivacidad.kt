package com.tfi.tecladoseguro

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorPrivacidad(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).logFraudeDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Ahora la función devuelve un String (el riesgo) o null (si está todo bien)
    fun procesarTexto(texto: String): String? {
        val resultados = MotorDlp.escanearTexto(texto)

        val tarjetas = resultados["tarjetas_validas"] ?: emptyList()
        val cbus = resultados["cbu_detectados"] ?: emptyList()
        val phishing = resultados["palabras_sospechosas"] ?: emptyList()

        if (tarjetas.isNotEmpty() || cbus.isNotEmpty() || phishing.size >= 2) {
            val tipoRiesgo = determinarTipoRiesgo(tarjetas, cbus, phishing)

            scope.launch {
                try {
                    val log = LogFraude(
                        timestamp = System.currentTimeMillis(),
                        tipoDatoDetectado = tipoRiesgo,
                        textoOfuscado = ofuscarDatosSensibles(texto),
                        nivelRiesgo = if (phishing.isNotEmpty() && (tarjetas.isNotEmpty() || cbus.isNotEmpty())) "Crítico" else "Alto"
                    )
                    dao.insertarLog(log)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return tipoRiesgo // Le avisamos al teclado qué detectamos
        }
        return null // No hay fraude
    }

    private fun determinarTipoRiesgo(tarjetas: List<String>, cbus: List<String>, phishing: List<String>): String {
        val tipos = mutableListOf<String>()
        if (tarjetas.isNotEmpty()) tipos.add("Tarjeta")
        if (cbus.isNotEmpty()) tipos.add("CBU")
        if (phishing.isNotEmpty()) tipos.add("Ingeniería Social")
        return tipos.joinToString(" + ")
    }

    private fun ofuscarDatosSensibles(texto: String): String {
        return texto.replace(Regex("\\b\\d{13,19}\\b"), "[TARJETA OFUSCADA]")
            .replace(Regex("\\b\\d{22}\\b"), "[CBU OFUSCADO]")
            .replace(Regex("\\b[1-9]\\d{6,7}\\b"), "[DNI OFUSCADO]")
    }
}