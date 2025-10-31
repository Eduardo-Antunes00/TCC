package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tcc.viewmodels.UsuarioViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController, usuarioViewModel: UsuarioViewModel) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tela de Login", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && senha.isNotBlank()) {
                    carregando = true
                    mensagem = ""
                    auth.signInWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            carregando = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                mensagem = "Bem-vindo, ${user?.email ?: "usuário"}!"
                                // 🔹 Exemplo: navegar para a tela inicial
//                                navController.navigate("home") {
//                                    popUpTo("login") { inclusive = true }
//                                }
                            } else {
                                mensagem = "Usuário ou senha incorretos."
                            }
                        }
                } else {
                    mensagem = "Preencha todos os campos."
                }
            }
        ) {
            Text("Entrar")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Não tem conta? Cadastre-se")
        }

        if (carregando) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (mensagem.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(mensagem)
        }
    }
}
