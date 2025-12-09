package com.example.tcc.telas_adm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var showAddDialog by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showActionDialog by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var modoMover by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        viewModel.init(routeId)
    }

    var selectedColor by remember { mutableStateOf(viewModel.corRota.value) }
    LaunchedEffect(viewModel.corRota.value) { selectedColor = viewModel.corRota.value }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = if (viewModel.isNewRoute.value) "Nova Rota" else "Editar Rota",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigate("homeAdm") }) {
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

        Box(modifier = Modifier.fillMaxWidth().weight(0.8f).clipToBounds()) {
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
                                    val (tipo, index) = modoMover!!
                                    if (tipo == "parada") {
                                        val sucesso = viewModel.tentarMoverParada(index, p)
                                        if (!sucesso) {
                                            Toast.makeText(context, "Parada muito longe da rota!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.moverPonto(index, p)
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
                    val eventOverlay = map.overlays.find { it is MapEventsOverlay } as? MapEventsOverlay
                    map.overlays.clear()
                    eventOverlay?.let { map.overlays.add(it) }

                    // === LINHA DA ROTA ===
                    if (viewModel.pontos.isNotEmpty()) {
                        val pontosRota = viewModel.pontos.map { it.ponto }
                        val linhaCompleta = pontosRota.toMutableList().apply {
                            if (size >= 2 && last() != first()) add(first())
                        }

                        map.overlays.add(Polyline().apply {
                            outlinePaint.color = android.graphics.Color.parseColor(viewModel.corRota.value)
                            outlinePaint.strokeWidth = 14f
                            setPoints(linhaCompleta)
                        })

                        // === SETAS PERFEITAS (só a ponta, sem haste, centralizadas) ===
                        if (viewModel.pontos.size >= 2) {
                            val pontosList = viewModel.pontos.map { it.ponto }
                            val isFechada = pontosList.size >= 3 && pontosList.first() == pontosList.last()
                            val pontos = if (isFechada) pontosList.dropLast(1) else pontosList

                            for (i in pontos.indices) {
                                val start = pontos[i]
                                val end = pontos[(i + 1) % pontos.size]

                                val distancia = viewModel.distanciaEntrePontos(start, end)
                                if (distancia < 70) continue

                                val passos = maxOf(1, (distancia / 90.0).toInt())

                                repeat(passos) { passo ->
                                    val t = (passo + 0.5) / passos.toDouble()
                                    val pontoAtual = GeoPoint(
                                        start.latitude + t * (end.latitude - start.latitude),
                                        start.longitude + t * (end.longitude - start.longitude)
                                    )

                                    val bearing = viewModel.bearingCorreto(start, end)

                                    map.overlays.add(Marker(map).apply {
                                        position = pontoAtual
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        icon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_forward_ios_24)?.apply {
                                            setTint(android.graphics.Color.BLACK)
                                            setBounds(-13, -13, 13, 13)
                                        }
                                        rotation = bearing.toFloat()
                                    })
                                }
                            }
                        }
                    }

                    // === PONTOS DA ROTA ===
                    viewModel.pontos.forEachIndexed { index, p ->
                        val isFechamento = viewModel.pontos.size >= 2 &&
                                index == viewModel.pontos.lastIndex &&
                                p.ponto == viewModel.pontos.first().ponto

                        if (!isFechamento) {
                            map.overlays.add(Marker(map).apply {
                                position = p.ponto
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                        val pontoNaLinha = viewModel.pontoMaisProximoNaLinha(parada.ponto)
                        pontoNaLinha?.let { ponto ->
                            map.overlays.add(Polyline().apply {
                                outlinePaint.color = android.graphics.Color.BLACK
                                outlinePaint.strokeWidth = 6f
                                outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                                setPoints(listOf(parada.ponto, ponto))
                            })
                        }

                        map.overlays.add(Marker(map).apply {
                            position = parada.ponto
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

        // Barra inferior de instruções
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

    // === DIÁLOGO: ADICIONAR PONTO OU PARADA ===
    if (showAddDialog && clickedPoint != null) {
        val pertoDaLinha = viewModel.distanciaPontoALinha(clickedPoint!!) <= 50.0

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar", fontWeight = FontWeight.Bold) },
            text = { Text("O que deseja adicionar neste local?", textAlign = TextAlign.Center) },
            confirmButton = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.adicionarPonto(clickedPoint!!)
                            showAddDialog = false
                            clickedPoint = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    ) {
                        Text("Ponto da Rota", color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    if (pertoDaLinha) {
                        Button(
                            onClick = {
                                viewModel.adicionarParada(clickedPoint!!)
                                showAddDialog = false
                                clickedPoint = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Parada", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Text(
                            text = "Só é possível adicionar paradas próximas à rota",
                            color = Color.Red.copy(alpha = 0.9f),
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.3f))

                    TextButton(
                        onClick = {
                            showAddDialog = false
                            clickedPoint = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = {},
            shape = RoundedCornerShape(16.dp)
        )
    }

    // === DIÁLOGO: AÇÕES (MOVER/EXCLUIR) ===
    showActionDialog?.let { (tipo, index) ->
        val id = if (tipo == "ponto") viewModel.pontos.getOrNull(index)?.id
        else viewModel.paradas.getOrNull(index)?.id ?: "?"

        AlertDialog(
            onDismissRequest = { showActionDialog = null },
            title = { Text("${if (tipo == "ponto") "Ponto" else "Parada"} $id", fontWeight = FontWeight.Bold) },
            text = { Text("O que deseja fazer?") },
            confirmButton = {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            modoMover = Pair(tipo, index)
                            showActionDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    ) {
                        Text("Mover", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (tipo == "ponto") viewModel.removerPonto(index)
                            else viewModel.removerParada(index)
                            showActionDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Excluir", color = Color.White)
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.3f))

                    TextButton(
                        onClick = { showActionDialog = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = {},
            shape = RoundedCornerShape(16.dp)
        )
    }

    // === DIÁLOGO: SALVAR ROTA (com nome, cor e horários) ===
    if (showSaveDialog) {
        val diasDaSemana = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        val cores = listOf(
            "#FF8ABDFF",  // azul
            "#FFFF6B6B",  // vermelho
            "#FF77DD77",  // verde
            "#FFFFD27D",  // laranja/amarelo
            "#FFD7A8FF",  // roxo
            "#FFFF8A65",  // laranja queimado
            "#FFB89F8B",  // marrom claro
            "#FFA8C8D8"   // azul acinzentado
        )

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Salvar Rota", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = viewModel.nomeRota.value,
                            onValueChange = { viewModel.nomeRota.value = it },
                            label = { Text("Nome da rota") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { Text("Horários de funcionamento:", fontWeight = FontWeight.Medium) }

                    items(diasDaSemana) { dia ->
                        OutlinedTextField(
                            value = viewModel.horarios[dia] ?: "",
                            onValueChange = { viewModel.horarios[dia] = it },
                            label = { Text(dia) },
                            placeholder = { Text("ex: 08:00-22:00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    item { Text("Cor da linha:", fontWeight = FontWeight.Medium) }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
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
                                    if (isSelected) {
                                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
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
                }) {
                    Text("Salvar", color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton({ showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // === LOADING ===
    if (viewModel.isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF0066FF), strokeWidth = 6.dp)
        }
    }
}