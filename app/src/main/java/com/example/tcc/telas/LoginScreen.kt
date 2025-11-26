package com.example.tcc.telas

import androidx.compose.runtime.saveable.rememberSaveable
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
    // Resetar o estado de autenticação ao abrir a tela
    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    // 🔒 Mantém os dados mesmo se girar a tela
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> mensagem = "Carregando..."
            is AuthState.Success -> {
                mensagem = "Login bem-sucedido!"
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                val error = authState as AuthState.Error
                mensagem = "As credenciais estão incorretas."
            }
            else -> {}
        }
    }
    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }
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

                // 🔁 Observa e reage ao estado da autenticação


                if (mensagem.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mensagem,
                        color = if (mensagem.contains("sucesso", true))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

