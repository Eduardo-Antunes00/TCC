package com.example.tcc.telas

import androidx.compose.foundation.Image
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                    Text("Mobilidade Urbana", style = MaterialTheme.typography.headlineSmall, color = azulEscuro)
                }
                Divider(color = azulClaro.copy(alpha = 0.3f))

                NavigationDrawerItem(
                    label = { Text("Perfil", color = azulEscuro) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Pessoa", tint = azulPrincipal) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Ouvidoria", color = azulEscuro) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Telefone", tint = azulPrincipal) },
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
                    Text("Sair da conta", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Mobilidade Urbana",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, tint = Color.White, contentDescription = "Menu")
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
                        }
                    },
                    update = { map ->
                        map.overlays.removeAll { it is Polyline }
                        polylines.forEach { map.overlays.add(it) }
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
                            .padding(16.dp)
                            .heightIn(max = 300.dp) // não cresce demais
                    ) {
                        items(rotas) { rota ->
                            Button(
                                onClick = { navController.navigate("route/${rota.id}") },
                                colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(62.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.tcc.R.drawable.outline_bus_alert_24),
                                    contentDescription = "Ônibus",
                                    modifier = Modifier.size(28.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                                Spacer(Modifier.width(16.dp))
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