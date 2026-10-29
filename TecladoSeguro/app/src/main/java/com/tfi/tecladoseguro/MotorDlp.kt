package com.tfi.tecladoseguro

object MotorDlp {

    // --- 1. EXPRESIONES REGULARES Y DICCIONARIOS ---
    private val REGEX_CBU = Regex("\\b\\d{22}\\b")
    private val REGEX_DNI = Regex("\\b[1-9]\\d{6,7}\\b")
    private val REGEX_TARJETA = Regex("\\b\\d{13,19}\\b")
    private val REGEX_VENCIMIENTO = Regex("\\b(0[1-9]|1[0-2])[-/]([0-9]{2}|[0-9]{4})\\b")
    private val REGEX_CVV = Regex("\\b\\d{3,4}\\b")

    private val PALABRAS_CLAVE_PHISHING = listOf(
        "urgente", "bloqueo", "soporte", "transferi", "transferí",
        "validar", "cuenta", "suspension", "suspendida", "asesor", "fraude"
    )

    // --- 2. LÓGICA MATEMÁTICA (Validación profunda) ---
    private fun validarLuhn(texto: String): Boolean {
        val digitos = texto.filter { it.isDigit() }.map { it.digitToInt() }
        if (digitos.size !in 13..19) return false

        var sumaTotal = 0
        val digitosReversos = digitos.reversed()

        for ((i, digito) in digitosReversos.withIndex()) {
            var valor = digito
            if (i % 2 == 1) {
                valor *= 2
                if (valor > 9) valor -= 9
            }
            sumaTotal += valor
        }
        return sumaTotal % 10 == 0
    }

    // --- 3. MOTOR DLP (Orquestador) ---
    fun escanearTexto(texto: String): Map<String, List<String>> {
        val textoLower = texto.lowercase()

        val tarjetasPotenciales = REGEX_TARJETA.findAll(texto).map { it.value }.toList()
        val tarjetasValidas = tarjetasPotenciales.filter { validarLuhn(it) }

        return mapOf(
            "cbu_detectados" to REGEX_CBU.findAll(texto).map { it.value }.toList(),
            "dni_detectados" to REGEX_DNI.findAll(texto).map { it.value }.toList(),
            "tarjetas_validas" to tarjetasValidas,
            "vencimientos_detectados" to REGEX_VENCIMIENTO.findAll(texto).map { it.value }.toList(),
            "cvv_potenciales" to REGEX_CVV.findAll(texto).map { it.value }.toList(),
            "palabras_sospechosas" to PALABRAS_CLAVE_PHISHING.filter { textoLower.contains(it) }
        )
    }
}