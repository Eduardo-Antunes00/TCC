package com.example.tcc.telas

import android.graphics.Color.parseColor
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.R
import com.example.tcc.database.model.ParadaComId
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.RouteViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    navController: NavController,
    routeId: String,
    routeViewModel: RouteViewModel = viewModel()
) {
    val context = LocalContext.current

    var route by remember { mutableStateOf<Route?>(null) }
    var paradas by remember { mutableStateOf<List<ParadaComId>>(emptyList()) }
    var paradaSelecionada by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    // CORES AZUIS FODAS
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro     = Color(0xFF00D4FF)
    val azulEscuro    = Color(0xFF003366)

    LaunchedEffect(routeId) {
        isLoading = true
        error = false
        try {
            route   = routeViewModel.pegarRotaPorId(routeId)
            paradas = routeViewModel.pegarParadasDaRota(routeId)
            if (route == null) error = true
        } catch (e: Exception) {
            android.util.Log.e("RouteScreen", "Erro ao carregar rota", e)
            error = true
        } finally {
            isLoading = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.8f

        // ==================== MAPA ====================
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    controller.setZoom(14.0)
                    setBuiltInZoomControls(false)
                }
            },
            update = { map ->
                map.overlays.clear()

                // ---- POLILINHA DA ROTA ----
                route?.let { r ->
                    val points = r.pontos.toMutableList()
                    // Fecha visualmente se ainda não estiver fechada
                    if (points.size >= 2 && points.last() != points.first()) {
                        points.add(points.first())
                    }

                    val polyline = Polyline().apply {
                        outlinePaint.color = parseColor(r.cor)
                        outlinePaint.strokeWidth = 14f
                        setPoints(points)
                    }
                    map.overlays.add(polyline)

                    // Zoom automático para a rota inteira
                    polyline.bounds?.let { bounds ->
                        map.zoomToBoundingBox(bounds, true, 80)
                        map.controller.setCenter(bounds.centerWithDateLine)
                    }
                }

                // ---- MARCADORES DAS PARADAS ----
                paradas.forEach { parada ->
                    val isSelected = parada.id == paradaSelecionada

                    val marker = Marker(map).apply {
                        position = parada.ponto
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        val tintColor = if (isSelected)
                            parseColor("#00FF00")   // Verde quando selecionada
                        else
                            parseColor("#FF1744")   // Vermelho padrão

                        icon = ContextCompat.getDrawable(context, R.drawable.baseline_place_24)?.apply {
                            setTint(tintColor)
                        }

                        title   = "Parada ${parada.id}"
                        snippet = "Toque para destacar"

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

        // ==================== LOADING ====================
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp)
            }
        }

        // ==================== ERRO ====================
        if (error) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Clear, tint = Color.Red, contentDescription = null, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(24.dp))
                Text("Rota não encontrada", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
            }
        }

        // ==================== CARD NOME DA ROTA ====================
        route?.let { r ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = r.nome,
                        style = MaterialTheme.typography.titleLarge,
                        color = azulEscuro,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==================== INFO PARADA SELECIONADA ====================
        if (!isLoading && !error && route != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = maxHeight * 0.08f)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.96f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.outline_bus_alert_24),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    colorFilter = ColorFilter.tint(azulPrincipal)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = paradaSelecionada?.let { "Parada $it" }
                        ?: "Toque em uma parada no mapa",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (paradaSelecionada != null) azulPrincipal else azulEscuro.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}