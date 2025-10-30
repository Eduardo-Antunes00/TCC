package com.example.tcc.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tcc.database.dao.*
import com.example.tcc.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        MapsEntity::class,
        RoutesEntity::class,
        RouteMapsEntity::class,
        BusPointsEntity::class,
        BusPointsRoutesEntity::class,
        UserBusPointsEntity::class
    ],
    version = 6, // ⚠️ Atualizar a versão sempre que adicionar ou remover tabelas!
    exportSchema = false
)
abstract class TCCDataBase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun mapsDao(): MapsDao
    abstract fun routesDao(): RoutesDao
    abstract fun routeMapsDao(): RouteMapsDao

    abstract fun busPointsDao(): BusPointsDao

    abstract fun busPointsRoutesDao(): BusPointsRoutesDao

    abstract fun userBusPointsDao(): UserBusPointsDao
}
