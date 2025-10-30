package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tcc.database.entities.UserEntity
import com.example.tcc.viewmodels.UsuarioViewModel

@Composable
fun RegisterScreen(navController: NavController, usuarioViewModel: UsuarioViewModel) {
    var nome by remember { mutableStateOf("") }
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
        Text("Tela de Cadastro", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
        Spacer(Modifier.height(8.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") })
        Spacer(Modifier.height(8.dp))
        TextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") })

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                val user = UserEntity(
                    nome = nome,
                    email = email,
                    senha = senha,
                    mapaAtualId = null,
                    cordx = null,
                    cordy = null
                )
                usuarioViewModel.inserirUsuario(user) {
                    mensagem = "Cadastro realizado com sucesso!"
                    navController.navigate("login")
                }
            } else {
                mensagem = "Preencha todos os campos!"
            }
        }) {
            Text("Cadastrar")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text("Já tem conta? Entrar")
        }

        if (mensagem.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(mensagem)
        }
    }
}
