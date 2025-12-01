// RouteEditViewModel.kt
package com.example.tcc.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
// RouteEditViewModel.kt (VERSÃO FINAL PERFEITA)
class RouteEditViewModel : ViewModel() {

    val pontos = mutableStateListOf<GeoPoint>()
    val paradas = mutableStateListOf<ParadaComId>() // ← nova classe!
    val nomeRota = mutableStateOf("")
    val corRota = mutableStateOf("#FF0066FF")
    val isLoading = mutableStateOf(false)
    val isNewRoute = mutableStateOf(true)

    private val _updateTrigger = mutableStateOf(0)
    val updateTrigger: State<Int> = _updateTrigger
    private fun triggerUpdate() { _updateTrigger.value += 1 }

    private var routeId: String = "" // ID do documento no Firestore
    private var campoId: Any = 0     // valor do campo "id" dentro do documento

    data class ParadaComId(val id: Int, val ponto: GeoPoint)

    fun init(routeIdNav: String) {
        isNewRoute.value = (routeIdNav == "new")
        if (isNewRoute.value) {
            campoId = gerarNovoId() // ex: 3, 4, 5...
        } else {
            carregarRotaPorCampoId(routeIdNav.toInt())
        }
    }

    private fun gerarNovoId(): Int {
        // Simples: pega o maior id existente + 1 (ou pode usar UUID)
        return (System.currentTimeMillis() / 1000).toInt()
    }

    private fun carregarRotaPorCampoId(idValor: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val query = FirebaseFirestore.getInstance()
                    .collection("rotas")
                    .whereEqualTo("id", idValor)
                    .limit(1)
                    .get()
                    .await()

                if (query.isEmpty) return@launch

                val doc = query.documents[0]
                routeId = doc.id
                campoId = idValor

                nomeRota.value = doc.getString("nome") ?: "Rota $idValor"
                corRota.value = doc.getString("cor") ?: "#FF0066FF"

                pontos.clear()
                paradas.clear()

                // Carregar pontos da rota
                (doc["codigo"] as? List<Map<String, Any>>)?.forEach { map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEach
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEach
                    pontos.add(GeoPoint(lat, lng))
                }

                // Carregar paradas
                (doc["paradas"] as? List<Map<String, Any>>)?.forEach { map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEach
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEach
                    val paradaId = (map["id"] as? Number)?.toInt() ?: 0
                    paradas.add(ParadaComId(paradaId, GeoPoint(lat, lng)))
                }

            } catch (e: Exception) {
                Log.e("RouteEditVM", "Erro ao carregar", e)
            } finally {
                isLoading.value = false
                triggerUpdate()
            }
        }
    }

    fun adicionarPonto(point: GeoPoint) {
        pontos.add(point)
        triggerUpdate()
    }

    fun adicionarParada(point: GeoPoint) {
        val novoId = (paradas.maxOfOrNull { it.id } ?: 0) + 1
        paradas.add(ParadaComId(novoId, point))
        triggerUpdate()
    }

    fun removerPonto(index: Int) {
        if (index in pontos.indices) {
            pontos.removeAt(index)
            triggerUpdate()
        }
    }

    fun removerParada(index: Int) {
        if (index in paradas.indices) {
            paradas.removeAt(index)
            triggerUpdate()
        }
    }

    fun salvarRota(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                if (pontos.size < 2) {
                    onError("Adicione pelo menos 2 pontos.")
                    return@launch
                }

                val data = hashMapOf<String, Any>(
                    "id" to campoId,
                    "nome" to nomeRota.value.ifBlank { "Rota ${campoId}" },
                    "cor" to corRota.value,
                    "codigo" to if (pontos.size >= 2) {
                        // Garante que o último ponto = primeiro ponto
                        val listaFechada = pontos.toMutableList()
                        listaFechada.add(pontos.first()) // fecha o circuito no banco!
                        listaFechada.map { hashMapOf("lat" to it.latitude, "lng" to it.longitude) }
                    } else {
                        pontos.map { hashMapOf("lat" to it.latitude, "lng" to it.longitude) }
                    },
                    "paradas" to paradas.map {
                        hashMapOf(
                            "id" to it.id,
                            "lat" to it.ponto.latitude,
                            "lng" to it.ponto.longitude
                        )
                    }
                )

                if (isNewRoute.value) {
                    FirebaseFirestore.getInstance().collection("rotas").add(data).await()
                } else {
                    FirebaseFirestore.getInstance()
                        .collection("rotas")
                        .document(routeId)
                        .set(data)
                        .await()
                }
                onSuccess()
            } catch (e: Exception) {
                onError("Erro ao salvar: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
}