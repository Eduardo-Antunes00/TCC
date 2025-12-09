package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.AuthState
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    fromAdmin: Boolean = false
) {
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val fundo = Color(0xFFF0F7FF)
    val sucessoCor = Color(0xFF4CAF50)
    val erroCor = Color(0xFFE53935)

    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmarSenha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)
    LaunchedEffect(fromAdmin) {
        nome = ""
        email = ""
        senha = ""
        confirmarSenha = ""
        mensagem = ""
        isLoading = false
        authViewModel.resetAuthState() // importante!
    }
    // Só redireciona quando o e-mail for VERIFICADO
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> {
                isLoading = true
                mensagem = "Cadastrando..."
            }

            is AuthState.Success -> {
                isLoading = false
                mensagem = "Cadastro realizado com sucesso!\n\nVerifique seu e-mail para ativar a conta."
                // NÃO redireciona aqui!
            }

            is AuthState.EmailVerified -> {
                mensagem = "E-mail verificado!\nRedirecionando..."
                delay(1500)
                if (fromAdmin) {
                    navController.navigate("usersAdm") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                authViewModel.resetAuthState()
            }

            is AuthState.Error -> {
                isLoading = false
                mensagem = (authState as AuthState.Error).message
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (fromAdmin) "Novo Usuário" else "Criar Conta",
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (fromAdmin) {
                            navController.navigate("usersAdm") { popUpTo("usersAdm") }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = fundo
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = if (fromAdmin) "Cadastrar novo usuário" else "Crie sua conta",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.lowercase().trim() },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        label = { Text("Confirmar senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Spacer (Modifier.height(28.dp))

                            Button(
                            onClick = {
                                when {
                                    nome.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank() ->
                                        mensagem = "Preencha todos os campos"

                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                        mensagem = "E-mail inválido"

                                    senha.length < 6 ->
                                        mensagem = "A senha deve ter pelo menos 6 caracteres"

                                    senha != confirmarSenha ->
                                        mensagem = "As senhas não coincidem"

                                    else -> {
                                        mensagem = ""
                                        authViewModel.register(email, senha, nome)
                                    }
                                }
                            },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = if (fromAdmin) "Criar Usuário" else "Cadastrar",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!fromAdmin) {
                        TextButton(onClick = { navController.navigate("login") }) {
                            Text("Já tem conta? Faça login", color = azulPrincipal)
                        }
                    }

                    // Mensagem de status
                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = mensagem,
                            color = if (mensagem.contains("sucesso", true) || mensagem.contains("verifique", true))
                                sucessoCor else erroCor,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Se já cadastrou e está esperando verificação
                        if (authState is AuthState.Success || authState is AuthState.EmailVerified) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    authViewModel.checkEmailVerification()
                                    mensagem = "Verificando e-mail..."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = azulEscuro)
                            ) {
                                Text("Já verifiquei meu e-mail", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Cores dos campos (Material3 atualizado 2025)
@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Color.Black,
    focusedIndicatorColor = Color.Black,
    unfocusedIndicatorColor = Color.Black,
    focusedLabelColor = Color.Black,
    unfocusedLabelColor = Color.Black.copy(0.7f)
)