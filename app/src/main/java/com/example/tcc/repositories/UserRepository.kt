package com.example.tcc.repositories

import com.example.tcc.database.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("usuarios")

    suspend fun addUser(user: User) {
        usersCollection.document(user.id).set(user).await()
    }

    suspend fun getUserById(id: String): User? {
        val doc = usersCollection.document(id).get().await()
        return if (doc.exists()) doc.toObject(User::class.java) else null
    }
}
