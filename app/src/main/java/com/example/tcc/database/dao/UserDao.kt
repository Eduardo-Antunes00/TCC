package com.example.tcc.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tcc.database.entities.UserEntity

@Dao
interface UserDao {

        // 🔹 Inserir novo usuário
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun inserirUsuario(usuario: UserEntity): Long

        // 🔹 Buscar por e-mail (para login)
        @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
        suspend fun buscarPorEmail(email: String): UserEntity?

        // 🔹 Buscar todos (opcional)
        @Query("SELECT * FROM usuarios")
        suspend fun listarUsuarios(): List<UserEntity>
}
