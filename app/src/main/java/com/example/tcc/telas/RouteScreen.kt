package com.example.tcc.telas

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.util.TableInfo
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.RouteViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint@Composable
fun RouteScreen(
    navController: NavController,
    routeId: String,
    routeViewModel: RouteViewModel = viewModel()
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf<Route?>(null) }
    var paradas by remember { mutableStateOf<List<RouteViewModel.ParadaComId>>(emptyList()) }
    var paradaSelecionada by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    // === SUAS CORES AZUIS FODAS (IGUAIS AO SPLASH E HOME) ===
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)

    LaunchedEffect(routeId) {
        isLoading = true
        error = false
        try {
            route = routeViewModel.pegarRotaPorId(routeId)
            paradas = routeViewModel.pegarParadasDaRota(routeId)
            if (route == null) error = true
        } catch (e: Exception) {
            Log.e("RouteScreen", "Erro ao carregar rota", e)
            error = true
        } finally {
            isLoading = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.8f

        // === MAPA ===
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    controller.setZoom(14.0)
                }
            },
            update = { map ->
                map.overlays.clear()

                // === LINHA DA ROTA (com a cor do banco) ===
                route?.let { r ->
                    val polyline = Polyline().apply {
                        outlinePaint.color = android.graphics.Color.parseColor(r.cor)
                        outlinePaint.strokeWidth = 12f
                        setPoints(r.pontos)
                    }
                    map.overlays.add(polyline)

                    // Zoom automático
                    polyline.bounds?.let { bounds ->
                        map.zoomToBoundingBox(bounds, true, 100)
                        map.controller.animateTo(bounds.centerWithDateLine)
                    }
                }

                // === PARADAS COM SEU ÍCONE LINDO (AZUL OU VERMELHO) ===
                paradas.forEach { parada ->
                    val marker = org.osmdroid.views.overlay.Marker(map).apply {
                        position = parada.ponto
                        setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)

                        // Usa seu outline_bus_alert_24 como pin (azul ou vermelho se selecionado)
                        val tintColor = if (parada.id == paradaSelecionada)
                            android.graphics.Color.parseColor("#0066FF") // azul quando selecionado
                        else
                            android.graphics.Color.parseColor("#0066FF") // MUDAR PARA VERDEEEEEEEEEEEEEEEEEEEEEEE

                        icon = ContextCompat.getDrawable(context, com.example.tcc.R.drawable.outline_pin_drop_24)?.apply {
                            setTint(tintColor)
                            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                        }

                        title ="Parada ${parada.id}"
                        snippet = "Toque para ver detalhes"

                        setOnMarkerClickListener { _, _ ->
                            paradaSelecionada = parada.id
                            map.controller.animateTo(parada.ponto, 17.0, 800L)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .align(Alignment.TopCenter)
                .clipToBounds()
        )

        // === LOADING AZUL ===
        if (isLoading) {
            CircularProgressIndicator(
                color = azulPrincipal,
                strokeWidth = 6.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // === ERRO ===
        if (error) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Clear, tint = Color.Red, contentDescription = "Erro", modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Rota não encontrada", color = Color.Red, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Voltar")
                }
            }
        }

        // === TEXTO DA PARADA SELECIONADA (AZUL FODA) ===
        if (!isLoading && !error && route != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = maxHeight * 0.08f)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = com.example.tcc.R.drawable.outline_bus_alert_24),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    colorFilter = ColorFilter.tint(azulPrincipal)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = paradaSelecionada?.let { "Parada: $it" } ?: "Toque em uma parada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 24.sp,
                    color = if (paradaSelecionada != null) azulPrincipal else azulEscuro.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // === CARD COM NOME DA ROTA (AZUL NO TOPO) ===
        route?.let { r ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.tcc.R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = r.nome,
                        style = MaterialTheme.typography.titleLarge,
                        color = azulEscuro,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}