package com.example.weather2.Model.Dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather7d")
data class Weather7d (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time : String = "",
    val ALLSKY_SFC_PAR_TOT : Double =0.0,
    val PRECTOTCORR : Double = 0.0,
    val PS: Double = 0.0,
    val QV2M: Double = 0.0,
    val T2M_MAX: Double = 0.0,
    val T2M_MIN: Double = 0.0
    )