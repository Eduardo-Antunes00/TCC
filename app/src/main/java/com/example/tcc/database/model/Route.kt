package com.example.tcc.database.model

import org.osmdroid.util.GeoPoint

data class Route(
    val id: String = "",
    val nome: String = "",
    val cor: String = "",
    val pontos: List<GeoPoint>
)
