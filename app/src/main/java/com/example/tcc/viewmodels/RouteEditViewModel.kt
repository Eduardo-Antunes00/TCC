package com.example.tcc.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

class RouteEditViewModel : ViewModel() {

    private val _pontos = mutableStateListOf<PontoComId>()
    val pontos: List<PontoComId> = _pontos

    private val _paradas = mutableStateListOf<ParadaComId>()
    val paradas: List<ParadaComId> = _paradas

    val nomeRota = mutableStateOf("")
    val corRota = mutableStateOf("#FF0066FF")
    val isLoading = mutableStateOf(false)
    val isNewRoute = mutableStateOf(true)

    // NOVO: Horários por dia da semana

    // Substitua a linha antiga por esta:
    val horarios: SnapshotStateMap<String, String> = mutableStateMapOf(
        "Seg" to "", "Ter" to "", "Qua" to "", "Qui" to "",
        "Sex" to "", "Sáb" to "", "Dom" to ""
    )
    private val _updateTrigger = mutableStateOf(0)
    val updateTrigger: State<Int> = _updateTrigger
    private fun triggerUpdate() { _updateTrigger.value += 1 }

    private var routeId: String = ""
    private var campoId: Any = 0

    data class PontoComId(val id: Int, val ponto: GeoPoint)
    data class ParadaComId(val id: Int, val ponto: GeoPoint)

    fun init(routeIdNav: String) {
        isNewRoute.value = (routeIdNav == "new")
        if (isNewRoute.value) {
            campoId = gerarNovoId()
        } else {
            carregarRotaPorCampoId(routeIdNav.toInt())
        }
    }

    private fun gerarNovoId(): Int = (System.currentTimeMillis() / 1000).toInt()

    private fun carregarRotaPorCampoId(idValor: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("rotas")
                    .whereEqualTo("id", idValor)
                    .limit(1)
                    .get()
                    .await()
                    .documents.firstOrNull() ?: return@launch

                routeId = doc.id
                campoId = idValor

                nomeRota.value = doc.getString("nome") ?: "Rota $idValor"
                corRota.value = doc.getString("cor") ?: "#FF0066FF"

                // Carregar horários
                val horariosMap = doc.get("horarios") as? Map<String, String>
                horariosMap?.forEach { (dia, hora) -> horarios[dia] = hora }

                _pontos.clear()
                _paradas.clear()

                (doc["codigo"] as? List<Map<String, Any>>)?.forEachIndexed { index, map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEachIndexed
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEachIndexed
                    _pontos.add(PontoComId(index + 1, GeoPoint(lat, lng)))
                }

                (doc["paradas"] as? List<Map<String, Any>>)?.forEach { map ->
                    val lat = (map["lat"] as? Number)?.toDouble() ?: return@forEach
                    val lng = (map["lng"] as? Number)?.toDouble() ?: return@forEach
                    val idParada = (map["id"] as? Number)?.toInt() ?: 0
                    _paradas.add(ParadaComId(idParada, GeoPoint(lat, lng)))
                }

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

    // moverPonto, moverParada, adicionarPonto, adicionarParada, removerPonto, removerParada, renumerar... (iguais ao anterior)

    fun moverPonto(index: Int, novoPonto: GeoPoint) {
        if (index in _pontos.indices) {
            _pontos[index] = _pontos[index].copy(ponto = novoPonto)
            renumerarPontos()
        }
    }

    fun moverParada(index: Int, novoPonto: GeoPoint) {
        if (index in _paradas.indices) {
            _paradas[index] = _paradas[index].copy(ponto = novoPonto)
            renumerarParadas()
        }
    }

    fun adicionarPonto(point: GeoPoint) {
        val novoId = if (_pontos.isEmpty()) 1 else _pontos.maxOf { it.id } + 1
        if (_pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto) {
            _pontos.add(_pontos.size - 1, PontoComId(novoId, point))
        } else {
            _pontos.add(PontoComId(novoId, point))
        }
        renumerarPontos()
    }

    fun adicionarParada(point: GeoPoint) {
        val novoId = if (_paradas.isEmpty()) 1 else _paradas.maxOf { it.id } + 1
        _paradas.add(ParadaComId(novoId, point))
        renumerarParadas()
    }

    fun removerPonto(index: Int) {
        if (index !in _pontos.indices) return
        val foiFechamento = (index == _pontos.size - 1) && _pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto
        _pontos.removeAt(index)
        if (foiFechamento && _pontos.isNotEmpty()) _pontos.removeAt(_pontos.size - 1)
        renumerarPontos()
    }

    fun removerParada(index: Int) {
        if (index in _paradas.indices) {
            _paradas.removeAt(index)
            renumerarParadas()
        }
    }

    private fun renumerarPontos() {
        _pontos.forEachIndexed { i, p -> if (p.id != i + 1) _pontos[i] = p.copy(id = i + 1) }
        triggerUpdate()
    }

    private fun renumerarParadas() {
        _paradas.forEachIndexed { i, p -> if (p.id != i + 1) _paradas[i] = p.copy(id = i + 1) }
        triggerUpdate()
    }

    fun salvarRota(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                if (_pontos.size < 2) {
                    onError("Adicione pelo menos 2 pontos.")
                    return@launch
                }

                renumerarPontos()
                renumerarParadas()

                val pontosParaSalvar = if (_pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto) {
                    _pontos.map { hashMapOf("lat" to it.ponto.latitude, "lng" to it.ponto.longitude) }
                } else {
                    val lista = _pontos.toMutableList().apply { add(first().copy(ponto = first().ponto)) }
                    lista.map { hashMapOf("lat" to it.ponto.latitude, "lng" to it.ponto.longitude) }
                }

                val data = hashMapOf<String, Any>(
                    "id" to campoId,
                    "nome" to nomeRota.value.ifBlank { "Rota ${campoId}" },
                    "cor" to corRota.value,
                    "codigo" to pontosParaSalvar,
                    "paradas" to _paradas.map {
                        hashMapOf("id" to it.id, "lat" to it.ponto.latitude, "lng" to it.ponto.longitude)
                    },
                    "horarios" to horarios.filterValues { it.isNotBlank() } // só salva os preenchidos
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