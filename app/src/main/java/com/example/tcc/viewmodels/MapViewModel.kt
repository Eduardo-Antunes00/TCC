package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint

data class MapaTrajeto(
    val id: String,
    val nome: String,
    val pontos: List<GeoPoint>
)

class MapViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _polylines = MutableStateFlow<List<Polyline>>(emptyList())
    val polylines: StateFlow<List<Polyline>> = _polylines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val polyline = Polyline().apply {
            title = "linha_teste"
            color = 0xFFFF0000.toInt()
            width = 80f // MUITO GROSSA
            isGeodesic = true
            addPoint(GeoPoint(-29.77812, -57.102036))
            addPoint(GeoPoint(-29.778097, -57.100419))
        }
        _polylines.value = listOf(polyline)
    }

    fun carregarTrajetos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = firestore.collection("mapas")
                    .get()
                    .await()

                val novasLinhas = mutableListOf<Polyline>()

                for (doc in result.documents) {
                    val id = doc.id
                    val nome = doc.getString("nome") ?: "Trajeto"
                    val array = doc.get("array") as? List<Map<String, Any>>  // Any, não Double!

                    if (array != null && array.size >= 2) {
                        val polyline = Polyline().apply {
                            title = "linha_$id"
                            color = 0xFFFF0000.toInt() // VERMELHO para teste
                            width = 15f
                            isGeodesic = true
                        }

                        array.forEach { ponto ->
                            val lat = (ponto["lat"] as? Number)?.toDouble() ?: return@forEach
                            val lng = (ponto["lng"] as? Number)?.toDouble() ?: return@forEach
                            polyline.addPoint(GeoPoint(lat, lng))
                        }

                        novasLinhas.add(polyline)
                    }
                }

                _polylines.value = novasLinhas
            } catch (e: Exception) {
                // Tratar erro (opcional)
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