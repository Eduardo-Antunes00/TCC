package com.example.tcc.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object EmailVerified : AuthState()

    data class Success(val tipoAcesso: Long) : AuthState()
      // Agora carrega o valor de "acesso"
    data class Error(
        val message: String,
        val exception: Throwable? = null  // NOVO: exceção completa
    ) : AuthState()
}



    class AuthViewModel : ViewModel() {
        private val auth: FirebaseAuth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()

        private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
        val authState: LiveData<AuthState> = _authState

        private var verificationJob: Job? = null
        private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        // ================================
        // REGISTRO
        // ================================
        fun register(email: String, password: String, nome: String) {
            _authState.value = AuthState.Loading

            viewModelScope.launch {
                try {
                    // 1. Cria usuário no Firebase Auth
                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = authResult.user ?: throw Exception("Usuário não criado.")

                    val userId = user.uid

                    // 2. Salva dados no Firestore
                    val userData = hashMapOf(
                        "id" to userId,
                        "nome" to nome,
                        "email" to email,
                        "cordx" to null,
                        "cordy" to null,
                    )

                    firestore.collection("usuarios")
                        .document(userId)
                        .set(userData)
                        .await()

                    // 3. Envia e-mail de verificação
                    user.sendEmailVerification().await()

                    _authState.value = AuthState.Success(1L)
                    startEmailVerificationWatcher(user)

                } catch (e: Exception) {
                    _authState.value = AuthState.Error(
                        message = e.localizedMessage ?: "Erro ao cadastrar.",
                        exception = e
                    )
                }
            }
        }

        // ================================
        // LOGIN
        // ================================
        fun login(email: String, password: String) {
            _authState.value = AuthState.Loading

            viewModelScope.launch {
                try {
                    // 1. Faz login no Firebase Auth
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user ?: throw IllegalStateException("Usuário não encontrado.")

                    user.reload().await()

                    // 2. Verifica se o e-mail está verificado
                    if (!user.isEmailVerified) {
                        auth.signOut()
                        _authState.value = AuthState.Error(
                            message = "Verifique seu e-mail antes de fazer login.",
                            exception = null
                        )
                        return@launch
                    }

                    // 3. Busca o campo "acesso" no Firestore
                    val userDoc = firestore.collection("usuarios")
                        .document(user.uid)
                        .get()
                        .await()

                    if (!userDoc.exists()) {
                        auth.signOut()
                        _authState.value = AuthState.Error("Dados do usuário não encontrados.")
                        return@launch
                    }

                    val acesso = userDoc.getLong("acesso") ?: 1L  // padrão = 1 se não existir

                    // 4. Emite sucesso + tipo de acesso
                    _authState.value = AuthState.Success(acesso)  // já está assim no código que te mandei

                } catch (e: Exception) {
                    val errorMessage = when {
                        e is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                            "Usuário não encontrado. Verifique o e-mail."
                        }
                        e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            if (e.errorCode == "ERROR_INVALID_CREDENTIAL") {
                                "As credenciais estão incorretas, malformadas ou expiradas."
                            } else {
                                "Senha incorreta."
                            }
                        }
                        e is com.google.firebase.auth.FirebaseAuthException -> {
                            when (e.errorCode) {
                                "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
                                "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
                                "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas. Tente novamente mais tarde."
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Erro de conexão. Verifique sua internet."
                                else -> e.localizedMessage ?: "Erro ao fazer login."
                            }
                        }
                        else -> e.localizedMessage ?: "Erro desconhecido."
                    }

                    _authState.value = AuthState.Error(errorMessage, e)
                }
            }
        }

        // ================================
        // VERIFICAÇÃO DE E-MAIL
        // ================================
        fun checkEmailVerification() {
            val user = auth.currentUser ?: return
            viewModelScope.launch {
                try {
                    user.reload().await()
                    if (user.isEmailVerified) {
                        _authState.value = AuthState.EmailVerified
                        stopVerificationWatcher()
                    }
                } catch (e: Exception) {
                    // Silencioso: só tenta de novo depois
                }
            }
        }

        private fun startEmailVerificationWatcher(user: FirebaseUser) {
            stopVerificationWatcher()
            verificationJob = viewModelScope.launch {
                while (isActive) {
                    delay(3000)
                    try {
                        user.reload().await()
                        if (user.isEmailVerified) {
                            _authState.value = AuthState.EmailVerified
                            stopVerificationWatcher()
                            break
                        }
                    } catch (e: Exception) {
                        // Ignora erros de rede temporários
                    }
                }
            }
        }

        private fun stopVerificationWatcher() {
            verificationJob?.cancel()
            verificationJob = null
        }

        // ================================
        // RESET
        // ================================
        fun resetAuthState() {
            _authState.value = AuthState.Idle
            stopVerificationWatcher()
        }

        override fun onCleared() {
            super.onCleared()
            stopVerificationWatcher()
            viewModelScope.cancel()
        }
        // ================================
// VERIFICA USUÁRIO JÁ LOGADO
// ================================
        fun checkCurrentUser(): FirebaseUser? {
            return auth.currentUser?.takeIf { it.isEmailVerified }
        }

        // Força reload pra garantir que o email foi verificado
        suspend fun verifyCurrentUser(): Boolean {
            return try {
                val user = auth.currentUser ?: return false
                user.reload().await()
                user.isEmailVerified
            } catch (e: Exception) {
                false
            }
        }
    }

