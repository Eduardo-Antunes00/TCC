package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.livedata.observeAsState
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val authState by authViewModel.authState.observeAsState(AuthState.Idle)
    var mensagem by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Login") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Faça seu login", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    // Validação local
                    when {
                        email.isEmpty() || senha.isEmpty() -> {
                            mensagem = "Por favor, preencha todos os campos."
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            mensagem = "Formato de e-mail inválido."
                        }
                        else -> {
                            mensagem = ""
                            authViewModel.login(email, senha)
                        }
                    }
                }) {
                    Text("Entrar")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { navController.navigate("register") }) {
                    Text("Não tem conta? Cadastre-se")
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Observa estado de autenticação
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Loading -> {
                            mensagem = "Carregando..."
                        }
                        is AuthState.Success -> {
                            // Navega para tela inicial ("home") e remove login da pilha
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true } // evita voltar para login
                            }
                        }
                        is AuthState.Error -> {
                            mensagem = (authState as AuthState.Error).message
                        }
                        else -> {}
                    }
                }


                if (mensagem.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mensagem,
                        color = if (mensagem.contains("sucesso")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
