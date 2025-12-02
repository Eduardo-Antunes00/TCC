package com.example.tcc.database.model

import org.osmdroid.util.GeoPoint

data class OnibusInfo(
    val position: GeoPoint,
    val status: String,
    val documentId: String
)