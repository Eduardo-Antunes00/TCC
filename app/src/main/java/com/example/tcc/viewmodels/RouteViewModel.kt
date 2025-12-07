package com.example.tcc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.OnibusInfo
import com.example.tcc.database.model.ParadaComId
import com.example.tcc.database.model.Route
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

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

    suspend fun pegarOnibusDaRota(routeId: String): List<OnibusInfo> {
        return try {
            Log.d("RouteVM", "Buscando todos os ônibus da rota: $routeId")

            val database = FirebaseDatabase.getInstance()
            val ref = database.reference.child("rotas").child(routeId).child("onibus")
            val snapshot = ref.get().await()

            val onibusList = mutableListOf<OnibusInfo>()
            snapshot.children.forEach { child ->
                val lat = child.child("lat").value as? Double ?: return@forEach
                val lng = child.child("lng").value as? Double ?: return@forEach
                val velocity = child.child("velocidade").value as? Double ?: 0.0

                onibusList.add(OnibusInfo(
                    position = GeoPoint(lat, lng),
                    status = "Em operação",  // Assumindo status padrão; ajuste se necessário
                    velocity = velocity
                ))
            }

            onibusList
        } catch (e: Exception) {
            Log.e("RouteVM", "Erro ao buscar ônibus", e)
            emptyList()
        }
    }

    fun bearingCorreto(start: GeoPoint, end: GeoPoint): Double {//ARRUMAR
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

    fun distanciaPontoALinha(ponto: GeoPoint, pontos: List<GeoPoint>): Double {
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

    fun calcularDistanciaAcumulada(ponto: GeoPoint, pontos: List<GeoPoint>): Double {
        val projetado = pontoMaisProximoNaLinha(ponto, pontos) ?: return 0.0
        var acumulado = 0.0

        for (i in 0 until pontos.size - 1) {
            val a = pontos[i]
            val b = pontos[i + 1]

            if (estaNoSegmento(projetado, a, b)) {
                acumulado += distanciaEntrePontos(a, projetado)
                return acumulado
            }
            acumulado += distanciaEntrePontos(a, b)
        }
        return acumulado
    }

    private fun estaNoSegmento(p: GeoPoint, a: GeoPoint, b: GeoPoint): Boolean {
        val distAB = distanciaEntrePontos(a, b)
        val distAP = distanciaEntrePontos(a, p)
        val distPB = distanciaEntrePontos(p, b)
        return (distAP + distPB <= distAB + 1.0)
    }

    fun calcularTempoParaParada(paradaPosition: GeoPoint, onibusList: List<OnibusInfo>, pontos: List<GeoPoint>): String {
        var minTempo = Double.MAX_VALUE
        val distParada = calcularDistanciaAcumulada(paradaPosition, pontos)

        onibusList.forEach { onibus ->
            val distOnibus = calcularDistanciaAcumulada(onibus.position, pontos)
            val dist = (distParada - distOnibus).coerceAtLeast(0.0) // Apenas se ônibus está antes da parada

            var velocity = onibus.velocity
            if (velocity < 2.78) { // < 10 km/h
                velocity = 8.33 // 30 km/h padrão em m/s
            }

            val tempo = if (velocity > 0) (dist / velocity) / 60 else Double.MAX_VALUE // em minutos

            if (tempo < minTempo) minTempo = tempo
        }

        return if (minTempo != Double.MAX_VALUE) minTempo.toInt().toString() else "Indisponível"
    }
}