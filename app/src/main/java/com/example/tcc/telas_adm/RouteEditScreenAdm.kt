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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.R
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

    // Estados de UI
    var showAddDialog by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showActionDialog by remember { mutableStateOf<Pair<String, Int>?>(null) } // "ponto" ou "parada", índice
    var modoMover by remember { mutableStateOf<Pair<String, Int>?>(null) }        // null = normal, senão = movendo
    var showSaveDialog by remember { mutableStateOf(false) }

    // Cor sincronizada
    var selectedColor by remember { mutableStateOf(viewModel.corRota.value) }
    LaunchedEffect(viewModel.corRota.value) { selectedColor = viewModel.corRota.value }

    // Inicializa configuração do osmdroid
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

        // === MAPA ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .clipToBounds()
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(-29.770881, -57.086261))

                        val receiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                clickedPoint = p

                                if (modoMover != null) {
                                    // Está no modo mover → reposiciona imediatamente
                                    val (tipo, index) = modoMover!!
                                    if (tipo == "ponto") {
                                        viewModel.moverPonto(index, p)
                                    } else {
                                        viewModel.moverParada(index, p)
                                    }
                                    modoMover = null
                                } else {
                                    showAddDialog = true
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        }
                        overlays.add(MapEventsOverlay(receiver))
                    }
                },
                update = { map ->
                    val eventOverlay = map.overlays.find { it is MapEventsOverlay }
                    map.overlays.clear()
                    eventOverlay?.let { map.overlays.add(it) }

                    // === LINHA DA ROTA ===
                    if (viewModel.pontos.isNotEmpty()) {
                        val linha = viewModel.pontos.map { it.ponto }.toMutableList()
                        if (linha.size >= 2) linha.add(linha.first())

                        map.overlays.add(Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor(viewModel.corRota.value)
                            outlinePaint.strokeWidth = 14f
                            setPoints(linha)
                        })
                    }

                    // === PONTOS DA ROTA ===
                    viewModel.pontos.forEachIndexed { index, p ->
                        val isFechamento = viewModel.pontos.size >= 2 &&
                                index == viewModel.pontos.size - 1 &&
                                p.ponto == viewModel.pontos.first().ponto

                        if (!isFechamento) {
                            map.overlays.add(Marker(map).apply {
                                position = p.ponto
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Ponto ${p.id}"
                                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)?.apply {
                                    setTint(android.graphics.Color.BLACK)
                                }
                                setOnMarkerClickListener { _, _ ->
                                    showActionDialog = Pair("ponto", index)
                                    true
                                }
                            })
                        }
                    }

                    // === PARADAS ===
                    viewModel.paradas.forEachIndexed { index, parada ->
                        map.overlays.add(Marker(map).apply {
                            position = parada.ponto
                            title = "Parada ${parada.id}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.baseline_place_24)?.apply {
                                setTint(android.graphics.Color.RED)
                            }
                            setOnMarkerClickListener { _, _ ->
                                showActionDialog = Pair("parada", index)
                                true
                            }
                        })
                    }

                    map.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // === MENSAGEM INFERIOR ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .background(if (modoMover != null) Color(0xFF0066FF) else Color(0xFFF5F5F5))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    modoMover != null -> {
                        val (tipo, index) = modoMover!!
                        val id = if (tipo == "ponto") viewModel.pontos.getOrNull(index)?.id
                        else viewModel.paradas.getOrNull(index)?.id
                        "Toque no mapa para reposicionar a ${if (tipo == "ponto") "Ponto" else "Parada"} $id"
                    }
                    else -> "Toque no mapa → adicionar ponto/parada\nToque em um marcador → mover ou excluir"
                },
                fontSize = 16.sp,
                color = if (modoMover != null) Color.White else Color(0xFF003366),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // === DIÁLOGO: Adicionar novo ponto/parada ===
    if (showAddDialog && clickedPoint != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar") },
            text = { Text("O que deseja adicionar neste local?") },
            confirmButton = {
                Row {
                    TextButton({
                        viewModel.adicionarPonto(clickedPoint!!)
                        showAddDialog = false
                        clickedPoint = null
                    }) { Text("Ponto da Rota") }
                    Spacer(Modifier.width(8.dp))
                    TextButton({
                        viewModel.adicionarParada(clickedPoint!!)
                        showAddDialog = false
                        clickedPoint = null
                    }) { Text("Parada") }
                }
            },
            dismissButton = {
                TextButton({ showAddDialog = false; clickedPoint = null }) { Text("Cancelar") }
            }
        )
    }

    // === DIÁLOGO: Ações (Mover ou Excluir) ===
    showActionDialog?.let { (tipo, index) ->
        val id = if (tipo == "ponto") viewModel.pontos.getOrNull(index)?.id
        else viewModel.paradas.getOrNull(index)?.id ?: "?"

        AlertDialog(
            onDismissRequest = { showActionDialog = null },
            title = { Text("${if (tipo == "ponto") "Ponto" else "Parada"} $id")},
            text = { Text("O que deseja fazer?") },
            confirmButton = {
                Column {
                    TextButton({
                        modoMover = Pair(tipo, index)
                        showActionDialog = null
                    }) {
                        Text("Mover", color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
                    }
                    TextButton({
                        if (tipo == "ponto") viewModel.removerPonto(index)
                        else viewModel.removerParada(index)
                        showActionDialog = null
                    }) {
                        Text("Excluir", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton({ showActionDialog = null }) { Text("Cancelar") }
            }
        )
    }

    // === DIÁLOGO: Salvar ===
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
                    Text("Cor da linha:", fontWeight = FontWeight.Medium)
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
                                        viewModel.corRota.value = corHex
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
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
                        onError = { }
                    )
                    showSaveDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton({ showSaveDialog = false }) { Text("Cancelar") } }
        )
    }

    // === LOADING ===
    if (viewModel.isLoading.value) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0066FF), strokeWidth = 6.dp)
        }
    }
}