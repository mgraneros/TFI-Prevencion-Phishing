package com.tfi.tecladoseguro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogFraudeDao {

    @Insert
    fun insertarLog(log: LogFraude): Long

    @Query("SELECT * FROM logs_fraude ORDER BY timestamp DESC")
    fun obtenerTodosLosLogs(): List<LogFraude>
}