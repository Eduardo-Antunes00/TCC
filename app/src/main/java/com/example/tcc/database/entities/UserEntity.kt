package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    foreignKeys = [
        ForeignKey(
            entity = MapsEntity::class, // Tabela "pai"
            parentColumns = ["id"],       // Coluna em mapas
            childColumns = ["mapaAtualId"], // Coluna em usuarios
            onDelete = ForeignKey.SET_NULL // Se o mapa for deletado, o campo fica nulo
        )
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val email: String,
    val senha: String,
    val cordx: Double? = null,
    val cordy: Double? = null,
    val mapaAtualId: Int? // <-- chave estrangeira para o mapa atual
)
