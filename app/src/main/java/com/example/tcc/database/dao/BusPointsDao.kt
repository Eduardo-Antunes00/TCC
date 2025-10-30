package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.BusPointsEntity

@Dao
interface BusPointsDao {

    @Insert
    suspend fun inserirParada(parada: BusPointsEntity): Long

    @Query("SELECT * FROM bus_points")
    suspend fun listarParadas(): List<BusPointsEntity>

    @Query("SELECT * FROM bus_points WHERE id = :id")
    suspend fun buscarPorId(id: Int): BusPointsEntity?
}
