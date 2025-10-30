package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bus_points_routes",
    foreignKeys = [
        ForeignKey(
            entity = BusPointsEntity::class,
            parentColumns = ["id"],
            childColumns = ["parada_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutesEntity::class,
            parentColumns = ["id"],
            childColumns = ["linha_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BusPointsRoutesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val parada_id: Int,
    val linha_id: Int,
    val ordem: Int // ordem das paradas na rota
)
