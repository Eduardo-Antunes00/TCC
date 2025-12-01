package com.example.tcc.viewmodels

import android.graphics.Color  // CORRETO para OSMDroid
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.database.model.Route
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint


class MapViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _polylines = MutableStateFlow<List<Polyline>>(emptyList())
    val polylines: StateFlow<List<Polyline>> = _polylines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun excluirRota(idCampo: String) {
        viewModelScope.launch {
            try {
                // 1. Busca o documento onde o campo "id" é igual ao valor passado
                val querySnapshot = firestore.collection("rotas")
                    .whereEqualTo("id", idCampo.toInt())  // ← aqui está o segredo!
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    Log.w("MapViewModel", "Rota com id $idCampo não encontrada")
                    return@launch
                }

                // 2. Pega o ID real do documento
                val documentId = querySnapshot.documents[0].id

                // 3. Exclui o documento correto
                firestore.collection("rotas")
                    .document(documentId)
                    .delete()
                    .await()

                Log.d("MapViewModel", "Rota $idCampo excluída com sucesso")

                // 4. Atualiza o mapa automaticamente
                carregarTrajetos()

            } catch (e: Exception) {
                Log.e("MapViewModel", "Erro ao excluir rota com id $idCampo", e)
            }
        }
    }

    fun carregarTrajetos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = firestore.collection("rotas")

                    .get()
                    .await()


                val novasLinhas = mutableListOf<Polyline>()

                for (doc in result.documents) {
                    val id = doc.id
                    val nome = doc.getString("nome") ?: "Rota $id"
                    val corString = doc.getString("cor") ?: "#FF0000" // Padrão: vermelho

                    // Converte HEX string para int (ex: "#FF5733" → Color.parseColor)
                    val corInt = try {
                        Color.parseColor(corString.uppercase())
                    } catch (e: Exception) {
                        0xFFFF0000.toInt() // Vermelho com alpha total (fallback)
                    }

                    val codigoArray = doc.get("codigo") as? List<Map<String, Any>>

                    if (codigoArray.isNullOrEmpty() || codigoArray.size < 2) continue

                    val polyline = Polyline().apply {
                        title = nome
                        color = corInt
                        width = 8f
                        isGeodesic = true
                    }

                    var pontosValidos = 0
                    for (ponto in codigoArray) {
                        val lat = (ponto["lat"] as? Number)?.toDouble()
                        val lng = (ponto["lng"] as? Number)?.toDouble()

                        if (lat != null && lng != null) {
                            polyline.addPoint(GeoPoint(lat, lng))
                            pontosValidos++
                        }
                    }

                    if (pontosValidos >= 2) {
                        novasLinhas.add(polyline)
                    }
                }

                _polylines.value = novasLinhas

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        _polylines.value = emptyList()
    }

}

suspend fun pegarRotas(): List<Route> {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("rotas")
            .orderBy("nome", Query.Direction.ASCENDING)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            val idRaw = doc.get("id")
            val id = when (idRaw) {
                is String -> idRaw
                is Number -> idRaw.toString()
                else -> doc.id  // fallback: ID do documento
            }
            val nome = doc.getString("nome") ?: "Rota $id"
            val cor = doc.getString("cor") ?: "#FF0000"

            val pontos = (doc.get("codigo") as? List<Map<String, Any>>)
                ?.mapNotNull { ponto ->
                    // Leia na ordem que o Firestore está mandando
                    val lng = (ponto["lng"] as? Number)?.toDouble()
                    val lat = (ponto["lat"] as? Number)?.toDouble()
                    if (lat != null && lng != null) {
                        GeoPoint(lat, lng)  // GeoPoint(lat, lng) ← ordem correta!
                    } else null
                }
                ?.takeIf { it.size >= 2 } ?: return@mapNotNull null

            Route(id, nome, cor, pontos)
        }
    } catch (e: Exception) {
        Log.e("ROTAS", "Erro ao carregar rotas", e)
        emptyList()
    }

}
