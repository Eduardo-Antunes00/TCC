package com.example.tcc.telas

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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Estados
    var rotas by remember { mutableStateOf<List<Route>>(emptyList()) }
    val polylines by mapViewModel.polylines.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val mapViewState = remember { mutableStateOf<MapView?>(null) }

    // Carregar rotas
    LaunchedEffect(Unit) {
        mapViewModel.carregarTrajetos()
        rotas = pegarRotas()
    }

    // Menu lateral (Drawer)
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // ← AQUI: só ativa gestos quando aberto
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF5F5F5),
                drawerTonalElevation = 8.dp
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                )

                // === ITENS DO MENU ===
                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("perfil") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Ouvidoria") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Implementar tela de ouvidoria
                    },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Ouvidoria") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))

                // Botão Sair
                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Sair", color = Color.Red)
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mobilidade Urbana") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
                            }
                        },
                        modifier = Modifier.height(80.dp),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    BoxWithConstraints {
                        val mapHeight = maxHeight * 6f / 16f

                        // === MAPA ===
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    setTilesScaledToDpi(true)
                                    controller.setZoom(14.0)
                                    controller.setCenter(GeoPoint(-29.7596, -57.0857))
                                    mapViewState.value = this
                                }
                            },
                            update = { map ->
                                mapViewState.value = map
                                map.overlays.removeAll { it is Polyline }
                                polylines.forEach { map.overlays.add(it) }
                                map.invalidate()
                            },
                            onRelease = { map ->
                                map.overlays.clear()
                                map.onDetach()
                                mapViewState.value = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(mapHeight)
                                .align(Alignment.TopCenter)
                                .clipToBounds()
                        )

                        // === LOADING ===
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        // === LISTA DE ROTAS (parte inferior) ===
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = maxHeight * 0.2f),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(200.dp)
                                    .background(Color.White.copy(alpha = 0.9f))
                                    .clip(MaterialTheme.shapes.medium)
                                    .padding(8.dp)
                            ) {
                                items(rotas) { rota ->
                                    Button(
                                        onClick = {
                                            navController.navigate("route/${rota.id}") {
                                                popUpTo(navController.graph.findStartDestination().id)
                                                launchSingleTop = true
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(rota.nome, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
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
