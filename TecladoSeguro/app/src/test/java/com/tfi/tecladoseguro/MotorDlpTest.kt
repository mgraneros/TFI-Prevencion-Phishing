package com.tfi.tecladoseguro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorDlpTest {

    @Test
    fun pruebaDeteccionFraudeCompleto() {
        // 1. Simulamos el escenario de ingeniería social y datos sensibles
        val textoSimulado = "Hola, me llamó el asesor por el bloqueo. Te paso urgente mi CBU 0140000000000000000000 y mi tarjeta 4242424242424242 vence el 12/26 con cvv 123"

        // 2. Ejecutamos el motor de escaneo
        val resultados = MotorDlp.escanearTexto(textoSimulado)

        // 3. Mostramos los resultados en la consola de Android Studio
        println("--- RESULTADOS DEL MOTOR DLP ---")
        resultados.forEach { (categoria, datos) ->
            if (datos.isNotEmpty()) {
                println("$categoria: $datos")
            }
        }

        // 4. Afirmaciones automáticas (Asserts) para garantizar que el motor funciona
        assertTrue("Fallo: No se detectó el CBU", resultados["cbu_detectados"]!!.contains("0140000000000000000000"))
        assertTrue("Fallo: No se validó la tarjeta por Luhn", resultados["tarjetas_validas"]!!.contains("4242424242424242"))
        assertTrue("Fallo: No se detectó el vencimiento", resultados["vencimientos_detectados"]!!.contains("12/26"))
        assertTrue("Fallo: No se detectó el CVV", resultados["cvv_potenciales"]!!.contains("123"))

        // Verificamos el diccionario de phishing
        val palabrasPhishing = resultados["palabras_sospechosas"]!!
        assertEquals("Fallo: Deben ser 3 palabras clave detectadas", 3, palabrasPhishing.size)
        assertTrue(palabrasPhishing.containsAll(listOf("asesor", "bloqueo", "urgente")))
    }
}