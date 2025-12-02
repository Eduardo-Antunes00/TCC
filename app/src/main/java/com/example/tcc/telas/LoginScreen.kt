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
    // Resetar estado sempre ao abrir
    LaunchedEffect(Unit) { authViewModel.resetAuthState() }

    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    val authState by authViewModel.authState.observeAsState(AuthState.Idle)

    // CORES do padrão da Home
    val azulPrincipal = Color(0xFF0066FF)
    val fundoGeral = Color(0xFFF0F7FF)

    // Reagir ao estado de login
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> mensagem = "Carregando..."

            is AuthState.Success -> {
                val ok = authState as AuthState.Success
                mensagem = "Login bem-sucedido!"

                val destino = if (ok.tipoAcesso == 3L) "homeAdm" else "home"

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
                    Text(
                        "Onibo",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = azulPrincipal
                )
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

                    Text(
                        "Faça seu login",
                        fontSize = 22.sp,
                        color = azulPrincipal
                    )
                    Spacer(Modifier.height(20.dp))

                    // ======== CAMPO EMAIL ========
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.lowercase() },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrect = false,
                            capitalization = KeyboardCapitalization.None
                        ),
                        visualTransformation = VisualTransformation { text ->
                            TransformedText(
                                AnnotatedString(text.text.lowercase()),
                                OffsetMapping.Identity
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
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

                    Spacer(Modifier.height(14.dp))

                    // ======== CAMPO SENHA ========
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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

                    Spacer(Modifier.height(22.dp))

                    // ======== BOTÃO ========
                    Button(
                        onClick = {
                            when {
                                email.isEmpty() || senha.isEmpty() ->
                                    mensagem = "Por favor, preencha todos os campos."

                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                    mensagem = "Formato de e-mail inválido."

                                else -> {
                                    mensagem = ""
                                    authViewModel.login(email, senha)
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                    ) {
                        Text("Entrar", color = Color.White)
                    }

                    Spacer(Modifier.height(10.dp))

                    TextButton(onClick = { navController.navigate("register") }) {
                        Text("Não tem conta? Cadastre-se", color = azulPrincipal)
                    }

                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            mensagem,
                            color = if (mensagem.contains("sucesso", true))
                                azulPrincipal else Color.Red
                        )
                    }
                }
            }
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
