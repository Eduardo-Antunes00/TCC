package com.example.tcc.database.model

import org.osmdroid.util.GeoPoint
import com.google.firebase.firestore.GeoPoint as FirebaseGeoPoint
import org.osmdroid.util.GeoPoint as OsmGeoPoint
data class Route(
    val id: String = "",
    val nome: String = "",
    val cor: String = "",
    val pontos: List<GeoPoint> = emptyList(),
    val horarios: Map<String, String> = emptyMap()
)