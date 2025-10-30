package com.example.tcc

import android.app.Application
import com.example.tcc.di.storageModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TCCApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TCCApp)
            modules(storageModule) // adicionar outros modulos
        }
    }
}
