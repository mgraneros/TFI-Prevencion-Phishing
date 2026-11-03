package com.tfi.tecladoseguro

import android.inputmethodservice.InputMethodService
import android.view.inputmethod.EditorInfo

class ServicioTeclado : InputMethodService() {

    private lateinit var gestorPrivacidad: GestorPrivacidad

    override fun onCreate() {
        super.onCreate()
        // Inicializamos tu motor de seguridad offline apenas el teclado se carga en memoria
        gestorPrivacidad = GestorPrivacidad(this)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Este evento se dispara cada vez que el usuario toca un campo de texto y el teclado aparece
    }

    // Esta función la conectaremos más adelante a la barra espaciadora o botón de "Enter"
    fun evaluarTextoEscrito(textoIngresado: String) {
        gestorPrivacidad.procesarTexto(textoIngresado)
    }

    override fun onCreateInputView(): android.view.View {
        // Inflamos (dibujamos) el diseño XML
        val vista = layoutInflater.inflate(R.layout.vista_teclado, null)

        // --- MAPEO DE TECLAS ---
        // Fila 1: Letras Superiores
        val btnQ = vista.findViewById<android.widget.Button>(R.id.btnQ)
        btnQ.setOnClickListener { currentInputConnection?.commitText("Q", 1) }

        val btnW = vista.findViewById<android.widget.Button>(R.id.btnW)
        btnW.setOnClickListener { currentInputConnection?.commitText("W", 1) }

        val btnE = vista.findViewById<android.widget.Button>(R.id.btnE)
        btnE.setOnClickListener { currentInputConnection?.commitText("E", 1) }

        val btnR = vista.findViewById<android.widget.Button>(R.id.btnR)
        btnR.setOnClickListener { currentInputConnection?.commitText("R", 1) }

        val btnT = vista.findViewById<android.widget.Button>(R.id.btnT)
        btnT.setOnClickListener { currentInputConnection?.commitText("T", 1) }

        val btnY = vista.findViewById<android.widget.Button>(R.id.btnY)
        btnY.setOnClickListener { currentInputConnection?.commitText("Y", 1) }

        val btnU = vista.findViewById<android.widget.Button>(R.id.btnU)
        btnU.setOnClickListener { currentInputConnection?.commitText("U", 1) }

        val btnI = vista.findViewById<android.widget.Button>(R.id.btnI)
        btnI.setOnClickListener { currentInputConnection?.commitText("I", 1) }

        val btnO = vista.findViewById<android.widget.Button>(R.id.btnO)
        btnO.setOnClickListener { currentInputConnection?.commitText("O", 1) }

        val btnP = vista.findViewById<android.widget.Button>(R.id.btnP)
        btnP.setOnClickListener { currentInputConnection?.commitText("P", 1) }

        // --- Fila 2: Letras Medias ---
        val btnA = vista.findViewById<android.widget.Button>(R.id.btnA)
        btnA.setOnClickListener { currentInputConnection?.commitText("A", 1) }
        val btnS = vista.findViewById<android.widget.Button>(R.id.btnS)
        btnS.setOnClickListener { currentInputConnection?.commitText("S", 1) }
        val btnD = vista.findViewById<android.widget.Button>(R.id.btnD)
        btnD.setOnClickListener { currentInputConnection?.commitText("D", 1) }
        val btnF = vista.findViewById<android.widget.Button>(R.id.btnF)
        btnF.setOnClickListener { currentInputConnection?.commitText("F", 1) }
        val btnG = vista.findViewById<android.widget.Button>(R.id.btnG)
        btnG.setOnClickListener { currentInputConnection?.commitText("G", 1) }
        val btnH = vista.findViewById<android.widget.Button>(R.id.btnH)
        btnH.setOnClickListener { currentInputConnection?.commitText("H", 1) }
        val btnJ = vista.findViewById<android.widget.Button>(R.id.btnJ)
        btnJ.setOnClickListener { currentInputConnection?.commitText("J", 1) }
        val btnK = vista.findViewById<android.widget.Button>(R.id.btnK)
        btnK.setOnClickListener { currentInputConnection?.commitText("K", 1) }
        val btnL = vista.findViewById<android.widget.Button>(R.id.btnL)
        btnL.setOnClickListener { currentInputConnection?.commitText("L", 1) }

        // --- Fila 3: Letras Inferiores ---
        val btnZ = vista.findViewById<android.widget.Button>(R.id.btnZ)
        btnZ.setOnClickListener { currentInputConnection?.commitText("Z", 1) }
        val btnX = vista.findViewById<android.widget.Button>(R.id.btnX)
        btnX.setOnClickListener { currentInputConnection?.commitText("X", 1) }
        val btnC = vista.findViewById<android.widget.Button>(R.id.btnC)
        btnC.setOnClickListener { currentInputConnection?.commitText("C", 1) }
        val btnV = vista.findViewById<android.widget.Button>(R.id.btnV)
        btnV.setOnClickListener { currentInputConnection?.commitText("V", 1) }
        val btnB = vista.findViewById<android.widget.Button>(R.id.btnB)
        btnB.setOnClickListener { currentInputConnection?.commitText("B", 1) }
        val btnN = vista.findViewById<android.widget.Button>(R.id.btnN)
        btnN.setOnClickListener { currentInputConnection?.commitText("N", 1) }
        val btnM = vista.findViewById<android.widget.Button>(R.id.btnM)
        btnM.setOnClickListener { currentInputConnection?.commitText("M", 1) }

        // --- Fila 4: Puntuación y Espacio ---
        val btnComa = vista.findViewById<android.widget.Button>(R.id.btnComa)
        btnComa.setOnClickListener { currentInputConnection?.commitText(",", 1) }
        val btnPunto = vista.findViewById<android.widget.Button>(R.id.btnPunto)
        btnPunto.setOnClickListener { currentInputConnection?.commitText(".", 1) }
        val btnEspacio = vista.findViewById<android.widget.Button>(R.id.btnEspacio)
        btnEspacio.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            }    // Aquí en el futuro llamaremos a tu evaluador del Motor DLP

            // --- Botones de Acción Especiales ---

            // Tecla Borrar (Es un ImageButton y envía el evento KEYCODE_DEL)
            val btnBorrar = vista.findViewById<android.widget.ImageButton>(R.id.btnBorrar)
            btnBorrar.setOnClickListener {
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
            }

            // Tecla Enter (Es un ImageButton y envía el evento KEYCODE_ENTER)
            val btnEnter = vista.findViewById<android.widget.ImageButton>(R.id.btnEnter)
            btnEnter.setOnClickListener {
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }

            // Tecla Mayúsculas (Es un ImageButton - Estructura base preparada para futura lógica)
            val btnMayus = vista.findViewById<android.widget.ImageButton>(R.id.btnMayus)
            btnMayus.setOnClickListener {
                // TODO: Alternar estado de letras a MAYÚSCULAS
            }

            // Tecla Símbolos (Es un Button normal - Estructura base preparada)
            val btnSimbolos = vista.findViewById<android.widget.Button>(R.id.btnSimbolos)
            btnSimbolos.setOnClickListener {
                // TODO: Cambiar vista a teclado numérico/símbolos
        }
        return vista
    }
}