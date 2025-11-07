package com.example.tcc.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.repositories.UserRepository
import com.example.tcc.database.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object EmailVerified : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepository()
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun register(email: String, senha: String, nome: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val userData = hashMapOf(
                        "id" to userId,
                        "nome" to nome, // precisa passar o nome junto no cadastro
                        "email" to email,
                        "cordx" to null,
                        "cordy" to null,
                        "mapaAtualId" to null
                    )
                    FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Success
                        }
                        .addOnFailureListener {
                            _authState.value = AuthState.Error("Erro ao salvar dados do usuário.")
                        }

                    val user = auth.currentUser
                    if (user != null) {
                        // envia o e-mail de verificação
                        user.sendEmailVerification().addOnCompleteListener {
                            // salva no Firestore
                            viewModelScope.launch {
                                val newUser = User(
                                    id = user.uid,
                                    nome = nome,
                                    email = email,
                                    senha = senha
                                )
                                userRepo.addUser(newUser)
                                _authState.value = AuthState.Success
                            }
                        }
                    }
                } else {
                    val exception = task.exception
                    val mensagemErro = if (exception is com.google.firebase.auth.FirebaseAuthException) {
                        when (exception.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "Formato de e-mail inválido."
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "E-mail já está em uso."
                            "ERROR_WEAK_PASSWORD" -> "A senha deve ter no mínimo 6 caracteres."
                            else -> exception.localizedMessage ?: "Erro ao cadastrar."
                        }
                    } else {
                        exception?.localizedMessage ?: "Erro ao cadastrar."
                    }
                    _authState.value = AuthState.Error(mensagemErro)
                }
            }
    }

    fun login(email: String, senha: String) {
        _authState.value = AuthState.Loading

        if (email.isEmpty() || senha.isEmpty()) {
            _authState.value = AuthState.Error("Por favor, preencha todos os campos.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Formato de e-mail inválido.")
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // garante que o usuário também exista no Firestore
                        viewModelScope.launch {
                            val existing = userRepo.getUserById(user.uid)
                            if (existing == null) {
                                val newUser = User(
                                    id = user.uid,
                                    nome = user.displayName ?: "",
                                    email = user.email ?: "",
                                    senha = senha
                                )
                                userRepo.addUser(newUser)
                            }
                            _authState.value = AuthState.Success
                        }
                    } else {
                        _authState.value = AuthState.Error("E-mail ainda não verificado.")
                        auth.signOut()
                    }
                } else {
                    _authState.value = AuthState.Error("E-mail ou senha incorretos.")
                }
            }
    }

    fun checkEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful && user.isEmailVerified) {
                    _authState.value = AuthState.EmailVerified
                }
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
