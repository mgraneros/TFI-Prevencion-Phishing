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
}