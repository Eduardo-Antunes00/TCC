package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.RouteMapsEntity

@Dao
interface RouteMapsDao {

    @Insert
    suspend fun inserirRelacionamento(rel: RouteMapsEntity): Long

    @Query("SELECT * FROM route_maps WHERE linha_id = :linhaId")
    suspend fun listarPorRota(linhaId: Int): List<RouteMapsEntity>

    @Query("SELECT * FROM route_maps WHERE mapa_id = :mapaId")
    suspend fun listarPorMapa(mapaId: Int): List<RouteMapsEntity>
}
