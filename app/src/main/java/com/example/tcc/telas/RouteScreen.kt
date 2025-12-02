package com.example.tcc.telas


import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap


import android.graphics.Color.parseColor
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.ui.graphics.Canvas
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
import androidx.core.graphics.toColorInt

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

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    // Chave para forçar o zoom apenas na primeira carga
    var firstLoad by remember { mutableStateOf(true) }

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.8f

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    controller.setZoom(14.0)
                    setBuiltInZoomControls(false)

                    // Listener para clique no mapa (fora de marcadores)
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

                // ROTA
                route?.let { r: Route ->
                    val points = r.pontos.toMutableList()
                    if (points.size >= 2 && points.last() != points.first()) {
                        points.add(points.first())
                    }

                    // 1. LINHA DE FUNDO BRANCA (grossa e arredondada)
                    val backgroundLine = Polyline().apply {
                        outlinePaint.color = android.graphics.Color.BLACK
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.isAntiAlias = true
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        setPoints(points)
                    }

                    // 2. LINHA COLORIDA POR CIMA (fina e perfeita)
                    val foregroundLine = Polyline().apply {
                        outlinePaint.color = android.graphics.Color.parseColor(r.cor)
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.isAntiAlias = true
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        setPoints(points)
                    }

                    map.overlays.add(backgroundLine)
                    map.overlays.add(foregroundLine)

                    if (firstLoad) {
                        foregroundLine.bounds?.let { bounds ->
                            map.zoomToBoundingBox(bounds, true, 100)
                            map.controller.setCenter(bounds.centerWithDateLine)
                        }
                        firstLoad = false
                    }
                }

                // PARADAS
                paradas.forEach { parada ->
                    val marker = Marker(map).apply {
                        position = parada.ponto
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.baseline_place_24)?.apply {
                            setTint(parseColor("#FF1744"))
                        }
                        title = "Parada ${parada.id}"
                        setOnMarkerClickListener { _, _ ->
                            selectedItem = "Parada ${parada.id}"
                            map.controller.animateTo(parada.ponto, 17.0, 600L)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                // ÔNIBUS – ícone menor!
                // Substitua TODO o bloco dos ÔNIBUS por esse aqui:

                onibusList.forEach { onibus ->
                    val corInt = when (onibus.status) {
                        "Em operação"     -> 0xFF0066FF.toInt()
                        "Parado"          -> 0xFFD50000.toInt()
                        "Manutenção"      -> 0xFF9C27B0.toInt()
                        "Fora de Serviço" -> 0xFFFF9800.toInt()
                        else              -> 0xFF757575.toInt()
                    }

                    val tamanhoPx = 32

                    // CRIA O BITMAP EM BRANCO
                    val bitmap = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)

                    // USA O Canvas DO ANDROID (não do Compose!)
                    val canvas = android.graphics.Canvas(bitmap)

                    // Pega o drawable, aplica cor e desenha
                    val drawable = ContextCompat.getDrawable(context, R.drawable.outline_bus_alert_24)!!
                        .mutate()  // importante!

                    drawable.setTint(corInt)
                    drawable.setBounds(0, 0, tamanhoPx, tamanhoPx)
                    drawable.draw(canvas)  // ← aqui a cor é aplicada de verdade

                    val marker = Marker(map).apply {
                        position = onibus.position
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Ônibus - ${onibus.status}"
                        icon = BitmapDrawable(context.resources, bitmap)

                        setOnMarkerClickListener { _, _ ->
                            selectedItem = "Ônibus: ${onibus.status}"
                            map.controller.animateTo(onibus.position, 17.0, 600L)
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

        // LOADING / ERRO (mantidos iguais)
        if (isLoading && route == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp)
            }
        }

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

        // CARD NOME DA ROTA
        route?.let { r ->
            Card(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = r.nome, fontWeight = FontWeight.Bold, color = azulEscuro)
                }
            }
        }

        // INFO EMBAIXO – texto sempre azul (exceto quando for parada)
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
                contentDescription = "Ônibus",
                modifier = Modifier.size(56.dp),
                colorFilter = ColorFilter.tint(azulPrincipal)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = selectedItem,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedItem.contains("Parada")) Color(0xFF0066FF) else azulPrincipal, // só parada vermelha, tudo azul
                textAlign = TextAlign.Center
            )
        }
    }
}