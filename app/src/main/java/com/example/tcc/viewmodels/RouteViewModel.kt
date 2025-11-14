package com.example.tcc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.Route
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

class RouteViewModel: ViewModel() {

    suspend fun pegarRotaPorId(id: String): Route? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("rotas")
                .whereEqualTo("id", id.toIntOrNull())
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return null

            val doc = snapshot.documents[0]

            val nome = doc.getString("nome") ?: "Rota $id"
            val cor = doc.getString("cor") ?: "#FF0000"

            val pontos = (doc.get("codigo") as? List<Map<String, Any>>)
                ?.mapNotNull { ponto ->
                    val lat = (ponto["lat"] as? Number)?.toDouble()
                    val lng = (ponto["lng"] as? Number)?.toDouble()
                    if (lat != null && lng != null) {
                        org.osmdroid.util.GeoPoint(lat, lng)
                    } else null
                }
                ?.takeIf { it.size >= 2 } ?: return null

            Route(id, nome, cor, pontos)
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao carregar rota ID=$id", e)
            null
        }
    }
}