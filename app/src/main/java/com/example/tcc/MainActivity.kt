package com.example.tcc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
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
    val scope = rememberCoroutineScope()

    // CORES AZUIS (defina aqui fora do LaunchedEffect)
    val azulPrincipal = androidx.compose.ui.graphics.Color(0xFF0066FF)
    val azulEscuro = androidx.compose.ui.graphics.Color(0xFF003366)
    val azulClaro = androidx.compose.ui.graphics.Color(0xFF00D4FF)

    // AQUI É O LUGAR CERTO PRA COLOCAR O UI!!!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.outline_bus_alert_24),
                contentDescription = "Ônibus Universitário",
                modifier = Modifier.size(140.dp),
                colorFilter = ColorFilter.tint(azulPrincipal)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Mobilidade",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = azulPrincipal
            )
            Text(
                text = "Urbana",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = azulClaro
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = azulPrincipal,
                strokeWidth = 6.dp,
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Carregando...",
                color = azulEscuro,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    // AQUI É SÓ O CÓDIGO DE VERIFICAÇÃO (deixa aqui embaixo!)
    LaunchedEffect(Unit) {
        scope.launch {
            delay(1400) // tempo do splash

            val isLoggedIn = authViewModel.verifyCurrentUser()
            onResult(if (isLoggedIn) "home" else "login")
        }
    }
}