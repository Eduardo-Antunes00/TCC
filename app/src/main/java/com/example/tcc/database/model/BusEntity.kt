package com.example.tcc.database.model

import org.osmdroid.util.GeoPoint

data class OnibusInfo(
    val position: GeoPoint,
    val status: String = "Em operação",
    val velocity: Double = 0.0,
    val documentId: String = ""  // ← agora tem valor padrão
)