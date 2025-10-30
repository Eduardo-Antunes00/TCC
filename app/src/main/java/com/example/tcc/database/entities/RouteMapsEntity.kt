package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_maps",
    foreignKeys = [
        ForeignKey(
            entity = RoutesEntity::class,
            parentColumns = ["id"],
            childColumns = ["linha_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MapsEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapa_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RouteMapsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val linha_id: Int, // id da rota
    val mapa_id: Int   // id do mapa
)
