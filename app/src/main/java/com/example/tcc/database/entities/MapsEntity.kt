package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mapas")
data class MapsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val cidade: String,
    val coordx1: Double,
    val coordx2: Double,
    val coordy1: Double,
    val coordy2: Double
)