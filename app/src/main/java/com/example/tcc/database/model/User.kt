package com.example.tcc.database.model

import com.google.firebase.firestore.PropertyName

data class User(
    var documentId: String = "", // opcional, se quiser guardar o ID do doc

    @PropertyName("id")
    val id: String? = null,

    @PropertyName("nome")
    val nome: String? = null,

    @PropertyName("email")
    val email: String? = null,

    @PropertyName("cordx")
    val cordx: Double? = null,

    @PropertyName("cordy")
    val cordy: Double? = null,

    @PropertyName("acesso")
    val acesso: Long? = null,
    @PropertyName("ativo")
    val ativo: Boolean? = true,
)