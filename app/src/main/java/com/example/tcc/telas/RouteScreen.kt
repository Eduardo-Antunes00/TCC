package com.example.tcc.telas

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.util.TableInfo
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.RouteViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@Composable
fun RouteScreen(
    navController: NavController,
    routeId: String,
    routeViewModel: RouteViewModel = viewModel() // Instancia o ViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mapHeight by remember { mutableStateOf(0) }
    var route by remember { mutableStateOf<Route?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }

    // Carrega a rota usando o ViewModel
    LaunchedEffect(routeId) {
            Log.d("RouteScreen", "ID recebido: '$routeId'")
        isLoading = true
        error = false
        try {
            route = routeViewModel.pegarRotaPorId(routeId) // Chama o método do ViewModel
            if (route == null) error = true
        } catch (e: Exception) {
            error = true
        } finally {
            isLoading = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.8f

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    controller.setZoom(14.0)
                    mapViewState.value = this
                }
            },
            update = { map ->
                mapViewState.value = map
                map.overlays.clear()

                route?.let { r ->
                    val polyline = Polyline().apply {
                        outlinePaint.color = android.graphics.Color.parseColor(r.cor)
                        outlinePaint.strokeWidth = 8f
                        setPoints(r.pontos)
                    }
                    map.overlays.add(polyline)

                    if (r.pontos.isNotEmpty()) {
                        val bounds = polyline.bounds
                        map.controller.animateTo(bounds.centerWithDateLine)
                        map.zoomToBoundingBox(bounds, true, 50)
                    }
                }
                map.invalidate()
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

        // Loading
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Erro
        if (error) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rota não encontrada", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Voltar")
                }
            }
        }

        // Botão Voltar
        if (!isLoading && !error && route != null) {
            Column (modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(0.dp,0.dp,0.dp,maxHeight * 0.08f)
            ){
                    Text("Selecione uma parada.", style = MaterialTheme.typography.titleMedium, fontSize = 30.sp)
            }
        }

        // Título da rota
        route?.let { r ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = r.nome,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}