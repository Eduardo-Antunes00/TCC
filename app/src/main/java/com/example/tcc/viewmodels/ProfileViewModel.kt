// ProfileViewModel.kt — VERSÃO CORRIGIDA E MELHORADA

package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.database.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUser = auth.currentUser ?: run {
            _error.value = "Usuário não autenticado"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val snapshot = db.collection("usuarios")
                    .whereEqualTo("email", currentUser.email)
                    .limit(1)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    _error.value = "Perfil não encontrado"
                } else {
                    _userProfile.value = snapshot.documents[0].toObject(User::class.java)
                }
            } catch (e: Exception) {
                _error.value = "Erro: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(newName: String, onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("usuarios")
                    .whereEqualTo("email", user.email)
                    .get()
                    .await()
                    .documents.firstOrNull()

                doc?.reference?.update("nome", newName)?.await()
                loadUserProfile()
                onComplete()
            } catch (e: Exception) {
                _error.value = "Erro ao salvar nome"
            }
        }
    }


    fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: run { onComplete(false, "Usuário não logado"); return }

        viewModelScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                user.reauthenticate(credential).await()

                user.updatePassword(newPassword).await()

                // AQUI ESTAVA FALTANDO: atualizar senha no Firestore (se você guarda)
                // Se você NÃO guarda senha no Firestore (recomendado!), ignore essa parte
                // Caso guarde (não recomendado), faça:
                // doc.reference.update("senha", newPassword)

                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, "Senha atual incorreta")
            }
        }
    }
}