package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tcc.database.entities.UserBusPointsEntity

@Dao
interface UserBusPointsDao {

    @Insert
    suspend fun inserirRelacao(relacao: UserBusPointsEntity): Long

    @Query("SELECT * FROM user_bus_points WHERE usuario_id = :usuarioId")
    suspend fun listarPorUsuario(usuarioId: Int): List<UserBusPointsEntity>

    @Query("SELECT * FROM user_bus_points WHERE parada_id = :paradaId")
    suspend fun listarPorParada(paradaId: Int): List<UserBusPointsEntity>

    @Query("DELETE FROM user_bus_points WHERE usuario_id = :usuarioId AND parada_id = :paradaId")
    suspend fun removerRelacao(usuarioId: Int, paradaId: Int)
}
