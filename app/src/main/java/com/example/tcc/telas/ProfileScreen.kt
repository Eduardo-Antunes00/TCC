package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tcc.viewmodels.ProfileViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // === MESMAS CORES DA HOME ===
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro     = Color(0xFF00D4FF)
    val azulEscuro    = Color(0xFF003366)
    val fundoTela     = Color(0xFFF0F7FF)   // mesmo fundo da Home

    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val error       by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Perfil do Usuário",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = fundoTela   // mesmo fundo azul bem claro da Home
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = azulPrincipal)
                }

                error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Erro ao carregar perfil",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = error ?: "", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadUserProfile() },
                            colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                        ) {
                            Text("Tentar novamente", color = Color.White)
                        }
                    }
                }

                userProfile == null -> {
                    Text("Nenhum perfil encontrado", color = azulEscuro)
                }

                else -> {
                    val profile = userProfile!!

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // CARD BRANCO COM ELEVAÇÃO (igual ao da lista de rotas)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                ProfileItemRow(label = "Nome", value = profile.nome.orEmpty())
                                Divider(color = azulClaro.copy(alpha = 0.3f))
                                ProfileItemRow(label = "E-mail", value = profile.email.orEmpty())
                                Divider(color = azulClaro.copy(alpha = 0.3f))
                                ProfileItemRow(
                                    label = "Coordenada X",
                                    value = profile.cordx?.toString() ?: "Não definida"
                                )
                                Divider(color = azulClaro.copy(alpha = 0.3f))
                                ProfileItemRow(
                                    label = "Coordenada Y",
                                    value = profile.cordy?.toString() ?: "Não definida"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Botão atualizar com a mesma cor da Home
                        Button(
                            onClick = { viewModel.loadUserProfile() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Atualizar dados", color = Color.White, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// Item de perfil mais bonito e alinhado
@Composable
private fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 15.sp
        )
        Text(
            text = value,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}