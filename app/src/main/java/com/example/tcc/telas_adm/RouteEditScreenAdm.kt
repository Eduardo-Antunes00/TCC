// RouteEditScreenAdm.kt
package com.example.tcc.telas_adm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.viewmodels.RouteEditViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditScreenAdm(
    routeId: String,
    navController: NavController,
    viewModel: RouteEditViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Estado para o seletor de cor
    // Sincroniza automaticamente com a cor atual do ViewModel
    var selectedColor by remember { mutableStateOf(viewModel.corRota.value) }
    LaunchedEffect(viewModel.corRota.value) {
        selectedColor = viewModel.corRota.value
    }

    val mapView = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(routeId) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        viewModel.init(routeId)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // === TOP BAR ===
        TopAppBar(
            title = {
                Text(
                    text = if (viewModel.isNewRoute.value) "Nova Rota" else "Editar Rota",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Voltar")
                }
            },
            actions = {
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Default.Done, tint = Color.White, contentDescription = "Salvar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0066FF))
        )

        // === MAPA (80% da tela) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .clipToBounds()
        ) {
            AndroidView(
                factory = {
                    MapView(it).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(-29.770881, -57.086261))
                        mapView.value = this

                        val receiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint) = run {
                                clickedPoint = p
                                showDialog = true
                                true
                            }
                            override fun longPressHelper(p: GeoPoint) = false
                        }
                        overlays.add(0, MapEventsOverlay(receiver))
                    }
                },
                update = { map ->
                    viewModel.updateTrigger
                    map.overlays.clear()

                    // POLYLINE FECHADA
                    if (viewModel.pontos.isNotEmpty()) {
                        val points = viewModel.pontos.toMutableList().apply {
                            if (size >= 2) add(first())
                        }
                        map.overlays.add(Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor(viewModel.corRota.value)
                            outlinePaint.strokeWidth = 12f
                            setPoints(points)
                        })
                    }

                    // PONTOS DA ROTA
                    viewModel.pontos.forEachIndexed { i, p ->
                        map.overlays.add(Marker(map).apply {
                            position = p
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)?.apply {
                                setTint(android.graphics.Color.BLACK)
                            }
                            setOnMarkerClickListener { _, _ ->
                                showDeleteDialog = Pair("ponto", i); true
                            }
                        })
                    }

                    // PARADAS
                    viewModel.paradas.forEachIndexed { i, parada ->
                        map.overlays.add(Marker(map).apply {
                            position = parada.ponto
                            title = "Parada ${parada.id}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = context.getDrawable(com.example.tcc.R.drawable.baseline_place_24)?.apply {
                                setTint(android.graphics.Color.RED)
                            }
                            setOnMarkerClickListener { _, _ ->
                                showDeleteDialog = Pair("parada", i); true
                            }
                        })
                    }

                    // RE-ADICIONA O EVENTO DE CLIQUE
                    val receiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint) = run {
                            clickedPoint = p; showDialog = true; true
                        }
                        override fun longPressHelper(p: GeoPoint) = false
                    }
                    map.overlays.add(0, MapEventsOverlay(receiver))

                    map.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // === TEXTO EXPLICATIVO (20% da tela) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Toque no mapa para adicionar pontos da rota ou paradas.\n" +
                        "Toque em um ponto/parada para excluí-lo.",
                fontSize = 16.sp,
                color = Color(0xFF003366),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // === DIÁLOGO: Adicionar ponto ou parada ===
    if (showDialog && clickedPoint != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Adicionar no mapa") },
            text = { Text("O que você deseja adicionar neste ponto?") },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.adicionarPonto(clickedPoint!!)
                            showDialog = false
                        }
                    ) {
                        Text("Ponto da Rota")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.adicionarParada(clickedPoint!!)
                            showDialog = false
                        }
                    ) {
                        Text("Parada")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // === DIÁLOGO: Excluir ===
    showDeleteDialog?.let { (tipo, index) ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Excluir ${if (tipo == "ponto") "ponto" else "parada"}?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton({
                    if (tipo == "ponto") viewModel.removerPonto(index)
                    else viewModel.removerParada(index)
                    showDeleteDialog = null
                }) { Text("Excluir", color = Color.Red) }
            },
            dismissButton = { TextButton({ showDeleteDialog = null }) { Text("Cancelar") } }
        )
    }

    // === DIÁLOGO: Salvar com seletor de cor ===
    if (showSaveDialog) {
        val cores = listOf(
            "#FF0066FF", "#FFFF0000", "#FF00FF00", "#FFFFA500", "#FF9C27B0",
            "#FFFF5722", "#FF795548", "#FF607D8B", "#FF04F928", "#FFFFEB3B"
        )

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Salvar Rota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.nomeRota.value,
                        onValueChange = { viewModel.nomeRota.value = it },
                        label = { Text("Nome da rota") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Escolha a cor da rota:", fontWeight = FontWeight.Medium)


                    val cores = listOf(
                        "#FF0066FF", "#FFFF0000", "#FF00FF00", "#FFFFA500", "#FF9C27B0",
                        "#FFFF5722", "#FF795548", "#FF607D8B", "#FF04F928", "#FFFFEB3B"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(cores.size) { i ->
                            val corHex = cores[i]
                            val cor = Color(android.graphics.Color.parseColor(corHex))
                            val isSelected = selectedColor == corHex

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(cor, CircleShape)
                                    .border(
                                        width = if (isSelected) 5.dp else 2.dp,
                                        color = if (isSelected) Color.White else Color(0xFFDDDDDD),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = corHex
                                        viewModel.corRota.value = corHex  // atualiza o ViewModel em tempo real
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Done,
                                        contentDescription = "Selecionada",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({
                    viewModel.corRota.value = selectedColor
                    viewModel.salvarRota(
                        onSuccess = { navController.popBackStack() },
                        onError = { /* snackbar opcional */ }
                    )
                    showSaveDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton({ showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (viewModel.isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF0066FF),
                strokeWidth = 6.dp
            )
        }
    }
}