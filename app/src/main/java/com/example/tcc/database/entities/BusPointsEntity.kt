package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_points")
data class BusPointsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val lat: Double,
    val long: Double
)
