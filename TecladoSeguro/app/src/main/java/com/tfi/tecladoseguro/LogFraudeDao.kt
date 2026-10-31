package com.tfi.tecladoseguro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogFraudeDao {

    @Insert
    suspend fun insertarLog(log: LogFraude)

    @Query("SELECT * FROM logs_fraude ORDER BY timestamp DESC")
    suspend fun obtenerTodosLosLogs(): List<LogFraude>
}