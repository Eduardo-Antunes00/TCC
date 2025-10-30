package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.BusPointsRoutesEntity

@Dao
interface BusPointsRoutesDao {

    @Insert
    suspend fun inserirRelacao(rel: BusPointsRoutesEntity): Long

    @Query("SELECT * FROM bus_points_routes WHERE linha_id = :linhaId ORDER BY ordem ASC")
    suspend fun listarPorRota(linhaId: Int): List<BusPointsRoutesEntity>

    @Query("SELECT * FROM bus_points_routes WHERE parada_id = :paradaId")
    suspend fun listarPorParada(paradaId: Int): List<BusPointsRoutesEntity>
}
