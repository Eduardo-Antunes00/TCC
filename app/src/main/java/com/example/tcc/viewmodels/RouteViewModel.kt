package com.example.tcc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.OnibusInfo
import com.example.tcc.database.model.ParadaComId
import com.example.tcc.database.model.Route
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

class RouteViewModel : ViewModel() {

    // Mapa para armazenar o estado anterior de cada ônibus (por documentId/uid)
    private val busStates = mutableMapOf<String, BusState>()

    data class BusState(var lastCumDist: Double = 0.0)

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

            val pontosRaw = doc.get("codigo") as? List<Map<String, Any>> ?: return null

            val pontos = pontosRaw.mapNotNull { map ->
                val lat = (map["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                val lng = (map["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                GeoPoint(lat, lng)
            }

            if (pontos.size < 2) return null

            val horariosMap = doc.get("horarios") as? Map<String, String> ?: emptyMap()

            Route(
                id = idLong.toString(),
                nome = nome,
                cor = cor,
                pontos = pontos,
                horarios = horariosMap
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
                    ponto = GeoPoint(lat, lng)
                )
            }.sortedBy { it.id }
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao carregar paradas", e)
            emptyList()
        }
    }

    // CORRIGIDO: Busca no Realtime Database na estrutura onibus/{uid} com rotaCodigo
    // === NOVA FUNÇÃO: pega o documentId da rota pelo campo "id" (número da linha) ===
    private suspend fun getDocumentIdDaRota(routeIdNumero: String): String? {
        return try {
            val idLong = routeIdNumero.toLongOrNull() ?: return null
            val snapshot = FirebaseFirestore.getInstance()
                .collection("rotas")
                .whereEqualTo("id", idLong)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao buscar documentId da rota $routeIdNumero", e)
            null
        }
    }

    // === FUNÇÃO CORRIGIDA: agora usa o documentId real ===
    suspend fun pegarOnibusDaRota(routeIdNumero: String): List<OnibusInfo> {
        return try {
            Log.d("RouteVM", "Buscando ônibus da rota número $routeIdNumero")

            // 1. Primeiro: pega o documentId real da rota (ex: "8jdCIDbxEldgCZyC4lFU")
            val documentId = getDocumentIdDaRota(routeIdNumero) ?: run {
                Log.w("RouteVM", "Rota $routeIdNumero não encontrada no Firestore")
                return emptyList()
            }

            Log.d("RouteVM", "DocumentId da rota: $documentId → buscando ônibus com rotaCodigo = $documentId")

            val database = FirebaseDatabase.getInstance()
            val snapshot = database.reference.child("onibus").get().await()

            if (!snapshot.exists()) {
                Log.w("RouteVM", "Nenhum ônibus na coleção 'onibus'")
                return emptyList()
            }

            val onibusList = mutableListOf<OnibusInfo>()
            snapshot.children.forEach { child ->
                val uid = child.key ?: return@forEach
                val lat = child.child("lat").value as? Double ?: return@forEach
                val lng = child.child("lng").value as? Double ?: return@forEach
                val rotaCodigo = child.child("rotaCodigo").value as? String
                val velocidade = child.child("velocidade").value as? Double ?: 0.0
                val status = child.child("status").value as? String ?: "Em operação"

                // FILTRA PELO DOCUMENTID REAL (não pelo número!)
                if (rotaCodigo == documentId) {
                    onibusList.add(OnibusInfo(
                        position = GeoPoint(lat, lng),
                        status = status,
                        velocity = velocidade,
                        documentId = uid
                    ))
                }
            }

            Log.d("RouteVM", "Encontrados ${onibusList.size} ônibus ativos na rota $routeIdNumero (docId: $documentId)")
            onibusList
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao buscar ônibus da rota $routeIdNumero", e)
            emptyList()
        }
    }

    fun bearingCorreto(start: GeoPoint, end: GeoPoint): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lon2 = Math.toRadians(end.longitude)

        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        bearing = 90 - bearing
        if (bearing < 0) bearing += 360

        return bearing
    }

    fun distanciaEntrePontos(a: GeoPoint, b: GeoPoint): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0].toDouble()
    }

    fun distanciaPontoALinha(ponto: GeoPoint, pontos: List<GeoPoint>): Double{
        if (pontos.size < 2) return Double.MAX_VALUE
        var menor = Double.MAX_VALUE
        for (i in 0 until pontos.size - 1) {
            val a = pontos[i]
            val b = pontos[i + 1]
            val dist = distanciaPontoASegmento(ponto, a, b)
            if (dist < menor) menor = dist
        }
        return menor
    }

    private fun distanciaPontoASegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val projetado = projetarPontoNoSegmento(p, a, b)
        return distanciaEntrePontos(p, projetado)
    }

    fun pontoMaisProximoNaLinha(ponto: GeoPoint, pontos: List<GeoPoint>): GeoPoint? {
        if (pontos.size < 2) return null
        var melhor: GeoPoint? = null
        var menorDist = Double.MAX_VALUE
        for (i in 0 until pontos.size - 1) {
            val a = pontos[i]
            val b = pontos[i + 1]
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

    // Nova função para calcular distâncias acumuladas dos vértices (otimização)
    private fun calcularVertexCumDists(pontos: List<GeoPoint>): List<Double> {
        val vertexCumDists = mutableListOf(0.0)
        var acc = 0.0
        for (i in 1 until pontos.size) {
            acc += distanciaEntrePontos(pontos[i - 1], pontos[i])
            vertexCumDists.add(acc)
        }
        return vertexCumDists
    }

    // Nova função para obter candidatos de projeção (segmentos próximos o suficiente)
    private fun getProjectionCandidates(
        ponto: GeoPoint,
        pontos: List<GeoPoint>,
        vertexCumDists: List<Double>,
        maxPerpDist: Double = 200.0  // Limite de distância perpendicular (metros)
    ): List<Pair<Double, GeoPoint>> {  // Pair<cumDist, projectedPoint>
        val candidates = mutableListOf<Pair<Double, GeoPoint>>()
        for (i in 0 until pontos.size - 1) {
            val a = pontos[i]
            val b = pontos[i + 1]
            val projetado = projetarPontoNoSegmento(ponto, a, b)
            val perpDist = distanciaEntrePontos(ponto, projetado)
            if (perpDist <= maxPerpDist) {
                val segmentStartCum = vertexCumDists[i]
                val offset = distanciaEntrePontos(a, projetado)
                val cum = segmentStartCum + offset
                candidates.add(cum to projetado)
            }
        }
        return candidates.sortedBy { it.first }  // Ordenado por distância acumulada
    }

    private fun estaNoSegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Boolean {
        val distAB = distanciaEntrePontos(a, b)
        val distAP = distanciaEntrePontos(a, p)
        val distPB = distanciaEntrePontos(p, b)
        return (distAP + distPB <= distAB + 1.0)
    }

    // 1. CORRIGIDA: agora aceita ônibus que já passaram da parada
    fun calcularTempoParaParada(paradaPosition: GeoPoint, onibusList: List<OnibusInfo>, pontos: List<GeoPoint>): String {
        if (onibusList.isEmpty() || pontos.size < 2) return "Indeterminado"

        val distParada = calcularDistanciaAcumuladaDoOnibusAtual(paradaPosition, pontos, "parada_fixa") // ID fixo pra parada
        val comprimentoTotal = calcularVertexCumDists(pontos).last()

        var menorTempo = Double.MAX_VALUE
        var melhorTexto = "Indeterminado"
        val temOnibusEmOperacao = onibusList.any { it.status == "Em operação" }
        if (!temOnibusEmOperacao) return "Indeterminado"
        onibusList.forEach { onibus ->
            // Agora passa o documentId do ônibus
            val distOnibus = calcularDistanciaAcumuladaDoOnibusAtual(onibus.position, pontos, onibus.documentId)

            var diferenca = distParada - distOnibus
            if (diferenca < -50) {
                diferenca += comprimentoTotal
            }

            val distanciaRestante = diferenca.coerceAtLeast(0.0)

            var velocidade = onibus.velocity
            if (velocidade < 5.55) velocidade = 7.0

            val tempoMinutos = if (velocidade > 0) (distanciaRestante / velocidade) / 60.0 else Double.MAX_VALUE

            if (tempoMinutos < menorTempo) {
                menorTempo = tempoMinutos
                melhorTexto = when {
                    tempoMinutos < 0.5 -> "Chegando"
                    tempoMinutos < 1   -> "<1 min"
                    tempoMinutos < 2   -> "1 min"
                    tempoMinutos > 120 -> ">2h"
                    tempoMinutos > 90  -> "Próxima volta: >90 min"
                    tempoMinutos > 60  -> "Próxima volta: ${tempoMinutos.toInt()} min"
                    else               -> "${tempoMinutos.toInt()} min"
                }
            }
        }

        return melhorTexto
    }

    fun calcularDistanciaAcumuladaDoOnibusAtual(
        busPosition: GeoPoint,
        pontos: List<GeoPoint>,
        busId: String  // Adicione o ID do ônibus para rastrear por veículo
    ): Double {
        if (pontos.size < 2) return 0.0

        // Calcula o ponto projetado mais próximo (como antes)
        val projetado = pontoMaisProximoNaLinha(busPosition, pontos) ?: return 0.0
        val cumDists = calcularVertexCumDists(pontos)

        var novaDistAcumulada = 0.0
        var encontrado = false

        for (i in 0 until pontos.size - 1) {
            val a = pontos[i]
            val b = pontos[i + 1]
            if (estaNoSegmento(projetado, a, b)) {
                novaDistAcumulada = cumDists[i] + distanciaEntrePontos(a, projetado)
                encontrado = true
                break
            }
        }

        if (!encontrado) {
            novaDistAcumulada = cumDists.last() + distanciaEntrePontos(pontos.last(), projetado)
        }

        // === AQUI ESTÁ O VERIFICADOR SIMPLES E PODEROSO ===
        val state = busStates.getOrPut(busId) { BusState() }

        // Se a nova distância é MENOR que a anterior (andou "pra trás"), ignora e mantém a antiga
        // Tolerância de 30 metros para evitar falsos positivos por ruído de GPS
        if (novaDistAcumulada < state.lastCumDist - 30.0) {
            // Ignora essa atualização "suspeita" — mantém a distância antiga
            return state.lastCumDist
        }

        // Se avançou (ou é a primeira vez), atualiza normalmente
        state.lastCumDist = novaDistAcumulada
        return novaDistAcumulada
    }
}