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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    var selectedItem by remember { mutableStateOf("Toque em uma parada ou ônibus") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }
    var showHorarios by remember { mutableStateOf(false) }
    var paradaSelecionada by remember { mutableStateOf<Int?>(null) }

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val fundoMapa = Color(0xFFF5F9FF)

    LaunchedEffect(paradaSelecionada) {
        if (paradaSelecionada == null) return@LaunchedEffect

        // Encontra a parada selecionada
        val parada = paradas.find { it.id == paradaSelecionada } ?: return@LaunchedEffect

        // Atualização imediata (primeira vez)
        val tempoEstimado = routeViewModel.calcularTempoParaParada(
            parada.ponto,
            onibusList,
            route?.pontos ?: emptyList()
        )
        selectedItem = "Parada ${parada.id}\nTempo estimado: $tempoEstimado"

        // Depois, atualiza a cada 3 segundos enquanto estiver selecionada
        while (true) {
            delay(3000L) // 3 segundos

            // Recalcula com os dados mais recentes (onibusList pode ter mudado no loop principal)
            val novoTempo = routeViewModel.calcularTempoParaParada(
                parada.ponto,
                onibusList,
                route?.pontos ?: emptyList()
            )
            selectedItem = "Parada ${parada.id}\nTempo estimado: $novoTempo"
        }
    }
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
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = route?.nome ?: "Carregando...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp
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
                            contentDescription = "Horários",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = fundoMapa
    ) { padding ->

        Box(modifier = Modifier.padding(padding)) {

            Column(modifier = Modifier.fillMaxSize()) {

                // === MAPA COM CANTOS ARREDONDADOS E SOMBRA BONITA ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                setTilesScaledToDpi(true)
                                controller.setZoom(14.0)
                                setBuiltInZoomControls(false)

                                // CAPTURA TOQUE FORA DAS PARADAS → DESMARCA TUDO
                                setOnTouchListener { _, event ->
                                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                                        selectedItem = "Selecione uma Parada/Ônibus"
                                        paradaSelecionada = null
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

                                // === LINHAS ===
                                val backgroundLine = Polyline().apply {
                                    outlinePaint.apply {
                                        color = android.graphics.Color.BLACK
                                        strokeWidth = 13f
                                        isAntiAlias = true
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                    }
                                    setPoints(pontosList.toMutableList().apply {
                                        if (size >= 2 && last() != first()) add(first())
                                    })
                                }

                                val foregroundLine = Polyline().apply {
                                    outlinePaint.apply {
                                        color = android.graphics.Color.parseColor(r.cor)
                                        strokeWidth = 9f
                                        isAntiAlias = true
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                    }
                                    setPoints(pontosList.toMutableList().apply {
                                        if (size >= 2 && last() != first()) add(first())
                                    })
                                }

                                map.overlays.add(backgroundLine)
                                map.overlays.add(foregroundLine)

                                // === SETAS (100% NÃO CLICÁVEIS) ===
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
                                                setBounds(-13, -13, 13, 13)
                                            }
                                            rotation = bearing.toFloat()

                                            setOnMarkerClickListener { _, _ -> true } // retorna true = "consumiu o clique"

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


                            paradas.forEach { parada ->
                                val isSelecionada = paradaSelecionada == parada.id

                                // Ícone da parada (maior quando selecionada + cor diferente)
                                Marker(map).apply {
                                    position = parada.ponto
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                    val tamanhoPx =
                                        if (isSelecionada) 72 else 52  // ainda maior quando selecionada
                                    val corParada = if (isSelecionada)
                                        android.graphics.Color.parseColor("#B71C1C") // vermelho escuro
                                    else
                                        android.graphics.Color.parseColor("#FF1744") // vermelho vivo

                                    val bitmap = Bitmap.createBitmap(
                                        tamanhoPx,
                                        tamanhoPx,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bitmap)
                                    val drawable = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.baseline_place_24
                                    )!!
                                        .mutate()
                                        .apply {
                                            setTint(corParada)
                                            setBounds(0, 0, tamanhoPx, tamanhoPx)
                                            draw(canvas)
                                        }

                                    icon = android.graphics.drawable.BitmapDrawable(
                                        context.resources,
                                        bitmap
                                    )

                                    // === AQUI ESTÁ A MÁGICA: REMOVE TOTALMENTE QUALQUER COMPORTAMENTO DE INFO WINDOW ===
                                    title = null
                                    snippet = null
                                    setInfoWindow(null) // desativa o balão
                                    isDraggable = false

                                    // Este listener impede o comportamento padrão de clique do osmdroid
                                    setOnMarkerClickListener { marker, mapView ->
                                        // Seu código de clique
                                        val tempoEstimado = routeViewModel.calcularTempoParaParada(
                                            parada.ponto, onibusList, route?.pontos ?: emptyList()
                                        )
                                        selectedItem =
                                            "Parada ${parada.id}\nTempo estimado: $tempoEstimado"
                                        paradaSelecionada = parada.id
                                        mapView.controller.animateTo(parada.ponto)

                                        // Muito importante: retornar true = "eu consumi o clique"
                                        // retornar false = deixa o osmdroid fazer o comportamento padrão (mostrar balão!)
                                        true
                                    }

                                    map.overlays.add(this)
                                }
                            }

                            // === ÔNIBUS (mesmo comportamento) ===
                            // ÔNIBUS (CORRIGIDO E FUNCIONANDO 100%)
                            onibusList.forEach { onibus ->
                                // Garante que temos os pontos da rota antes de projetar
                                val pontosDaRota = route?.pontos
                                if (pontosDaRota.isNullOrEmpty()) return@forEach

                                var position = onibus.position

                                // CORREÇÃO 1: ordem correta dos parâmetros!
                                val distToLine = routeViewModel.distanciaPontoALinha(position, pontosDaRota)
                                if (distToLine <= 50.0) {
                                    // CORREÇÃO 2: ordem correta aqui também
                                    position = routeViewModel.pontoMaisProximoNaLinha(position, pontosDaRota) ?: position
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
                                    this.position = position
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Ônibus - ${onibus.status}"
                                    icon = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)

                                    setOnMarkerClickListener { _, _ ->
                                        selectedItem = "Ônibus: ${onibus.status}"
                                        map.controller.animateTo(position)
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

                // === BARRA INFERIOR ESTILOSA ===
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.outline_bus_alert_24),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            colorFilter = ColorFilter.tint(azulPrincipal)
                        )
                        Spacer(Modifier.width(20.dp))
                        Text(
                            text = selectedItem,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = azulEscuro,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === CARD DE HORÁRIOS (FLUTUANTE E LINDO) ===
            AnimatedVisibility(
                visible = showHorarios,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(32.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.outline_calendar_clock_24), null, tint = azulPrincipal, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Horários da Linha", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = azulEscuro)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showHorarios = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(0.5f))

                        val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                        val horarios = route?.horarios ?: emptyMap()

                        if (horarios.isEmpty()) {
                            Text("Sem horários cadastrados", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(32.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(dias) { dia ->
                                    horarios[dia]?.takeIf { it.isNotBlank() }?.let { horario ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(dia, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                                            Text(horario, color = azulEscuro, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                        }
                                    }
                                }
                                val fechados = dias.filter { horarios[it].isNullOrBlank() }
                                if (fechados.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text("Fechado: ${fechados.joinToString(", ")}", color = Color.Red.copy(0.8f), fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Loading & Erro
            if (isLoading && route == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp)
                }
            }

            if (error) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Clear, tint = Color.Red, modifier = Modifier.size(90.dp), contentDescription = null)
                        Spacer(Modifier.height(24.dp))
                        Text("Rota não encontrada", color = Color.Red, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)) {
                            Text("Voltar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}