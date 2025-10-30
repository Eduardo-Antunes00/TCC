package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tcc.viewmodels.UsuarioViewModel

@Composable
fun LoginScreen(navController: NavController, usuarioViewModel: UsuarioViewModel) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tela de Login", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        TextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") })
        Spacer(Modifier.height(8.dp))
        TextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") })

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            usuarioViewModel.buscarPorEmail(email) { user ->
                if (user != null && user.senha == senha) {
                    mensagem = "Login realizado com sucesso, Bem vindo ${user.nome}!"

                    // Fazer navegar para tela inicial
                } else {
                    mensagem = "Usuário ou senha incorretos."
                }
            }
        }) {
            Text("Entrar")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Não tem conta? Cadastre-se")
        }

        if (mensagem.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(mensagem)
        }
    }
}
