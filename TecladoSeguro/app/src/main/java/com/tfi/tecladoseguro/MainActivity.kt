package com.tfi.tecladoseguro

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gestorConfig: GestorConfiguracion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos nuestro motor de base de datos de configuración
        gestorConfig = GestorConfiguracion(this)

        // 1. Vincular los elementos visuales del XML (activity_main.xml) con Kotlin
        val etNumeroPrincipal = findViewById<EditText>(R.id.etNumeroPrincipal)
        val etNumeroRespaldo = findViewById<EditText>(R.id.etNumeroRespaldo)
        val rgTamanoTeclado = findViewById<RadioGroup>(R.id.rgTamanoTeclado)
        val rbNormal = findViewById<RadioButton>(R.id.rbNormal)
        val rbGrande = findViewById<RadioButton>(R.id.rbGrande)
        val rbExtraGrande = findViewById<RadioButton>(R.id.rbExtraGrande)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        // 2. Cargar la configuración guardada previamente al abrir la app
        etNumeroPrincipal.setText(gestorConfig.obtenerNumeroPrincipal())
        etNumeroRespaldo.setText(gestorConfig.obtenerNumeroRespaldo())

        when (gestorConfig.obtenerTamanoTeclado()) {
            "Grande" -> rbGrande.isChecked = true
            "Extra Grande" -> rbExtraGrande.isChecked = true
            else -> rbNormal.isChecked = true // Valor por defecto
        }

        // 3. Acción al presionar el botón "GUARDAR CONFIGURACIÓN"
        btnGuardar.setOnClickListener {
            val numeroPrincipal = etNumeroPrincipal.text.toString().trim()
            val numeroRespaldo = etNumeroRespaldo.text.toString().trim()

            // Pequeña validación: Evitar que guarden sin un número principal
            if (numeroPrincipal.isEmpty()) {
                etNumeroPrincipal.error = "Debe ingresar un número principal para las alertas"
                return@setOnClickListener
            }

            // Detectar qué tamaño de teclado eligió el usuario
            val tamanoSeleccionado = when (rgTamanoTeclado.checkedRadioButtonId) {
                R.id.rbGrande -> "Grande"
                R.id.rbExtraGrande -> "Extra Grande"
                else -> "Normal"
            }

            // Guardar todos los datos usando nuestro gestor
            gestorConfig.guardarNumeroPrincipal(numeroPrincipal)
            gestorConfig.guardarNumeroRespaldo(numeroRespaldo)
            gestorConfig.guardarTamanoTeclado(tamanoSeleccionado)

            // Confirmación de guardado
            val vistaRaiz = findViewById<android.view.View>(android.R.id.content)
            com.google.android.material.snackbar.Snackbar.make(vistaRaiz, "✅ Configuración guardada exitosamente", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setBackgroundTint(android.graphics.Color.parseColor("#2E7D32")) // Verde moderno de éxito
                .setTextColor(android.graphics.Color.WHITE)
                .show()
        }
    }
}