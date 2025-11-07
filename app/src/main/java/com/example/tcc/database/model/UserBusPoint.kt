package com.example.tcc.database.model

data class UserBusPoint(
    val id: String = "",
    val usuario_id: String = "", // id do usuário
    val parada_id: String = ""   // id do ponto de ônibus
)
