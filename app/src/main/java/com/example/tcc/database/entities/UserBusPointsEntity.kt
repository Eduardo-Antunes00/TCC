package com.example.tcc.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_bus_points",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BusPointsEntity::class,
            parentColumns = ["id"],
            childColumns = ["parada_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserBusPointsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuario_id: Int,
    val parada_id: Int
)
