package com.example.tcc.telas

import android.util.Patterns
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.AuthState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    LaunchedEffect(Unit) { authViewModel.resetAuthState() }

    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    // Estados do Dialog de recuperação
    var mostrarDialogRecuperarSenha by rememberSaveable { mutableStateOf(false) }
    var emailRecuperacao by rememberSaveable { mutableStateOf("") }
    var mensagemDialog by rememberSaveable { mutableStateOf("") }
    var mostrandoMensagemSucesso by rememberSaveable { mutableStateOf(false) }


    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

    val azulPrincipal = Color(0xFF0066FF)
    val fundoGeral = Color(0xFFF0F7FF)

    // Reação ao login
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> mensagem = "Carregando..."
            is AuthState.Success -> {
                val sucesso = authState as AuthState.Success
                mensagem = "Login bem-sucedido!"
                val destino = if (sucesso.tipoAcesso == 3L) "homeAdm" else "home"
                navController.navigate(destino) {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                val erro = authState as AuthState.Error
                mensagem = traduzErroFirebase(erro.message)
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Onibo", color = Color.White, fontSize = 25.sp)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = fundoGeral
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Faça seu login", fontSize = 22.sp, color = azulPrincipal)
                    Spacer(Modifier.height(20.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.lowercase() },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        visualTransformation = VisualTransformation { text ->
                            TransformedText(AnnotatedString(text.text.lowercase()), OffsetMapping.Identity)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,      // borda preta
                            unfocusedBorderColor = Color.Black,    // borda preta
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    // Senha
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,      // borda preta
                            unfocusedBorderColor = Color.Black,    // borda preta
                            cursorColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(22.dp))

                    // Botão Entrar
                    Button(
                        onClick = {
                            when {
                                email.isEmpty() || senha.isEmpty() -> mensagem = "Preencha todos os campos."
                                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> mensagem = "E-mail inválido."
                                else -> {
                                    mensagem = ""
                                    authViewModel.login(email, senha)
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                    ) {
                        Text("Entrar", color = Color.White)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Cadastre-se
                    TextButton(onClick = { navController.navigate("register") }) {
                        Text("Não tem conta? Cadastre-se", color = azulPrincipal)
                    }

                    // Esqueci minha senha (agora embaixo!)
                    TextButton(onClick = { mostrarDialogRecuperarSenha = true }) {
                        Text("Esqueci minha senha", color = azulPrincipal)
                    }

                    // Mensagem de erro/sucesso do login
                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = mensagem,
                            color = if (mensagem.contains("sucesso", true)) azulPrincipal else Color.Red
                        )
                    }
                }
            }
        }

        // ====================== ALERT DIALOG RECUPERAR SENHA ======================
        if (mostrarDialogRecuperarSenha) {
            AlertDialog(
                onDismissRequest = { if (!mostrandoMensagemSucesso) mostrarDialogRecuperarSenha = false },
                title = { Text("Recuperar senha", color = azulPrincipal, fontSize = 20.sp) },
                text = {
                    Column {
                        if (mostrandoMensagemSucesso) {
                            Text(mensagemDialog, color = Color(0xFF006400), fontSize = 16.sp)
                        } else {
                            OutlinedTextField(
                                value = emailRecuperacao,
                                onValueChange = { emailRecuperacao = it.lowercase() },
                                label = { Text("Seu e-mail cadastrado") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,      // borda preta
                                    unfocusedBorderColor = Color.Black,    // borda preta
                                    cursorColor = Color.Black,
                                    focusedLabelColor = Color.Black,
                                    unfocusedLabelColor = Color.Black,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )

                            if (mensagemDialog.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(mensagemDialog, color = Color.Red, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    if (mostrandoMensagemSucesso) {
                        TextButton(onClick = {
                            mostrarDialogRecuperarSenha = false
                            mostrandoMensagemSucesso = false
                            emailRecuperacao = ""
                            mensagemDialog = ""
                        }) {
                            Text("Fechar", color = azulPrincipal)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                authViewModel.recuperarSenha(emailRecuperacao) { sucesso, msg ->
                                    mensagemDialog = msg
                                    if (sucesso) mostrandoMensagemSucesso = true
                                }
                            },
                            enabled = emailRecuperacao.isNotBlank()
                        ) {
                            Text("Enviar e-mail", color = azulPrincipal)
                        }
                    }
                },
                dismissButton = {
                    if (!mostrandoMensagemSucesso) {
                        TextButton(onClick = {
                            mostrarDialogRecuperarSenha = false
                            emailRecuperacao = ""
                            mensagemDialog = ""
                        }) {
                            Text("Cancelar", color = Color.Red)
                        }
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

private fun traduzErroFirebase(msg: String?): String {
    if (msg == null) return "Erro desconhecido. Tente novamente."

    return when {
        msg.contains("no user record", true) -> "Usuário não encontrado."
        msg.contains("no user corresponding", true) -> "Usuário não encontrado."
        msg.contains("wrong password", true) -> "Senha incorreta."
        msg.contains("invalid", true) -> "Credenciais inválidas."
        msg.contains("badly formatted", true) -> "Formato de e-mail inválido."
        msg.contains("network", true) -> "Erro de conexão. Verifique sua internet."
        msg.contains("too many", true) -> "Muitas tentativas. Tente mais tarde."
        else -> "Erro: $msg"
    }
}
