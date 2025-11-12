package com.example.tcc.telas

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tcc.viewmodels.MapViewModel
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.log2

@Composable
fun HomeScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    var locationPermissionGranted by remember { mutableStateOf(false) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    // ... (permissão igual)
    val polylines by mapViewModel.polylines.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 7f / 16f
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true) // MANTENHA ATIVADO!
                    controller.setZoom(15.0) // Double, não Int
                    controller.setCenter(GeoPoint(-29.7596, -57.0857))
                    mapViewState.value = this
                    setupLocationOverlay(this)
                }
            },
            update = { map ->
                mapViewState.value = map

                map.overlays.removeAll { it is Polyline && it.title?.startsWith("linha_") == true }
                polylines.forEach { map.overlays.add(it) }

                // FORÇA REDESENHO
                map.invalidate()
                // TRUQUE FINAL: delay + invalidate
                map.post { map.invalidate() }
            },
            onRelease = { map ->
                map.overlays.clear()
                map.onDetach()
                mapViewState.value = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .align(Alignment.TopCenter)
                .clipToBounds()
        )

        // CENTRALIZA APÓS DADOS CHEGAREM
        LaunchedEffect(polylines) {
            delay(200)
            mapViewState.value?.let { map ->
                map.invalidate()
                map.post { map.invalidate() }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

// Composable de permissão (agora existe!)
@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Precisamos da permissão de localização para mostrar sua posição no mapa.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder permissão")
        }
    }
}

// Configura localização UMA VEZ
private fun setupLocationOverlay(mapView: MapView) {
    // Evita duplicação
    if (mapView.overlays.any { it is MyLocationNewOverlay }) return

    if (ContextCompat.checkSelfPermission(
            mapView.context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    val locationOverlay = MyLocationNewOverlay(
        GpsMyLocationProvider(mapView.context),
        mapView
    ).apply {
        enableMyLocation()
        enableFollowLocation()

        runOnFirstFix {
            mapView.post {
                myLocation?.let {
                    mapView.controller.animateTo(it)
                }
            }
        }
    }

    mapView.overlays.add(locationOverlay)
}

fun MapView.centerOnPoints(points: List<GeoPoint>) {
    if (points.size < 2) return

    var minLat = points[0].latitude
    var maxLat = points[0].latitude
    var minLng = points[0].longitude
    var maxLng = points[0].longitude

    points.forEach { p ->
        minLat = minOf(minLat, p.latitude)
        maxLat = maxOf(maxLat, p.latitude)
        minLng = minOf(minLng, p.longitude)
        maxLng = maxOf(maxLng, p.longitude)
    }

    val center = GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2)
    controller.animateTo(center)

    // Zoom ajustado
    val latSpan = maxLat - minLat
    val lngSpan = maxLng - minLng
    val maxSpan = maxOf(latSpan, lngSpan)
    if (maxSpan > 0) {
        val zoom = 18.0 - log2(maxSpan * 111000 / height.toDouble())
        controller.setZoom(zoom.coerceIn(10.0, 18.0))
    }
}