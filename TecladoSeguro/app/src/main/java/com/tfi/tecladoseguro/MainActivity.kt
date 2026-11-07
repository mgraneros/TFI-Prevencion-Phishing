package com.tfi.tecladoseguro

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gestorConfig: GestorConfiguracion
    private val CODIGO_PERMISO_SMS = 100

    // Variables para la Tarjeta de Estado del Teclado
    private lateinit var tvEstadoTeclado: TextView
    private lateinit var btnActivarTeclado: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicializamos nuestro motor de configuración
        gestorConfig = GestorConfiguracion(this)

        // 2. Lógica de Privacidad y Cumplimiento Normativo (Play Store)
        verificarPrivacidadYPermisos()

        // 3. Vincular los elementos visuales
        tvEstadoTeclado = findViewById(R.id.tvEstadoTeclado)
        btnActivarTeclado = findViewById(R.id.btnActivarTeclado)
        val etNumeroPrincipal = findViewById<EditText>(R.id.etNumeroPrincipal)
        val etNumeroRespaldo = findViewById<EditText>(R.id.etNumeroRespaldo)
        val rgTamanoTeclado = findViewById<RadioGroup>(R.id.rgTamanoTeclado)
        val rbNormal = findViewById<RadioButton>(R.id.rbNormal)
        val rbGrande = findViewById<RadioButton>(R.id.rbGrande)
        val rbExtraGrande = findViewById<RadioButton>(R.id.rbExtraGrande)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        // 4. Cargar configuración guardada
        etNumeroPrincipal.setText(gestorConfig.obtenerNumeroPrincipal())
        etNumeroRespaldo.setText(gestorConfig.obtenerNumeroRespaldo())

        when (gestorConfig.obtenerTamanoTeclado()) {
            "Grande" -> rbGrande.isChecked = true
            "Extra Grande" -> rbExtraGrande.isChecked = true
            else -> rbNormal.isChecked = true
        }

        // 5. Acción del Botón Inteligente de Activación
        btnActivarTeclado.setOnClickListener {
            manejarFlujoActivacionTeclado()
        }

        // 6. Acción del Botón Guardar Configuración
        btnGuardar.setOnClickListener {
            val numeroPrincipal = etNumeroPrincipal.text.toString().trim()
            val numeroRespaldo = etNumeroRespaldo.text.toString().trim()

            if (numeroPrincipal.isEmpty()) {
                etNumeroPrincipal.error = "Debe ingresar un número principal para las alertas"
                return@setOnClickListener
            }

            val tamanoSeleccionado = when (rgTamanoTeclado.checkedRadioButtonId) {
                R.id.rbGrande -> "Grande"
                R.id.rbExtraGrande -> "Extra Grande"
                else -> "Normal"
            }

            gestorConfig.guardarNumeroPrincipal(numeroPrincipal)
            gestorConfig.guardarNumeroRespaldo(numeroRespaldo)
            gestorConfig.guardarTamanoTeclado(tamanoSeleccionado)

            val vistaRaiz = findViewById<android.view.View>(android.R.id.content)
            com.google.android.material.snackbar.Snackbar.make(vistaRaiz, "✅ Configuración guardada exitosamente", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.parseColor("#2E7D32"))
                .setTextColor(Color.WHITE)
                .show()
        }
    }

    // Se ejecuta cada vez que el usuario vuelve a la app desde otra pantalla
    override fun onResume() {
        super.onResume()
        actualizarEstadoVisualTeclado()
    }

    // NUEVO: Se ejecuta cada vez que la app pierde o recupera el foco de la pantalla
    // (Por ejemplo, al cerrarse el cuadro emergente del sistema para elegir teclado)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            actualizarEstadoVisualTeclado()
        }
    }

    // --- FUNCIONES DEL ESTADO DEL TECLADO ---

    private fun actualizarEstadoVisualTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val miPaquete = packageName
        var estaHabilitado = false

        // 1. Verificamos si el usuario ya habilitó el teclado en los ajustes
        for (teclado in imm.enabledInputMethodList) {
            if (teclado.packageName == miPaquete) {
                estaHabilitado = true
                break
            }
        }

        // 2. Verificamos si actualmente es el teclado predeterminado (seleccionado)
        val tecladoPredeterminado = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val estaSeleccionado = tecladoPredeterminado?.contains(miPaquete) == true

        // 3. Actualizamos los colores y textos según el estado
        if (estaSeleccionado) {
            // ESTADO 3: Todo listo
            tvEstadoTeclado.text = "✅ Teclado Seguro está ACTIVO"
            tvEstadoTeclado.setTextColor(Color.parseColor("#00E676")) // Verde
            btnActivarTeclado.text = "TECLADO CONFIGURADO"
            btnActivarTeclado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2A3B52")) // Botón gris oscuro
            btnActivarTeclado.setTextColor(Color.parseColor("#8AA2B9"))
            btnActivarTeclado.isEnabled = false // Apagamos el botón
        } else if (estaHabilitado) {
            // ESTADO 2: Habilitado pero falta seleccionar
            tvEstadoTeclado.text = "⚠️ Teclado habilitado, falta seleccionar"
            tvEstadoTeclado.setTextColor(Color.parseColor("#FFCA28")) // Amarillo
            btnActivarTeclado.text = "2. SELECCIONAR TECLADO"
            btnActivarTeclado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00E676"))
            btnActivarTeclado.setTextColor(Color.parseColor("#06111C"))
            btnActivarTeclado.isEnabled = true
        } else {
            // ESTADO 1: Apagado totalmente
            tvEstadoTeclado.text = "⚠️ El teclado no está activado"
            tvEstadoTeclado.setTextColor(Color.parseColor("#FF5252")) // Rojo
            btnActivarTeclado.text = "1. HABILITAR TECLADO"
            btnActivarTeclado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00E676"))
            btnActivarTeclado.setTextColor(Color.parseColor("#06111C"))
            btnActivarTeclado.isEnabled = true
        }
    }

    private fun manejarFlujoActivacionTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val miPaquete = packageName
        var estaHabilitado = false

        for (teclado in imm.enabledInputMethodList) {
            if (teclado.packageName == miPaquete) {
                estaHabilitado = true
                break
            }
        }

        if (!estaHabilitado) {
            // Si no está habilitado, llevamos al usuario a la pantalla de ajustes de Android
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        } else {
            // Si ya está habilitado, mostramos el cuadro nativo para elegir el teclado
            imm.showInputMethodPicker()
        }
    }

    // --- FUNCIONES DE CUMPLIMIENTO NORMATIVO ---

    private fun verificarPrivacidadYPermisos() {
        val prefs = getSharedPreferences("TecladoSeguroPrefs", Context.MODE_PRIVATE)
        val privacidadAceptada = prefs.getBoolean("privacidad_aceptada", false)

        if (!privacidadAceptada) {
            mostrarDialogoPrivacidad()
        } else {
            verificarPermisoSMS()
        }
    }

    private fun mostrarDialogoPrivacidad() {
        val dialogo = Dialog(this)
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogo.setContentView(R.layout.dialogo_privacidad)
        dialogo.setCancelable(false)
        dialogo.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnAceptar = dialogo.findViewById<Button>(R.id.btnAceptar)
        val btnRechazar = dialogo.findViewById<Button>(R.id.btnRechazar)

        btnAceptar.setOnClickListener {
            val prefs = getSharedPreferences("TecladoSeguroPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("privacidad_aceptada", true).apply()

            dialogo.dismiss()
            verificarPermisoSMS()
        }

        btnRechazar.setOnClickListener {
            dialogo.dismiss()
            finish()
        }

        val anchoPantalla = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialogo.window?.setLayout(anchoPantalla, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        dialogo.show()
    }

    private fun verificarPermisoSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), CODIGO_PERMISO_SMS)
        }
    }
}