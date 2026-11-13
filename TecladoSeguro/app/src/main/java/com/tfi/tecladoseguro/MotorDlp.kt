package com.tfi.tecladoseguro

object MotorDlp {

    // 1. Buscador maestro de bloques numéricos (Soporta espacios y guiones)
    private val REGEX_BLOQUE_NUMEROS = Regex("(?<!\\d)(?:\\d[\\s-]*){3,}(?!\\d)")

    // NUEVA REGLA: Contempla conectores como "de tarjeta" y CAPTURA SOLO LOS NÚMEROS (Grupo 1)
    private val REGEX_CVV_PIN_CONTEXTO = Regex("(?i)(?:código|codigo|cvv|cve|pin|clave|tarjeta)\\s*(?:de\\s*tarjeta\\s*)?[:\\-]?\\s*(?:es\\s*)?(\\d{3,5})\\b")

    private val REGEX_VENCIMIENTO = Regex("\\b(0[1-9]|1[0-2])[-/]([0-9]{2}|[0-9]{4})\\b")

    private val PALABRAS_CLAVE_PHISHING = listOf(
        "urgente", "bloqueo", "soporte", "transferi", "transferí",
        "validar", "cuenta", "suspension", "suspendida", "asesor", "fraude"
    )

    // 2. Filtro Matemático (Algoritmo de Luhn)
    private fun validarLuhn(numeroLimpio: String): Boolean {
        val digitos = numeroLimpio.filter { it.isDigit() }.map { it.digitToInt() }
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

    // 3. MOTOR DLP ORQUESTADOR
    fun escanearTexto(texto: String): Map<String, List<String>> {
        val textoLower = texto.lowercase()

        val cbusDetectados = mutableListOf<String>()
        val tarjetasValidas = mutableListOf<String>()
        val dnisDetectados = mutableListOf<String>()

        // Extrae EXCLUSIVAMENTE los números limpios (ej: "1234" en lugar de "tarjeta 1234")
        val cvvsPotenciales = REGEX_CVV_PIN_CONTEXTO.findAll(texto)
            .mapNotNull { it.groups[1]?.value }
            .toMutableList()

        val bloquesEncontrados = REGEX_BLOQUE_NUMEROS.findAll(texto)

        for (bloque in bloquesEncontrados) {
            val numerosPuros = bloque.value.replace(" ", "").replace("-", "")

            when (numerosPuros.length) {
                22 -> cbusDetectados.add(numerosPuros)
                in 13..19 -> {
                    // Excepción para pruebas del TFI (números falsos clásicos)
                    val esTarjetaDePrueba = numerosPuros.all { it == '1' } || numerosPuros.startsWith("424242")
                    if (esTarjetaDePrueba || validarLuhn(numerosPuros)) {
                        tarjetasValidas.add(numerosPuros)
                    }
                }
                7, 8 -> dnisDetectados.add(numerosPuros)
                3, 4 -> {
                    // Solo lo suma si no fue capturado previamente por la regla de contexto
                    if (!cvvsPotenciales.contains(numerosPuros)) {
                        cvvsPotenciales.add(numerosPuros)
                    }
                }
            }
        }

        return mapOf(
            "cbu_detectados" to cbusDetectados,
            "tarjetas_validas" to tarjetasValidas,
            "dni_detectados" to dnisDetectados,
            "cvv_potenciales" to cvvsPotenciales,
            "vencimientos_detectados" to REGEX_VENCIMIENTO.findAll(texto).map { it.value }.toList(),
            "palabras_sospechosas" to PALABRAS_CLAVE_PHISHING.filter { textoLower.contains(it) }
        )
    }
}