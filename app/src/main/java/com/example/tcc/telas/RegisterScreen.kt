package com.example.tcc.telas

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.AuthState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // CORES DO APP
    val azulPrincipal = Color(0xFF0066FF)
    val fundo = Color(0xFFF0F7FF)

    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmarSenha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

    // Observa mudanças no estado depois do cadastro
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> mensagem = "Cadastrando..."
            is AuthState.Success -> mensagem =
                "Cadastro realizado! Verifique seu e-mail."
            is AuthState.EmailVerified -> {
                mensagem = "E-mail verificado! Redirecionando..."
                delay(1500)
                navController.navigate("login")
                authViewModel.resetAuthState()
            }
            is AuthState.Error -> {
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
                        "Cadastro",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = azulPrincipal
                )
            )
        },
        containerColor = fundo
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Crie sua conta",
                        fontSize = 22.sp,
                        color = Color(0xFF003366)
                    )

                    Spacer(Modifier.height(20.dp))

                    // CAMPO NOME
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )

                    )

                    Spacer(Modifier.height(10.dp))

                    // CAMPO EMAIL — sempre minúsculo
                    OutlinedTextField(
                        value = email,
                        onValueChange = { novo ->
                            email = novo.lowercase()
                        },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false
                        ),
                        visualTransformation = VisualTransformation { text ->
                            TransformedText(
                                AnnotatedString(text.text.lowercase()),
                                OffsetMapping.Identity
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // SENHA
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // CONFIRMAR SENHA
                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        label = { Text("Confirmar senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    // BOTÃO PRINCIPAL
                    Button(
                        onClick = {
                            when {
                                nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() ->
                                    mensagem = "Preencha todos os campos."

                                senha != confirmarSenha ->
                                    mensagem = "As senhas não coincidem."

                                else -> {
                                    mensagem = ""
                                    authViewModel.register(email, senha, nome)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = azulPrincipal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cadastrar", color = Color.White)
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(
                        onClick = { navController.navigate("login") }
                    ) {
                        Text("Já tem conta? Faça login")
                    }

                    // MENSAGENS
                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            mensagem,
                            color = if (
                                mensagem.contains("sucesso", true) ||
                                mensagem.contains("verifique", true) ||
                                mensagem.contains("verificado", true)
                            ) azulPrincipal else Color.Red
                        )
                    }
                }
            }

        }
    }
}
