package com.example.tcc

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File
class TCCApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
            userAgentValue = packageName
        }
    }
}
