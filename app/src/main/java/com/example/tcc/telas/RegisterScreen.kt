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
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
import com.example.tcc.viewmodels.AuthState
import com.example.tcc.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

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
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Crie sua conta", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmarSenha,
                    onValueChange = { confirmarSenha = it },
                    label = { Text("Confirmar senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        when {
                            nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() ->
                                mensagem = "Por favor, preencha todos os campos."
                            senha != confirmarSenha ->
                                mensagem = "As senhas não coincidem."
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                mensagem = "Formato de e-mail inválido."
                            else -> {
                                mensagem = ""
                                authViewModel.register(email, senha, nome)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cadastrar")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Já tem conta? Faça login")
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Observa o estado de autenticação
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Loading -> mensagem = "Cadastrando..."
                        is AuthState.Success -> {
                            mensagem = "Cadastro realizado com sucesso! Verifique seu e-mail."
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                        is AuthState.Error -> mensagem = (authState as AuthState.Error).message
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
