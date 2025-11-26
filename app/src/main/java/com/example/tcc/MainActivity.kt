package com.example.tcc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.tcc.ui.theme.TCCTheme
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.MapViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val authViewModel by lazy { AuthViewModel() }
    private val mapViewModel by lazy { MapViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TCCTheme {
                val navController = rememberNavController()

                // ESSA LINHA É A MÁGICA: salva mesmo se o processo morrer
                var initialRoute by rememberSaveable { mutableStateOf<String?>(null) }

                // Só mostra splash na primeira vez ou quando ainda não decidiu
                if (initialRoute == null) {
                    SplashWithPersistentAuth(
                        authViewModel = authViewModel,  // ← PASSA O VIEWMODEL AQUI
                        onResult = { route ->
                            initialRoute = route
                        }
                    )
                } else {
                    // Depois que decidiu, vai direto pra tela certa
                    AppNavigation(
                        navController = navController,
                        authViewModel = authViewModel,
                        mapViewModel = mapViewModel,
                        startDestination = initialRoute!!
                    )
                }
            }
        }
    }
}

@Composable
fun SplashWithPersistentAuth(
    authViewModel: AuthViewModel,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado para controlar o que mostrar
    var isChecking by remember { mutableStateOf(true) }
    var hasInternet by remember { mutableStateOf<Boolean?>(null) } // null = ainda verificando

    // CORES
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val azulClaro = Color(0xFF00D4FF)
    val vermelhoErro = Color(0xFFDD2C00)

    // Função para verificar internet
    fun checkInternetAndProceed() {
        scope.launch {
            isChecking = true
            delay(800) // pequena animação inicial

            val connected = context.isInternetAvailable()
            hasInternet = connected

            if (connected) {
                delay(600) // só pra manter o splash bonito
                val isLoggedIn = authViewModel.verifyCurrentUser()
                onResult(if (isLoggedIn) "home" else "login")
            }
            isChecking = false
        }
    }

    // Executa na primeira vez
    LaunchedEffect(Unit) {
        checkInternetAndProceed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F7FF))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 1. Verificando conexão...
            isChecking || hasInternet == null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = "Ônibus Universitário",
                        modifier = Modifier.size(140.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        "Mobilidade",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulPrincipal
                    )
                    Text(
                        "Urbana",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulClaro
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = azulPrincipal,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Verificando conexão...", color = azulEscuro)
                }
            }

            // 2. Sem internet → Tela de erro
            hasInternet == false -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = vermelhoErro,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Sem conexão com a internet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = azulEscuro,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Verifique sua conexão Wi-Fi ou dados móveis e tente novamente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { checkInternetAndProceed() },
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tentar Novamente", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}