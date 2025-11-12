package com.example.tcc.telas

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Tela principal com mapa (OSM Droid) que mostra:
 * - Mapa com zoom e gestos
 * - Localização do usuário (se permissão concedida)
 * - Solicitação de permissão se necessário
 */
@Composable
fun HomeScreen() {
    // Pega o contexto atual (necessário para verificar permissão e criar o MapView)
    val context = LocalContext.current

    // Estado: se a permissão de localização foi concedida
    var locationPermissionGranted by remember { mutableStateOf(false) }

    // Referência ao MapView (para acessar depois no update)
    var mapView: MapView? by remember { mutableStateOf(null) }

    // Launcher para solicitar permissão de localização
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Callback: quando o usuário aceita ou nega
        locationPermissionGranted = granted
        if (granted) {
            // Se aceitou, configura o overlay de localização
            mapView?.let { setupLocationOverlay(it) }
        }
    }

    // Executa uma vez ao carregar a tela
    LaunchedEffect(Unit) {
        when {
            // Verifica se a permissão JÁ foi concedida
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
            }
            else -> {
                // Se não tem, solicita ao usuário
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Se a permissão NÃO foi concedida, mostra tela de solicitação
    if (!locationPermissionGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Precisamos da permissão de localização para mostrar sua posição no mapa.")
            Button(onClick = {
                // Botão para tentar novamente
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) {
                Text("Conceder permissão")
            }
        }
        return // Sai da função aqui (não mostra o mapa)
    }

    // ========================================
    // MAPA: AndroidView para integrar MapView (não-Compose)
    // ========================================
    AndroidView(
        // Cria o MapView pela primeira vez
        factory = { ctx ->
            MapView(ctx).apply {
                // Fonte de tiles: OpenStreetMap (gratuita)
                setTileSource(TileSourceFactory.MAPNIK)

                // Ativa zoom com dois dedos e gestos
                setMultiTouchControls(true)

                // Define zoom inicial (15 = nível de rua)
                controller.setZoom(15.0)

                // Centro inicial do mapa (ex: sua cidade ou ponto fixo)
                controller.setCenter(GeoPoint(-29.7596, -57.0857))

                // Salva a referência do MapView para usar depois
                mapView = this
            }
        },
        // Atualiza o mapa sempre que houver recomposição
        update = { map ->
            // Atualiza a referência
            mapView = map

            // Só configura localização se tiver permissão
            if (locationPermissionGranted) {
                if (ContextCompat.checkSelfPermission(
                        map.context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    setupLocationOverlay(map)
                }
            }
        },
        modifier = Modifier.fillMaxSize() // O mapa ocupa toda a tela
    )
}

/**
 * Configura o overlay de localização no mapa
 * - Mostra ícone de localização
 * - Segue o usuário automaticamente
 * - Centraliza na primeira localização
 */
private fun setupLocationOverlay(mapView: MapView) {
    // Remove overlay antigo (evita duplicação ao rotacionar tela)
    mapView.overlays.removeAll { it is MyLocationNewOverlay }

    // Verifica permissão ANTES de usar GPS (evita crash!)
    if (ContextCompat.checkSelfPermission(
            mapView.context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return // Sai sem fazer nada
    }

    // Cria o overlay de localização
    val locationOverlay = MyLocationNewOverlay(
        GpsMyLocationProvider(mapView.context), // Provedor de GPS
        mapView
    )

    // Ativa exibição da localização no mapa
    locationOverlay.enableMyLocation()

    // Faz o mapa seguir o usuário automaticamente
    locationOverlay.enableFollowLocation()

    // Quando a primeira localização for detectada
    locationOverlay.runOnFirstFix {
        // Executa na thread principal (UI)
        mapView.post {
            // Centraliza o mapa na posição atual
            mapView.controller.animateTo(locationOverlay.myLocation)
        }
    }

    // Adiciona o overlay ao mapa
    mapView.overlays.add(locationOverlay)
}