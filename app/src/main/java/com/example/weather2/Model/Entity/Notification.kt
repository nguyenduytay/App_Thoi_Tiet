package com.example.weather2.Model.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification")
data class Notification(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: Int=0,
    val temp: Boolean=false,
    val humidityAir: Boolean=false,
    val humidityLand: Boolean=false,
    val status : Boolean=false
)