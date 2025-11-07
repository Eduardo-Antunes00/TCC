package com.example.tcc.database.model

data class BusPointRoute(
    val id: String = "",
    val parada_id: String = "", // id de BusPoint
    val linha_id: String = "",  // id de Route
    val ordem: Int = 0
)