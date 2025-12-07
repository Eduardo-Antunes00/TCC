package com.example.tcc.telas

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.R
import com.example.tcc.database.model.OnibusInfo
import com.example.tcc.database.model.ParadaComId
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.RouteViewModel
import kotlinx.coroutines.delay
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
    var onibusList by remember { mutableStateOf<List<OnibusInfo>>(emptyList()) }
    var selectedItem by remember { mutableStateOf("Selecione uma Parada/Ônibus") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }
    var showHorarios by remember { mutableStateOf(false) }

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    LaunchedEffect(routeId) {
        while (true) {
            isLoading = true
            try {
                route = routeViewModel.pegarRotaPorId(routeId)
                paradas = routeViewModel.pegarParadasDaRota(routeId)
                onibusList = routeViewModel.pegarOnibusDaRota(routeId)
                if (route == null) error = true
            } catch (e: Exception) {
                error = true
            } finally {
                isLoading = false
            }
            delay(8000)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = route?.nome ?: "Linha",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showHorarios = !showHorarios }) {
                        Icon(
                            painter = painterResource(R.drawable.outline_calendar_clock_24),
                            contentDescription = "Ver horários",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = Color(0xFFF0F7FF)
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {

            Column(modifier = Modifier.fillMaxSize()) {

                // === MAPA - 80% DA TELA ===
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
                                setTilesScaledToDpi(true)
                                controller.setZoom(14.0)
                                setBuiltInZoomControls(false)

                                setOnTouchListener { _, event ->
                                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                                        selectedItem = "Selecione uma Parada/Ônibus"
                                    }
                                    false
                                }
                            }
                        },
                        update = { map ->
                            map.overlays.clear()

                            route?.let { r ->
                                val pontosList = r.pontos
                                val isFechada = pontosList.size >= 3 && pontosList.first() == pontosList.last()
                                val pontos = if (isFechada) pontosList.dropLast(1) else pontosList

                                // Linha de fundo (preta)
                                val backgroundLine = Polyline().apply {
                                    outlinePaint.color = android.graphics.Color.BLACK
                                    outlinePaint.strokeWidth = 13f
                                    outlinePaint.isAntiAlias = true
                                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                    setPoints(pontosList.toMutableList().apply { if (size >= 2 && last() != first()) add(first()) })
                                }

                                // Linha colorida da rota
                                val foregroundLine = Polyline().apply {
                                    outlinePaint.color = android.graphics.Color.parseColor(r.cor)
                                    outlinePaint.strokeWidth = 9f
                                    outlinePaint.isAntiAlias = true
                                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                    setPoints(pontosList.toMutableList().apply { if (size >= 2 && last() != first()) add(first()) })
                                }

                                map.overlays.add(backgroundLine)
                                map.overlays.add(foregroundLine)

                                // === SETAS PRETAS (só a ponta, perfeitas) ===
                                for (i in pontos.indices) {
                                    val start = pontos[i]
                                    val end = pontos[(i + 1) % pontos.size]

                                    val distancia = routeViewModel.distanciaEntrePontos(start, end)
                                    if (distancia < 80) continue

                                    val passos = maxOf(1, (distancia / 100.0).toInt())

                                    repeat(passos) { passo ->
                                        val t = (passo + 0.5) / passos.toDouble()
                                        val pontoAtual = GeoPoint(
                                            start.latitude + t * (end.latitude - start.latitude),
                                            start.longitude + t * (end.longitude - start.longitude)
                                        )

                                        val bearing = routeViewModel.bearingCorreto(start, end)

                                        map.overlays.add(Marker(map).apply {
                                            position = pontoAtual
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                            icon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_forward_ios_24)?.apply {
                                                setTint(android.graphics.Color.BLACK)
                                                setBounds(-13, -13, 13, 13) // tamanho ímpar = centralização perfeita
                                            }
                                            rotation = bearing.toFloat()
                                        })
                                    }
                                }

                                if (firstLoad) {
                                    foregroundLine.bounds?.let { bounds ->
                                        map.zoomToBoundingBox(bounds, true, 100)
                                        map.controller.setCenter(bounds.centerWithDateLine)
                                    }
                                    firstLoad = false
                                }
                            }

                            // === PARADAS ===
                            paradas.forEach { parada ->
                                Marker(map).apply {
                                    position = parada.ponto
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = ContextCompat.getDrawable(context, R.drawable.baseline_place_24)?.apply {
                                        setTint(android.graphics.Color.parseColor("#FF1744"))
                                    }
                                    title = "Parada ${parada.id}"
                                    setOnMarkerClickListener { _, _ ->
                                        val tempoEstimado = routeViewModel.calcularTempoParaParada(parada.ponto, onibusList, route?.pontos ?: emptyList())
                                        selectedItem = "Parada ${parada.id}\nTempo estimado: $tempoEstimado min"
                                        map.controller.animateTo(parada.ponto, 17.0, 600L)
                                        true
                                    }
                                    map.overlays.add(this)
                                }
                            }

                            // === ÔNIBUS ===
                            onibusList.forEach { onibus ->
                                var position = onibus.position
                                val distToLine = routeViewModel.distanciaPontoALinha(position, route?.pontos ?: emptyList())
                                if (distToLine <= 50.0) {
                                    position = routeViewModel.pontoMaisProximoNaLinha(position, route?.pontos ?: emptyList()) ?: position
                                }

                                val corInt = when (onibus.status) {
                                    "Em operação"     -> 0xFF0066FF.toInt()
                                    "Parado"          -> 0xFFD50000.toInt()
                                    "Manutenção"      -> 0xFF9C27B0.toInt()
                                    "Fora de Serviço" -> 0xFFFF9800.toInt()
                                    else              -> 0xFF757575.toInt()
                                }

                                val tamanhoPx = 32
                                val bitmap = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                val drawable = ContextCompat.getDrawable(context, R.drawable.outline_bus_alert_24)!!
                                    .mutate()
                                drawable.setTint(corInt)
                                drawable.setBounds(0, 0, tamanhoPx, tamanhoPx)
                                drawable.draw(canvas)

                                Marker(map).apply {
                                    position = position
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Ônibus - ${onibus.status}"
                                    icon = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                                    setOnMarkerClickListener { _, _ ->
                                        selectedItem = "Ônibus: ${onibus.status}"
                                        map.controller.animateTo(position, 17.0, 600L)
                                        true
                                    }
                                    map.overlays.add(this)
                                }
                            }

                            map.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // === PARTE DE BAIXO - 20% (BRANCO PURO) ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f)
                        .background(Color.White)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.outline_bus_alert_24),
                            contentDescription = "Ônibus",
                            modifier = Modifier.size(56.dp),
                            colorFilter = ColorFilter.tint(azulPrincipal)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = selectedItem,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = azulPrincipal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // === CARD DE HORÁRIOS NA PARTE DE BAIXO (FLUTUANTE) ===
            AnimatedVisibility(
                visible = showHorarios,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.outline_calendar_clock_24),
                                contentDescription = null,
                                tint = azulPrincipal,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Horários da Rota",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = azulEscuro
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showHorarios = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(0.5f))

                        val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                        val horariosMap = route?.horarios ?: emptyMap()

                        if (horariosMap.isEmpty()) {
                            Text(
                                "Nenhum horário cadastrado",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            ) {
                                items(dias) { dia ->
                                    val horario = horariosMap[dia]
                                    if (!horario.isNullOrBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(dia, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                                            Text(horario, color = azulEscuro, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                        }
                                    }
                                }

                                val fechado = dias.filter { horariosMap[it].isNullOrBlank() }
                                if (fechado.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Fechado: ${fechado.joinToString(", ")}",
                                            color = Color.Red.copy(0.9f),
                                            fontSize = 15.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // === LOADING & ERRO ===
        if (isLoading && route == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp)
            }
        }

        if (error) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Clear, tint = Color.Red, modifier = Modifier.size(80.dp), contentDescription = null)
                    Spacer(Modifier.height(24.dp))
                    Text("Rota não encontrada", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
                }
            }
        }
    }
}