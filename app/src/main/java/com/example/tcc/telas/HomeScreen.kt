package com.example.tcc.telas

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.MapViewModel
import com.example.tcc.viewmodels.pegarRotas
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.nio.file.WatchEvent
import kotlin.math.log2


@Composable
fun HomeScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    var locationPermissionGranted by remember { mutableStateOf(false) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    // ... (permissão igual)
    val polylines by mapViewModel.polylines.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    var rotas by remember { mutableStateOf<List<Route>>(emptyList()) }

    LaunchedEffect(navController.currentBackStackEntry) {
        // Só recarrega quando voltar para HomeScreen
        mapViewModel.carregarTrajetos()
    }

    LaunchedEffect(Unit) {
        mapViewModel.carregarTrajetos()  // Carrega as polylines
    }

    LaunchedEffect(Unit) {
        rotas = pegarRotas()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 7f / 16f
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(-29.7596, -57.0857))
                    mapViewState.value = this

                }
            },
            update = { map ->
                if (map == null) return@AndroidView
                mapViewState.value = map

                map.overlays.removeAll { it is Polyline }
                polylines.forEach { map.overlays.add(it) }
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
        DisposableEffect(Unit) {
            onDispose {
                mapViewState.value?.let { map ->
                    map.overlays.removeAll { it is Polyline }
                    map.post { map.invalidate() }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        Column (modifier = Modifier.fillMaxSize().padding(0.dp,0.dp,0.dp,30.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            ){
        LazyColumn (modifier = Modifier.width(200.dp).height(200.dp),){
                items(rotas){ rotas ->
                    Button(onClick = {
                        navController.navigate("route/${rotas.id}"){
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true}
                                     },
                        Modifier.width(200.dp))
                    { Text(rotas.nome) }
                }
            }
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
