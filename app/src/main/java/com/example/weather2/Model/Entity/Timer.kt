package com.example.weather2.Model.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer")
data class Timer(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val timeStart: Int = 0,
        val timeEnd: Int = 0,
        val repeat: String = "",
        val status: Boolean = false
)