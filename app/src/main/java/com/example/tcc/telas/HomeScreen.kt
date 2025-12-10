package com.example.tcc.telas

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.database.model.Route
import com.example.tcc.viewmodels.MapViewModel
import com.example.tcc.viewmodels.pegarRotas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var rotas by remember { mutableStateOf<List<Route>>(emptyList()) }
    val polylines by mapViewModel.polylines.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)
    val fundoDrawer = Color(0xFFF8FBFF)

    LaunchedEffect(Unit) {
        mapViewModel.carregarTrajetos()
        rotas = pegarRotas()
    }
//Menu
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = fundoDrawer, drawerTonalElevation = 12.dp) {
                Spacer(Modifier.height(32.dp))
                Column(modifier = Modifier.padding(24.dp)) {
                    Image(
                        painter = painterResource(id = com.example.tcc.R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ombo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = azulEscuro
                    )
                }
                Divider(color = azulClaro.copy(alpha = 0.3f))

                NavigationDrawerItem(
                    label = { Text("Perfil", color = azulEscuro) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        navController.navigate("profile") // Navega para a tela de perfil
                    },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Pessoa",
                            tint = azulPrincipal
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                val context = LocalContext.current
                NavigationDrawerItem(
                    label = { Text("Ouvidoria", color = azulEscuro) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }

                        // Agora o context foi declarado lá em cima, dentro do lado de fora
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://wa.me/555592475454")
                        }
                        context.startActivity(intent)
                    },
                    icon = {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Ouvidoria",
                            tint = azulPrincipal
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color.Red)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Sair da conta",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) {//Menu Flutuante
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Ombo",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                tint = Color.White,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
                )
            },
            containerColor = Color(0xFFF0F7FF)
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {

                // === MAPA EM TELA CHEIA ===
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            setTilesScaledToDpi(true)
                            controller.setZoom(13.7)
                            controller.setCenter(GeoPoint(-29.770881, -57.086261))
                            setBuiltInZoomControls(false)
                        }
                    },
                    update = { map ->
                        // Remove apenas as polylines antigas
                        map.overlays.removeAll { it is Polyline }

                        // Agora usa as ROTAS completas (que têm a cor!) para criar as linhas
                        rotas.forEach { rota ->
                            val points = rota.pontos  // assumindo que Route tem 'pontos: List<GeoPoint>'
                            if (points.size < 2) return@forEach

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
                                outlinePaint.color = android.graphics.Color.parseColor(rota.cor)  // ← AQUI USAMOS rota.cor!
                                outlinePaint.strokeWidth = 8f
                                outlinePaint.isAntiAlias = true
                                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                setPoints(points)
                            }

                            // Adiciona na ordem certa: fundo primeiro, cor depois
                            map.overlays.add(backgroundLine)
                            map.overlays.add(foregroundLine)
                        }

                        map.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // === LISTA DE ROTAS FLUTUANTE (SÓ NA PARTE DE BAIXO, NÃO COBRE O MAPA) ===
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 1.0f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(8.dp, 16.dp)
                            .heightIn(max = 200.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rotas) { rota ->
                            Button(
                                onClick = { navController.navigate("route/${rota.id}") },
                                colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                                shape = RoundedCornerShape(50.dp), // botão bem arredondado (cápsula)
                                contentPadding = PaddingValues(0.dp), // importante pra gente controlar o padding interno
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // FAIXA COLORIDA (lado esquerdo)
                                    Box(
                                        modifier = Modifier
                                            .width(15.dp)
                                            .fillMaxHeight()
                                            .background(
                                                color = Color(android.graphics.Color.parseColor(rota.cor)),
                                                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                                            )
                                    )

                                    // ÍCONE + TEXTO (com espaçamento perfeito)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = com.example.tcc.R.drawable.outline_bus_alert_24),
                                            contentDescription = "Ônibus",
                                            modifier = Modifier.size(28.dp),
                                            colorFilter = ColorFilter.tint(Color.White)
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Text(
                                            text = rota.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                }
                            }
                        }
                    }
                }


                // === LOADING ===
                if (isLoading) {
                    CircularProgressIndicator(
                        color = azulPrincipal,
                        modifier = Modifier.align(Alignment.Center)
                    )
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactCenteredTopAppBar(
    title: String,
    onMenuClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .statusBarsPadding()
        ) {
            // Ícone do menu (esquerda)
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
            }

            // TÍTULO CENTRALIZADO PERFEITO
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 72.dp) // ← espaço pros lados (nunca mais corta)
            )
        }
    }
}
