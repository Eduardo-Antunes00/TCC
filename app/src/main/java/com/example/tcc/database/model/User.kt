package com.example.tcc.database.model

data class User(
    val id: String = "",
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val cordx: Double? = null,
    val cordy: Double? = null,
    val mapaAtualId: String? = null // id do mapa atual
)
