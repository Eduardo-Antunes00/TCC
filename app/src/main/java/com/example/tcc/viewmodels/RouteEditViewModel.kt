// RouteEditViewModel.kt
package com.example.tcc.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

// =============================================
// VIEW MODEL DE EDIÇÃO DE ROTA (VERSÃO FINAL)
// =============================================
class RouteEditViewModel : ViewModel() {

    // PONTOS DA ROTA (agora com ID!)
    private val _pontos = mutableStateListOf<PontoComId>()
    val pontos: List<PontoComId> = _pontos

    // PARADAS (já com ID)
    private val _paradas = mutableStateListOf<ParadaComId>()
    val paradas: List<ParadaComId> = _paradas

    val nomeRota = mutableStateOf("")
    val corRota = mutableStateOf("#FF0066FF")
    val isLoading = mutableStateOf(false)
    val isNewRoute = mutableStateOf(true)

    private val _updateTrigger = mutableStateOf(0)
    val updateTrigger: State<Int> = _updateTrigger
    private fun triggerUpdate() { _updateTrigger.value += 1 }

    private var routeId: String = ""
    private var campoId: Any = 0

    // ============ DATA CLASSES COM ID ============
    data class PontoComId(val id: Int, val ponto: GeoPoint)
    data class ParadaComId(val id: Int, val ponto: GeoPoint)

    // ============ INICIALIZAÇÃO ============
    fun init(routeIdNav: String) {
        isNewRoute.value = (routeIdNav == "new")
        if (isNewRoute.value) {
            campoId = gerarNovoId()
        } else {
            carregarRotaPorCampoId(routeIdNav.toInt())
        }
    }

    private fun gerarNovoId(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    // ============ CARREGAR ROTA DO FIRESTORE ============
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

                _pontos.clear()
                _paradas.clear()

                // --- Carregar pontos da rota ---
                (doc["codigo"] as? List<Map<String, Any>>)?.forEachIndexed { index, map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEachIndexed
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEachIndexed
                    _pontos.add(PontoComId(index + 1, GeoPoint(lat, lng)))
                }

                // --- Carregar paradas ---
                (doc["paradas"] as? List<Map<String, Any>>)?.forEach { map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEach
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEach
                    val idParada = (map["id"] as? Number)?.toInt() ?: 0
                    _paradas.add(ParadaComId(idParada, GeoPoint(lat, lng)))
                }

                // Reordena e renumera tudo para ficar consistente
                renumerarPontos()
                renumerarParadas()

            } catch (e: Exception) {
                Log.e("RouteEditVM", "Erro ao carregar rota", e)
            } finally {
                isLoading.value = false
                triggerUpdate()
            }
        }
    }

    // ============ ADICIONAR PONTO ============
    fun adicionarPonto(point: GeoPoint) {
        val novoId = if (_pontos.isEmpty()) 1 else _pontos.maxOf { it.id } + 1

        if (_pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto) {
            // Rota já fechada → insere antes do ponto de fechamento
            _pontos.add(_pontos.size - 1, PontoComId(novoId, point))
        } else {
            _pontos.add(PontoComId(novoId, point))
        }
        renumerarPontos()
    }

    // ============ ADICIONAR PARADA ============
    fun adicionarParada(point: GeoPoint) {
        val novoId = if (_paradas.isEmpty()) 1 else _paradas.maxOf { it.id } + 1
        _paradas.add(ParadaComId(novoId, point))
        renumerarParadas()
    }

    // ============ REMOVER PONTO ============
    fun removerPonto(index: Int) {
        if (index !in _pontos.indices) return

        // Se remover o ponto de fechamento (último = primeiro), remove o duplicado também
        val foiRemovidoPontoDeFechamento = (index == _pontos.size - 1) &&
                _pontos.size >= 2 &&
                _pontos.last().ponto == _pontos.first().ponto

        _pontos.removeAt(index)

        if (foiRemovidoPontoDeFechamento && _pontos.isNotEmpty()) {
            _pontos.removeAt(_pontos.size - 1)
        }

        renumerarPontos()
    }

    // ============ REMOVER PARADA ============
    fun removerParada(index: Int) {
        if (index in _paradas.indices) {
            _paradas.removeAt(index)
            renumerarParadas()
        }
    }

    // ============ RENUMERAR PONTOS (1, 2, 3...) ============
    private fun renumerarPontos() {
        _pontos.forEachIndexed { index, ponto ->
            if (ponto.id != index + 1) {
                _pontos[index] = ponto.copy(id = index + 1)
            }
        }
        triggerUpdate()
    }

    // ============ RENUMERAR PARADAS (1, 2, 3...) ============
    private fun renumerarParadas() {
        _paradas.forEachIndexed { index, parada ->
            if (parada.id != index + 1) {
                _paradas[index] = parada.copy(id = index + 1)
            }
        }
        triggerUpdate()
    }

    // ============ SALVAR ROTA ============
    fun salvarRota(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                if (_pontos.size < 2) {
                    onError("Adicione pelo menos 2 pontos.")
                    return@launch
                }

                // Garante numeração correta antes de salvar
                renumerarPontos()
                renumerarParadas()

                // Monta lista de pontos (fecha o circuito se necessário)
                val pontosParaSalvar = if (_pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto) {
                    _pontos.map { hashMapOf("lat" to it.ponto.latitude, "lng" to it.ponto.longitude) }
                } else {
                    val listaFechada = _pontos.toMutableList().apply {
                        add(first().copy(ponto = first().ponto)) // adiciona ponto de fechamento
                    }
                    listaFechada.map { hashMapOf("lat" to it.ponto.latitude, "lng" to it.ponto.longitude) }
                }

                val data = hashMapOf<String, Any>(
                    "id" to campoId,
                    "nome" to nomeRota.value.ifBlank { "Rota ${campoId}" },
                    "cor" to corRota.value,
                    "codigo" to pontosParaSalvar,
                    "paradas" to _paradas.map {
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
                        .set(data, SetOptions.merge())
                        .await()
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("RouteEditVM", "Erro ao salvar rota", e)
                onError("Erro ao salvar: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
}