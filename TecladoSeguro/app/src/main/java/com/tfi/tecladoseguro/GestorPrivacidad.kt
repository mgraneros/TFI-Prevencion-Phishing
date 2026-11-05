package com.tfi.tecladoseguro

import android.content.Context
import android.telephony.SmsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorPrivacidad(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).logFraudeDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun procesarTexto(texto: String): String? {
        val resultados = MotorDlp.escanearTexto(texto)

        val tarjetas = resultados["tarjetas_validas"] ?: emptyList()
        val cbus = resultados["cbu_detectados"] ?: emptyList()
        val phishing = resultados["palabras_sospechosas"] ?: emptyList()

        if (tarjetas.isNotEmpty() || cbus.isNotEmpty() || phishing.size >= 2) {
            val tipoRiesgo = determinarTipoRiesgo(tarjetas, cbus, phishing)
            val nivelRiesgo = if (phishing.isNotEmpty() && (tarjetas.isNotEmpty() || cbus.isNotEmpty())) "Crítico" else "Alto"

            if (nivelRiesgo == "Crítico") {
                enviarAlertaSms(tipoRiesgo)
            }

            scope.launch {
                try {
                    val log = LogFraude(
                        timestamp = System.currentTimeMillis(),
                        tipoDatoDetectado = tipoRiesgo,
                        textoOfuscado = ofuscarDatosSensibles(texto),
                        nivelRiesgo = nivelRiesgo
                    )
                    dao.insertarLog(log)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return tipoRiesgo
        }
        return null
    }

    private fun enviarAlertaSms(tipoRiesgo: String) {
        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            val numeroDestino = "1124066013"

            // TEXTO GSM-7 ESTRICTO: Sin tildes y muy por debajo de los 160 caracteres (aprox 90 max)
            val mensaje = "TFI Aviso: El teclado detecto riesgo ($tipoRiesgo). Llame al usuario urgente."

            val sentAccion = "${context.packageName}.SMS_SENT"
            val intent = android.content.Intent(sentAccion).apply {
                setPackage(context.packageName)
            }
            val sentIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: android.content.Intent) {
                    val diagnostico = when (resultCode) {
                        android.app.Activity.RESULT_OK -> "🟢 ÉXITO: SMS enviado sin bloqueo de operadora."
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "🔴 FALLA: Bloqueo de red celular."
                        else -> "🔴 ERROR: Código $resultCode"
                    }

                    scope.launch {
                        try {
                            dao.insertarLog(LogFraude(
                                timestamp = System.currentTimeMillis(),
                                tipoDatoDetectado = "Auditoría de Radio",
                                textoOfuscado = diagnostico,
                                nivelRiesgo = "Info"
                            ))
                        } catch (e: Exception) {}
                    }
                    ctx.unregisterReceiver(this)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, android.content.IntentFilter(sentAccion), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, android.content.IntentFilter(sentAccion))
            }

            smsManager.sendTextMessage(numeroDestino, null, mensaje, sentIntent, null)

        } catch (e: Exception) {
            scope.launch {
                try {
                    dao.insertarLog(LogFraude(
                        timestamp = System.currentTimeMillis(),
                        tipoDatoDetectado = "Error Interno SMS",
                        textoOfuscado = e.message ?: "Fallo interno",
                        nivelRiesgo = "Crítico"
                    ))
                } catch (ex: Exception) {}
            }
        }
    }

    private fun determinarTipoRiesgo(tarjetas: List<String>, cbus: List<String>, phishing: List<String>): String {
        val tipos = mutableListOf<String>()
        if (tarjetas.isNotEmpty()) tipos.add("Tarjeta")
        if (cbus.isNotEmpty()) tipos.add("CBU")

        // SIN TILDE EN "INGENIERIA" PARA EVITAR LA CODIFICACIÓN UCS-2 QUE REDUCE EL SMS A 70 CARACTERES
        if (phishing.isNotEmpty()) tipos.add("Ingenieria Social")

        return tipos.joinToString(" + ")
    }

    private fun ofuscarDatosSensibles(texto: String): String {
        return texto.replace(Regex("\\b\\d{13,19}\\b"), "[TARJETA OFUSCADA]")
            .replace(Regex("\\b\\d{22}\\b"), "[CBU OFUSCADO]")
            .replace(Regex("\\b[1-9]\\d{6,7}\\b"), "[DNI OFUSCADO]")
    }
}