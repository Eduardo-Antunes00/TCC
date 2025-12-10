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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RouteEditViewModel : ViewModel() {

    private val _pontos = mutableStateListOf<PontoComId>()
    val pontos: List<PontoComId> = _pontos

    private val _paradas = mutableStateListOf<ParadaComId>()
    val paradas: List<ParadaComId> = _paradas

    val nomeRota = mutableStateOf("")
    val corRota = mutableStateOf("#FF0066FF")
    val isLoading = mutableStateOf(false)
    val isNewRoute = mutableStateOf(true)

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
    data class ParadaComId(val id: Int, val ponto: GeoPoint, val distanciaDoInicio: Double = 0.0)

    // === CÁLCULO DE DISTÂNCIA ===
 fun distanciaEntrePontos(a: GeoPoint, b: GeoPoint): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    // Substitua ou adicione esta função no ViewModel
    fun bearingCorreto(start: GeoPoint, end: GeoPoint): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lon2 = Math.toRadians(end.longitude)

        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)

        var bearing = Math.toDegrees(atan2(y, x))

        // AQUI ESTÁ A CORREÇÃO QUE RESOLVE TUDO:
        bearing = (bearing + 360) % 360
        bearing = 90 - bearing  // <--- ESSA LINHA É A MÁGICA
        if (bearing < 0) bearing += 360

        return bearing
    }

    // CORRIGIDA: calcula corretamente a distância acumulada até o ponto projetado na linha
    private fun calcularDistanciaAcumulada(ponto: GeoPoint): Double {
        if (pontos.size < 2) return 0.0

        val projetado = pontoMaisProximoNaLinha(ponto) ?: return 0.0
        var acumulado = 0.0

        for (i in 0 until pontos.size - 1) {
            val a = pontos[i].ponto
            val b = pontos[i + 1].ponto

            // Se o ponto projetado está neste segmento
            if (estaNoSegmento(projetado, a, b)) {
                acumulado += distanciaEntrePontos(a, projetado)
                return acumulado
            }
            acumulado += distanciaEntrePontos(a, b)
        }

        // Se chegou aqui, está depois do último ponto → usa o último segmento
        return acumulado
    }

    // Verifica se um ponto está dentro do segmento (com tolerância)
    private fun estaNoSegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Boolean {
        val distAB = distanciaEntrePontos(a, b)
        val distAP = distanciaEntrePontos(a, p)
        val distPB = distanciaEntrePontos(p, b)
        return (distAP + distPB <= distAB + 1.0) // 1 metro de tolerância
    }

    fun pontoMaisProximoNaLinha(ponto: GeoPoint): GeoPoint? {
        if (pontos.size < 2) return null
        var melhor: GeoPoint? = null
        var menorDist = Double.MAX_VALUE
        for (i in 0 until pontos.size - 1) {
            val a = pontos[i].ponto
            val b = pontos[i + 1].ponto
            val projetado = projetarPontoNoSegmento(ponto, a, b)
            val dist = distanciaEntrePontos(ponto, projetado)
            if (dist < menorDist) {
                menorDist = dist
                melhor = projetado
            }
        }
        return melhor
    }

    private fun projetarPontoNoSegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): GeoPoint {
        val lat1 = a.latitude
        val lon1 = a.longitude
        val lat2 = b.latitude
        val lon2 = b.longitude
        val dx = lon2 - lon1
        val dy = lat2 - lat1
        if (dx == 0.0 && dy == 0.0) return GeoPoint(a)
        var t = ((p.longitude - lon1) * dx + (p.latitude - lat1) * dy) / (dx * dx + dy * dy)
        t = t.coerceIn(0.0, 1.0)
        return GeoPoint(lat1 + t * dy, lon1 + t * dx)
    }

    fun distanciaPontoALinha(ponto: GeoPoint): Double {
        if (pontos.size < 2) return Double.MAX_VALUE
        var menor = Double.MAX_VALUE
        for (i in 0 until pontos.size - 1) {
            val a = pontos[i].ponto
            val b = pontos[i + 1].ponto
            val dist = distanciaPontoASegmento(ponto, a, b)
            if (dist < menor) menor = dist
        }
        return menor
    }

    private fun distanciaPontoASegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val projetado = projetarPontoNoSegmento(p, a, b)
        return distanciaEntrePontos(p, projetado)
    }


    fun tentarMoverParada(index: Int, pontoClicado: GeoPoint): Boolean {
        if (distanciaPontoALinha(pontoClicado) <= 50.0) {
            moverParada(index, pontoClicado)
            return true
        }
        return false
    }

    // === EDIÇÃO DE PONTOS (primeiro = último) ===
    fun moverPonto(index: Int, novoPonto: GeoPoint) {
        if (index !in _pontos.indices) return
        _pontos[index] = _pontos[index].copy(ponto = novoPonto)
        if (index == 0 && _pontos.size >= 2) {
            _pontos[_pontos.lastIndex] = _pontos.last().copy(ponto = novoPonto)
        } else if (index == _pontos.lastIndex && _pontos.size >= 2) {
            _pontos[0] = _pontos[0].copy(ponto = novoPonto)
        }
        recalcularDistanciasParadas()
        triggerUpdate()
    }

    fun adicionarPonto(point: GeoPoint) {
        when {
            _pontos.isEmpty() -> {
                _pontos.add(PontoComId(1, point))
                _pontos.add(PontoComId(2, point))
            }
            else -> {
                val novoId = _pontos.maxOf { it.id } + 1
                if (_pontos.size >= 2 && _pontos.last().ponto == _pontos.first().ponto) {
                    _pontos.add(_pontos.size - 1, PontoComId(novoId, point))
                } else {
                    _pontos.add(PontoComId(novoId, point))
                }
            }
        }
        reordenarIdsPontos()
        triggerUpdate()
    }
    private fun reordenarIdsPontos() {
        _pontos.forEachIndexed { index, ponto ->
            _pontos[index] = ponto.copy(id = index + 1)
        }
    }
    fun adicionarParada(point: GeoPoint) {
        val novoId = if (_paradas.isEmpty()) 1 else _paradas.maxOf { it.id } + 1
        val distancia = calcularDistanciaAcumulada(point)
        _paradas.add(ParadaComId(novoId, point, distancia))
        ordenarParadasPorDistancia()
        triggerUpdate()
    }

    fun moverParada(index: Int, novoPonto: GeoPoint) {
        if (index !in _paradas.indices) return
        val distancia = calcularDistanciaAcumulada(novoPonto)
        _paradas[index] = _paradas[index].copy(ponto = novoPonto, distanciaDoInicio = distancia)
        ordenarParadasPorDistancia()
        triggerUpdate()
    }

    fun removerPonto(index: Int) {
        if (index !in _pontos.indices) return

        val wasClosed = _pontos.size >= 3 && _pontos.first().ponto == _pontos.last().ponto

        when {
            // Caso especial: remover o ponto 1 (índice 0) em rota fechada
            wasClosed && index == 0 -> {
                // Remove o ponto 1 (índice 0)
                _pontos.removeAt(0)
                // Remove o último ponto (cópia antiga do ponto 1)
                _pontos.removeAt(_pontos.lastIndex)

                // Agora o novo ponto 1 é o antigo ponto 2
                // Criamos uma cópia dele no final para manter a rota fechada
                val novoPontoInicial = _pontos[0].ponto
                _pontos.add(_pontos.size, PontoComId(_pontos.maxOf { it.id } + 1, novoPontoInicial))
            }

            // Caso especial: remover o último ponto (cópia do primeiro)
            wasClosed && index == _pontos.lastIndex -> {
                // Remove o último (cópia)
                _pontos.removeAt(_pontos.lastIndex)
                // Remove o primeiro (original)
                _pontos.removeAt(0)
                // Recria o último como cópia do novo primeiro (antigo ponto 2)
                val novoPontoInicial = _pontos[0].ponto
                _pontos.add(PontoComId(_pontos.maxOf { it.id } + 1, novoPontoInicial))
            }

            // Qualquer outro ponto (meio ou rota aberta)
            else -> {
                _pontos.removeAt(index)

                // Se era fechada e ainda tem pontos suficientes, recria o fechamento
                if (wasClosed && _pontos.size >= 2) {
                    _pontos.removeAt(_pontos.lastIndex) // remove a cópia antiga
                    val novoPontoInicial = _pontos[0].ponto
                    _pontos.add(PontoComId(_pontos.maxOf { it.id } + 1, novoPontoInicial))
                }
            }
        }

        // Reordena IDs sequencialmente (1, 2, 3...)
        _pontos.forEachIndexed { i, ponto ->
            _pontos[i] = ponto.copy(id = i + 1)
        }
        reordenarIdsPontos()
        recalcularDistanciasParadas()
        triggerUpdate()
    }

    fun removerParada(index: Int) {
        if (index in _paradas.indices) {
            _paradas.removeAt(index)
            ordenarParadasPorDistancia()
            triggerUpdate()
        }
    }

    private fun recalcularDistanciasParadas() {
        _paradas.forEachIndexed { i, parada ->
            val novaDistancia = calcularDistanciaAcumulada(parada.ponto)
            _paradas[i] = parada.copy(distanciaDoInicio = novaDistancia)
        }
        ordenarParadasPorDistancia()
    }

    private fun ordenarParadasPorDistancia() {
        val ordenadas = _paradas.sortedBy { it.distanciaDoInicio }
        ordenadas.forEachIndexed { i, parada ->
            val indexOriginal = _paradas.indexOf(parada)
            if (indexOriginal != -1) {
                _paradas[indexOriginal] = parada.copy(id = i + 1)
            }
        }
    }

    // === CARREGAMENTO E SALVAMENTO ===
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
                    val ponto = GeoPoint(lat, lng)
                    val distancia = calcularDistanciaAcumulada(ponto)
                    _paradas.add(ParadaComId(idParada, ponto, distancia))
                }

                ordenarParadasPorDistancia()
                triggerUpdate()

            } catch (e: Exception) {
                Log.e("RouteEditVM", "Erro ao carregar rota", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun salvarRota(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                if (_pontos.size < 2) {
                    onError("Adicione pelo menos 2 pontos.")
                    return@launch
                }

                val pontosParaSalvar = _pontos.toMutableList().apply {
                    if (size >= 2 && last().ponto != first().ponto) add(first().copy())
                }.map { hashMapOf("lat" to it.ponto.latitude, "lng" to it.ponto.longitude) }

                val data = hashMapOf<String, Any>(
                    "id" to campoId,
                    "nome" to nomeRota.value.ifBlank { "Rota ${campoId}" },
                    "cor" to corRota.value,
                    "codigo" to pontosParaSalvar,
                    "paradas" to _paradas.map {
                        hashMapOf("id" to it.id, "lat" to it.ponto.latitude, "lng" to it.ponto.longitude)
                    },
                    "horarios" to horarios.filterValues { it.isNotBlank() }
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
                onError("Erro ao salvar: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
}