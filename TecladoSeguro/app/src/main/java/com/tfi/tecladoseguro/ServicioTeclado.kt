package com.tfi.tecladoseguro

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

class ServicioTeclado : InputMethodService() {

    private lateinit var gestorPrivacidad: GestorPrivacidad
    private lateinit var gestorConfig: GestorConfiguracion
    private lateinit var barraAlertaDlp: TextView
    private var isCaps = false
    private var isCapsLocked = false
    private var isNumericMode = false
    private lateinit var listaBotonesLetras: List<Button>
    private var palabraActual = StringBuilder()

    // Variable para detectar si el usuario cambió el tamaño en la app
    private var tamanoActualAplicado = ""

    private val letrasQwerty = listOf(
        "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
        "a", "s", "d", "f", "g", "h", "j", "k", "l",
        "z", "x", "c", "v", "b", "n", "m"
    )

    private val caracteresNumericos = listOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
        "@", "#", "$", "%", "&", "-", "+", "(", ")",
        "*", "\"", "'", ":", ";", "!", "?"
    )

    override fun onCreate() {
        super.onCreate()
        gestorPrivacidad = GestorPrivacidad(this)
        gestorConfig = GestorConfiguracion(this)
        // Guardamos el tamaño que tiene el teclado al nacer
        tamanoActualAplicado = gestorConfig.obtenerTamanoTeclado()
    }

    // NUEVO: Este método se ejecuta cada vez que el teclado "aparece" en la pantalla
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        val nuevoTamano = gestorConfig.obtenerTamanoTeclado()
        // Si el tamaño guardado es diferente al que tenemos dibujado...
        if (nuevoTamano != tamanoActualAplicado) {
            tamanoActualAplicado = nuevoTamano
            // Forzamos a Android a descartar el caché y redibujar el teclado completo
            setInputView(onCreateInputView())
        }
    }

    fun evaluarTextoEscrito(textoIngresado: String) {
        if (textoIngresado.isNotBlank()) {
            val riesgoDetectado = gestorPrivacidad.procesarTexto(textoIngresado)

            if (riesgoDetectado != null) {
                barraAlertaDlp.text = "⚠️ BLOQUEADO: $riesgoDetectado SENSIBLE"
                barraAlertaDlp.visibility = View.VISIBLE

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    barraAlertaDlp.visibility = View.GONE
                }, 3000)
            }
        }
    }

    override fun onCreateInputView(): View {
        val vista = layoutInflater.inflate(R.layout.vista_teclado, null)

        barraAlertaDlp = vista.findViewById(R.id.barraAlertaDlp)

        val botonesLetrasIds = listOf(
            R.id.btnQ, R.id.btnW, R.id.btnE, R.id.btnR, R.id.btnT, R.id.btnY, R.id.btnU, R.id.btnI, R.id.btnO, R.id.btnP,
            R.id.btnA, R.id.btnS, R.id.btnD, R.id.btnF, R.id.btnG, R.id.btnH, R.id.btnJ, R.id.btnK, R.id.btnL,
            R.id.btnZ, R.id.btnX, R.id.btnC, R.id.btnV, R.id.btnB, R.id.btnN, R.id.btnM
        )

        listaBotonesLetras = botonesLetrasIds.map { vista.findViewById<Button>(it) }

        for (boton in listaBotonesLetras) {
            boton.setOnClickListener {
                val textoAEscribir = boton.text.toString()
                currentInputConnection?.commitText(textoAEscribir, 1)

                palabraActual.append(textoAEscribir)

                if (palabraActual.length > 150) {
                    palabraActual.delete(0, palabraActual.length - 150)
                }

                if (!isNumericMode && isCaps && !isCapsLocked) {
                    isCaps = false
                    actualizarTextosBotones()
                }

                evaluarTextoEscrito(palabraActual.toString())
            }
        }

        actualizarTextosBotones()

        vista.findViewById<Button>(R.id.btnComa).setOnClickListener {
            currentInputConnection?.commitText(",", 1)
            palabraActual.append(",")
            evaluarTextoEscrito(palabraActual.toString())
        }

        vista.findViewById<Button>(R.id.btnPunto).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
            palabraActual.append(".")
            evaluarTextoEscrito(palabraActual.toString())
        }

        vista.findViewById<Button>(R.id.btnEspacio).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            palabraActual.append(" ")

            if (palabraActual.length > 150) {
                palabraActual.delete(0, palabraActual.length - 150)
            }

            evaluarTextoEscrito(palabraActual.toString())
        }

        val btnBorrar = vista.findViewById<ImageButton>(R.id.btnBorrar)
        val handlerBorrado = android.os.Handler(android.os.Looper.getMainLooper())
        var borrando = false

        val accionBorrar = object : Runnable {
            override fun run() {
                if (borrando) {
                    currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                    currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))

                    if (palabraActual.isNotEmpty()) {
                        palabraActual.deleteCharAt(palabraActual.length - 1)
                    }
                    handlerBorrado.postDelayed(this, 100)
                }
            }
        }

        btnBorrar.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    borrando = true
                    handlerBorrado.post(accionBorrar)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    borrando = false
                    handlerBorrado.removeCallbacks(accionBorrar)
                    true
                }
                else -> false
            }
        }

        val btnEnter = vista.findViewById<ImageButton>(R.id.btnEnter)
        btnEnter.setOnClickListener {
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))

            evaluarTextoEscrito(palabraActual.toString())
        }

        val btnMayus = vista.findViewById<ImageButton>(R.id.btnMayus)
        btnMayus.setOnClickListener {
            if (isCapsLocked) {
                isCapsLocked = false
                isCaps = false
            } else {
                isCaps = !isCaps
            }
            actualizarTextosBotones()
        }
        btnMayus.setOnLongClickListener {
            isCapsLocked = true
            isCaps = true
            actualizarTextosBotones()
            true
        }

        val btnSimbolos = vista.findViewById<Button>(R.id.btnSimbolos)
        btnSimbolos.setOnClickListener {
            isNumericMode = !isNumericMode
            btnSimbolos.text = if (isNumericMode) "ABC" else "?123"

            if (!isNumericMode) {
                isCaps = false
                isCapsLocked = false
            }
            actualizarTextosBotones()
        }

        aplicarTamanoDinamico(vista, btnBorrar, btnEnter, btnMayus, btnSimbolos)

        return vista
    }

    private fun aplicarTamanoDinamico(vista: View, btnBorrar: View, btnEnter: View, btnMayus: View, btnSimbolos: View) {
        val tamanoSeleccionado = gestorConfig.obtenerTamanoTeclado()

        // Ajustamos la altura de la caja (DP)
        val alturaDp = when (tamanoSeleccionado) {
            "Grande" -> 60f
            "Extra Grande" -> 75f
            else -> 45f // Normal
        }

        // NUEVO: Ajustamos el tamaño de la letra para que acompañe el crecimiento
        val tamanoFuenteSp = when (tamanoSeleccionado) {
            "Grande" -> 20f
            "Extra Grande" -> 24f
            else -> 16f // Normal
        }

        val alturaPx = (alturaDp * resources.displayMetrics.density).toInt()

        val todosLosBotones = listaBotonesLetras.toMutableList<View>()
        todosLosBotones.addAll(listOf(
            vista.findViewById<Button>(R.id.btnComa),
            vista.findViewById<Button>(R.id.btnPunto),
            vista.findViewById<Button>(R.id.btnEspacio),
            btnBorrar, btnEnter, btnMayus, btnSimbolos
        ))

        for (boton in todosLosBotones) {
            boton.layoutParams = boton.layoutParams.apply {
                height = alturaPx
            }
            // Si es un botón con texto (letras, coma, punto), le agrandamos la fuente
            if (boton is Button) {
                boton.textSize = tamanoFuenteSp
            }
        }
    }

    private fun actualizarTextosBotones() {
        for ((index, boton) in listaBotonesLetras.withIndex()) {
            if (isNumericMode) {
                boton.text = caracteresNumericos[index]
            } else {
                val letra = letrasQwerty[index]
                boton.text = if (isCaps) letra.uppercase() else letra.lowercase()
            }
        }
    }
}