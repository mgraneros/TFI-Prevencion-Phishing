package com.tfi.tecladoseguro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GestorPrivacidad(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).logFraudeDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Instanciamos nuestro motor de configuración
    private val gestorConfig = GestorConfiguracion(context)

    fun procesarTexto(texto: String): String? {
        val resultados = MotorDlp.escanearTexto(texto)

        val tarjetas = resultados["tarjetas_validas"] ?: emptyList()
        val cbus = resultados["cbu_detectados"] ?: emptyList()
        val phishing = resultados["palabras_sospechosas"] ?: emptyList()

        // Si detecta CUALQUIER información sensible (Tarjeta, CBU o Phishing)
        if (tarjetas.isNotEmpty() || cbus.isNotEmpty() || phishing.isNotEmpty()) {
            val tipoRiesgo = determinarTipoRiesgo(tarjetas, cbus, phishing)
            val nivelRiesgo = if (phishing.isNotEmpty() && (tarjetas.isNotEmpty() || cbus.isNotEmpty())) "Crítico" else "Alto"

            // Disparamos el SMS siempre que haya una detección
            enviarAlertaSms(tipoRiesgo)

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
            // Verificamos permisos antes de actuar
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            // Limpiamos los números por si el usuario dejó un espacio en blanco sin querer al escribirlos
            val numeroPrincipal = gestorConfig.obtenerNumeroPrincipal().replace("\\s".toRegex(), "")
            val numeroRespaldo = gestorConfig.obtenerNumeroRespaldo().replace("\\s".toRegex(), "")

            val numerosDestino = mutableListOf<String>()
            if (numeroPrincipal.isNotEmpty()) numerosDestino.add(numeroPrincipal)
            if (numeroRespaldo.isNotEmpty()) numerosDestino.add(numeroRespaldo)

            if (numerosDestino.isEmpty()) return

            // Usar getDefault() es la única forma 100% estable de enviar SMS desde un InputMethodService
            @Suppress("DEPRECATION")
            val smsManager: SmsManager = SmsManager.getDefault()

            // CORRECCIÓN: Texto sin tildes ni caracteres especiales para no romper el límite GSM de 160 caracteres
            val mensaje = "Teclado Seguro: La aplicacion detecto riesgo ($tipoRiesgo). Llame al usuario urgente."

            for (numero in numerosDestino) {
                val sentAccion = "${context.packageName}.SMS_SENT_$numero"
                val intent = android.content.Intent(sentAccion).apply {
                    setPackage(context.packageName)
                }

                // Intent inmutable requerido por políticas de seguridad recientes de Android
                val sentIntent = android.app.PendingIntent.getBroadcast(
                    context, numero.hashCode(), intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Registro del receiver para auditoría
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: android.content.Intent) {
                        ctx.unregisterReceiver(this) // Nos desuscribimos para no saturar la memoria
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, android.content.IntentFilter(sentAccion), Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(receiver, android.content.IntentFilter(sentAccion))
                }

                // Disparo de la red celular
                smsManager.sendTextMessage(numero, null, mensaje, sentIntent, null)
            }

        } catch (e: Exception) {
            // Evitamos que un fallo de red celular crashee el teclado
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