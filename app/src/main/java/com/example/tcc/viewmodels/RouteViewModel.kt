package com.example.tcc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.ParadaComId
import com.example.tcc.database.model.Route
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint  // IMPORT OBRIGATÓRIO

class RouteViewModel : ViewModel() {

    suspend fun pegarRotaPorId(id: String): Route? {
        return try {
            val idLong = id.toLongOrNull() ?: return null

            val snapshot = FirebaseFirestore.getInstance()
                .collection("rotas")
                .whereEqualTo("id", idLong)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return null

            val doc = snapshot.documents[0]

            val nome = doc.getString("nome") ?: "Rota $id"
            val cor = doc.getString("cor") ?: "#FF0066FF"

            // Lê como List<Map<String, Any>> (o formato que você salva!)
            val pontosRaw = doc.get("codigo") as? List<Map<String, Any>> ?: return null

            val pontos = pontosRaw.mapNotNull { map ->
                val lat = (map["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                val lng = (map["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                GeoPoint(lat, lng) // org.osmdroid.util.GeoPoint
            }

            if (pontos.size < 2) return null

            Route(
                id = idLong.toString(),
                nome = nome,
                cor = cor,
                pontos = pontos // Tipo correto: List<org.osmdroid.util.GeoPoint>
            )
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao carregar rota ID=$id", e)
            null
        }
    }

    suspend fun pegarParadasDaRota(idRota: String): List<ParadaComId> {
        return try {
            val idLong = idRota.toLongOrNull() ?: return emptyList()

            val snapshot = FirebaseFirestore.getInstance()
                .collection("rotas")
                .whereEqualTo("id", idLong)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return emptyList()

            val doc = snapshot.documents[0]
            val paradasRaw = doc.get("paradas") as? List<Map<String, Any>> ?: return emptyList()

            paradasRaw.mapNotNull { item ->
                val idParada = (item["id"] as? Number)?.toInt() ?: return@mapNotNull null

                val lat = (item["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                val lng = (item["lng"] as? Number)?.toDouble() ?: return@mapNotNull null

                ParadaComId(
                    id = idParada,
                    ponto = GeoPoint(lat, lng) // org.osmdroid.util.GeoPoint
                )
            }.sortedBy { it.id }
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao carregar paradas", e)
            emptyList()
        }
    }
}