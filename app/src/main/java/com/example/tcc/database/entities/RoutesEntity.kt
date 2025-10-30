package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RoutesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val codigo: String // texto grande, ex: dados de rota em JSON ou script
)