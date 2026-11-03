package com.tfi.tecladoseguro

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogFraude::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logFraudeDao(): LogFraudeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teclado_seguro_db"
                )
                    // --- AJUSTE CRÍTICO APLICADO ---
                    // Permite ejecutar consultas en el hilo principal temporalmente
                    // ya que retiramos el modificador 'suspend' del DAO.
                    .allowMainThreadQueries()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}