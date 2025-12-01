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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.tcc.ui.theme.TCCTheme
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.MapViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    onResult: (String) -> Unit  // "login", "home" ou "homeAdm"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(true) }
    var hasInternet by remember { mutableStateOf<Boolean?>(null) }

    // CORES
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val azulClaro = Color(0xFF00D4FF)
    val vermelhoErro = Color(0xFFDD2C00)

    // Função principal que verifica tudo
    fun checkAuthAndRedirect() {
        scope.launch {
            isChecking = true
            delay(800)

            // 1. Verifica internet
            val connected = context.isInternetAvailable()
            hasInternet = connected

            if (!connected) {
                isChecking = false
                return@launch
            }

            // 2. Verifica se tem usuário logado e e-mail verificado
            val user = authViewModel.checkCurrentUser()
            if (user == null) {
                delay(600)
                onResult("login")
                return@launch
            }

            // 3. Busca o campo "acesso" no Firestore
            try {
                val uid = user.uid
                val doc = FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get()
                    .await()

                if (!doc.exists()) {
                    onResult("login")
                    return@launch
                }

                val acesso = doc.getLong("acesso") ?: 1L

                delay(600) // só pra splash ficar bonito

                onResult(
                    when (acesso) {
                        3L -> "homeAdm"
                        else -> "home"  // 1 ou qualquer outro valor = usuário comum
                    }
                )

            } catch (e: Exception) {
                // Se der erro ao buscar o documento (ex: sem internet temporária)
                onResult("login")
            } finally {
                isChecking = false
            }
        }
    }

    // Executa na primeira vez
    LaunchedEffect(Unit) {
        checkAuthAndRedirect()
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
            // Verificando conexão ou autenticando...
            isChecking || hasInternet == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = "Ônibus Universitário",
                        modifier = Modifier.size(140.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text("Onibo", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sem internet
            hasInternet == false -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = vermelhoErro, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Sem conexão com a internet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = azulEscuro,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Verifique sua conexão e tente novamente.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { checkAuthAndRedirect() },
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Tentar Novamente", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}