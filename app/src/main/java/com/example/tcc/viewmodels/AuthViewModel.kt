package com.example.tcc.viewmodels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object EmailVerified : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun register(email: String, senha: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                        _authState.value = AuthState.Success
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

        // Validação local
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
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error("E-mail ainda não verificado.")
                        auth.signOut()
                    }
                } else {
                    // Mensagem genérica para evitar expor se o email existe ou não
                    _authState.value = AuthState.Error("E-mail ou senha incorretos.")
                }
            }
    }




    fun checkEmailVerification() {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    if (user.isEmailVerified) {
                        _authState.value = AuthState.EmailVerified
                    }
                }
            }
        } else {
            // Nenhum usuário logado → ignora
        }
    }


    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
