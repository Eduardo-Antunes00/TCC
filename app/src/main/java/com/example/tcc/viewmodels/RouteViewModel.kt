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
                .whereEqualTo("id", id.toLongOrNull())
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
    // No seu RouteViewModel.kt
    data class ParadaComId(
        val id: String,
        val ponto: org.osmdroid.util.GeoPoint
    )

    suspend fun pegarParadasDaRota(idRota: String): List<ParadaComId> {
        return try {

            val idLong = idRota.toLongOrNull() ?: run {
                return emptyList()
            }

            val snapshot = FirebaseFirestore.getInstance()
                .collection("rotas")
                .whereEqualTo("id", idLong)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return emptyList()
            }

            val doc = snapshot.documents[0]

            val paradasRaw = doc.get("paradas") as? List<Map<String, Any>> ?: run {
                return emptyList()
            }

            val resultado = paradasRaw.mapNotNull { item ->
                val idParada = item["id"] as? String
                    ?: item["id"]?.toString() // tenta converter qualquer coisa pra string
                    ?: return@mapNotNull null

                // TENTA TODOS OS FORMATOS POSSÍVEIS
                val lat = when (val l = item["lat"]) {
                    is Number -> l.toDouble()
                    is String -> l.toDoubleOrNull()
                    else -> null
                } ?: return@mapNotNull null

                val lng = when (val l = item["lng"]) {
                    is Number -> l.toDouble()
                    is String -> l.toDoubleOrNull()
                    else -> null
                } ?: return@mapNotNull null

                ParadaComId(
                    id = idParada,
                    ponto = org.osmdroid.util.GeoPoint(lat, lng)
                )
            }

            resultado
        } catch (e: Exception) {
            Log.e("RouteVM", "ERRO CRÍTICO ao carregar paradas", e)
            emptyList()
        }
    }
}

