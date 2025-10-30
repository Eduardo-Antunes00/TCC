package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.MapsEntity

@Dao
interface MapsDao {

    @Insert
    suspend fun inserirMapa(mapa: MapsEntity): Long

    @Query("SELECT * FROM mapas WHERE id = :id")
    suspend fun buscarPorId(id: Int): MapsEntity?

    @Query("SELECT * FROM mapas")
    suspend fun listarTodos(): List<MapsEntity>
}
