package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.viewmodels.AuthState
import com.example.tcc.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    val authState by authViewModel.authState.observeAsState(AuthState.Idle)
    var mensagem by remember { mutableStateOf("") }

    var stopChecking by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cadastro") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Crie sua conta", fontSize = 22.sp)
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmarSenha,
                    onValueChange = { confirmarSenha = it },
                    label = { Text("Confirmar Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    // Valida se todos os campos foram preenchidos
                    if (email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                        mensagem = "Por favor, preencha todos os campos."
                        stopChecking = true
                        return@Button
                    }

                    // Valida se as senhas coincidem
                    if (senha != confirmarSenha) {
                        mensagem = "As senhas não coincidem."
                        stopChecking = true
                        return@Button
                    }

                    // Se passou nas validações, registra
                    mensagem = ""
                    authViewModel.register(email, senha)
                    // stopChecking será alterado apenas no Success
                }) {
                    Text("Cadastrar")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Já tem conta? Fazer login")
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Observa o estado de autenticação
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Success -> {
                            mensagem = "Conta criada com sucesso! Verifique seu e-mail."
                            stopChecking = false // inicia loop de verificação
                        }

                        is AuthState.Error -> {
                            mensagem = (authState as AuthState.Error).message
                            stopChecking = true // garante que loop não rode
                        }

                        else -> {}
                    }
                }

                // Loop seguro de verificação de e-mail
                LaunchedEffect(stopChecking) {
                    if (!stopChecking) {
                        while (!stopChecking) {
                            delay(5000)

                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                user.reload().addOnCompleteListener {
                                    if (user.isEmailVerified) {
                                        stopChecking = true
                                        navController.navigate("login") {
                                            popUpTo("register") { inclusive = false }
                                        }
                                    }
                                }
                            } else {
                                stopChecking = true
                            }
                        }
                    }
                }

                // Mensagem de erro/sucesso
                if (mensagem.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mensagem,
                        color = if (mensagem.contains("Verifique seu e-mail"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
