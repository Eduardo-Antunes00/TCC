package com.example.tcc.telas

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.AuthState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // Mantém os campos e mensagens após girar a tela
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmarSenha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

    // 🔄 Só começa a verificar o e-mail após o cadastro ser concluído
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            while (true) {
                delay(3000)
                authViewModel.checkEmailVerification()
            }
        }
    }

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
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Crie sua conta", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { novoTexto ->
                        // Converte tudo para minúscula automaticamente
                        email = novoTexto.lowercase()
                    },
                    label = { Text("E-mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,           // teclado de e-mail
                        autoCorrect = false,
                        capitalization = KeyboardCapitalization.None // ← impede maiúsculas
                    ),
                    visualTransformation = VisualTransformation { text ->
                        // Garante que mesmo colando texto, ele fique em minúsculo
                        TransformedText(AnnotatedString(text.text.lowercase()), OffsetMapping.Identity)
                    },
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
                    label = { Text("Confirmar senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    when {
                        nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() -> {
                            mensagem = "Preencha todos os campos."
                        }
                        senha != confirmarSenha -> {
                            mensagem = "As senhas não coincidem."
                        }
                        else -> {
                            mensagem = ""
                            authViewModel.register(email, senha, nome)
                        }
                    }
                }) {
                    Text("Cadastrar")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Já tem conta? Faça login")
                }

                // Reage às mudanças de estado de autenticação
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Loading -> mensagem = "Cadastrando..."
                        is AuthState.Success -> mensagem =
                            "Cadastro realizado! Verifique seu e-mail para continuar."
                        is AuthState.EmailVerified -> {
                            mensagem = "E-mail verificado! Redirecionando..."
                            delay(2000)
                            // 👇 não remove a tela de registro da pilha, permite voltar
                            navController.navigate("login")
                            authViewModel.resetAuthState()
                        }
                        is AuthState.Error -> mensagem =
                            (authState as AuthState.Error).message
                        else -> {}
                    }
                }

                if (mensagem.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mensagem,
                        color = if (mensagem.contains("sucesso", true)
                            || mensagem.contains("verifique", true)
                            || mensagem.contains("verificado", true)
                        )
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
