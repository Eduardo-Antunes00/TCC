package com.example.tcc.di

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.tcc.database.TCCDataBase
import com.example.tcc.viewmodels.UsuarioViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val storageModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            TCCDataBase::class.java, // <- precisa do .java aqui
            "tcc_database.db"
        ).fallbackToDestructiveMigration()
            .build()
    }
    single {    get<TCCDataBase>().userDao()}
    single {    get<TCCDataBase>().mapsDao()}
    single {    get<TCCDataBase>().routesDao()}
    single {    get<TCCDataBase>().routeMapsDao()}
    single {    get<TCCDataBase>().busPointsDao()}
    single {    get<TCCDataBase>().busPointsRoutesDao()}
    single {    get<TCCDataBase>().userBusPointsDao()}

    viewModel { UsuarioViewModel(get()) }
}//adicionar sempre as entidades aqui