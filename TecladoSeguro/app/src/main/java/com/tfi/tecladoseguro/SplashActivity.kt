package com.tfi.tecladoseguro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Temporizador de 2.5 segundos (2500 milisegundos)
        Handler(Looper.getMainLooper()).postDelayed({
            // Saltamos a la pantalla de configuración principal
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Destruimos esta pantalla de carga para que no quede en la memoria
            finish()
        }, 2500)
    }
}