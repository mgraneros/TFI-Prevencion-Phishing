package com.tfi.tecladoseguro

import android.content.Context
import android.telephony.SmsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorPrivacidad(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).logFraudeDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // 1. Instanciamos nuestro nuevo motor de configuración
    private val gestorConfig = GestorConfiguracion(context)

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
            // 2. Traemos los números de teléfono reales guardados por el usuario
            val numeroPrincipal = gestorConfig.obtenerNumeroPrincipal()
            val numeroRespaldo = gestorConfig.obtenerNumeroRespaldo()

            val numerosDestino = mutableListOf<String>()
            if (numeroPrincipal.isNotEmpty()) numerosDestino.add(numeroPrincipal)
            if (numeroRespaldo.isNotEmpty()) numerosDestino.add(numeroRespaldo)

            // Si el usuario no configuró ningún número, lo registramos y cancelamos el envío
            if (numerosDestino.isEmpty()) {
                scope.launch {
                    try {
                        dao.insertarLog(LogFraude(
                            timestamp = System.currentTimeMillis(),
                            tipoDatoDetectado = "Alerta SMS Cancelada",
                            textoOfuscado = "No hay contactos de emergencia configurados en la App.",
                            nivelRiesgo = "Info"
                        ))
                    } catch (e: Exception) {}
                }
                return
            }

            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            val mensaje = "TFI Aviso: El teclado detecto riesgo ($tipoRiesgo). Llame al usuario urgente."

            // 3. Enviamos el SMS a cada número configurado en cadena
            for (numero in numerosDestino) {
                val sentAccion = "${context.packageName}.SMS_SENT_$numero"
                val intent = android.content.Intent(sentAccion).apply {
                    setPackage(context.packageName)
                }
                val sentIntent = android.app.PendingIntent.getBroadcast(
                    context, numero.hashCode(), intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: android.content.Intent) {
                        val diagnostico = when (resultCode) {
                            android.app.Activity.RESULT_OK -> "🟢 ÉXITO: SMS enviado a $numero sin bloqueo."
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "🔴 FALLA: Bloqueo de red celular para $numero."
                            else -> "🔴 ERROR: Código $resultCode al enviar a $numero"
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

                smsManager.sendTextMessage(numero, null, mensaje, sentIntent, null)
            }

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
        if (phishing.isNotEmpty()) tipos.add("Ingenieria Social")
        return tipos.joinToString(" + ")
    }

    private fun ofuscarDatosSensibles(texto: String): String {
        return texto.replace(Regex("\\b\\d{13,19}\\b"), "[TARJETA OFUSCADA]")
            .replace(Regex("\\b\\d{22}\\b"), "[CBU OFUSCADO]")
            .replace(Regex("\\b[1-9]\\d{6,7}\\b"), "[DNI OFUSCADO]")
    }
}