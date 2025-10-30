package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.RoutesEntity

@Dao
interface RoutesDao {

    @Insert
    suspend fun inserirRoute(route: RoutesEntity): Long

    @Query("SELECT * FROM routes")
    suspend fun listarRoutes(): List<RoutesEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun buscarPorId(id: Int): RoutesEntity?
}