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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, usuarioViewModel: UsuarioViewModel) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope() // ⚡ escopo para navegação

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cadastro de Usuário") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                        auth.createUserWithEmailAndPassword(email.trim(), senha.trim())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Cadastro no Auth deu certo, podemos navegar
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }

                                    // Agora salva os dados extras em Firestore
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        val dadosUsuario = hashMapOf(
                                            "nome" to nome,
                                            "email" to email.trim(),
                                            "cidade_preferida_id" to null,
                                            "cord_x" to null,
                                            "cord_y" to null
                                        )
                                        db.collection("usuarios")
                                            .document(userId)
                                            .set(dadosUsuario)
                                            .addOnFailureListener {
                                                // opcional: mostrar Snackbar de erro, mas não bloqueia navegação
                                            }
                                    }
                                } else {
                                    mensagem = "Erro ao cadastrar: ${task.exception?.message}"
                                }
                            }
                    } else {
                        mensagem = "Preencha todos os campos!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cadastrar")
            }


            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Já tem conta? Entrar")
            }

            if (mensagem.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(mensagem)
            }
        }
    }
}


