// arquivo: com/example/tcc/viewmodels/UsersViewModel.kt
package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.database.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class UsersViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _userBeingEdited = MutableStateFlow<User?>(null)
    val userBeingEdited: StateFlow<User?> = _userBeingEdited.asStateFlow()

    private val _isLoadingEdit = MutableStateFlow(false)
    val isLoadingEdit: StateFlow<Boolean> = _isLoadingEdit.asStateFlow()
    init {
        loadUsers()
    }
    fun selectUserForEdit(id: String) {
        _isLoadingEdit.value = true
        viewModelScope.launch {
            try {
                val doc = db.collection("usuarios").document(id).get().await()
                if (doc.exists()) {
                    val user = User(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "",
                        email = doc.getString("email") ?: "",
                        acesso = doc.getLong("acesso") ?: 1L
                    )
                    _userBeingEdited.value = user
                }
            } catch (e: Exception) {
                // Opcional: mostrar erro com Snackbar
                _userBeingEdited.value = null
            } finally {
                _isLoadingEdit.value = false
            }
        }
    }

    // Limpar quando fechar o popup
    fun clearUserBeingEdited() {
        _userBeingEdited.value = null
    }
    fun loadUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios")
                    .orderBy("nome")
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        User(
                            id = doc.id,
                            nome = doc.getString("nome") ?: "",
                            email = doc.getString("email") ?: "",
                            acesso = doc.getLong("acesso") ?: 1L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _users.value = list
            } catch (e: Exception) {
                // Em produção você pode mostrar um toast/snackbar
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    // Adicione esta função dentro da classe UsersViewModel
    fun updateUser(
        id: String,
        novoNome: String,
        novoEmail: String,
        novoAcesso: Long,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val userRef = db.collection("usuarios").document(id)

                val updates = hashMapOf<String, Any>(
                    "nome" to novoNome,
                    "email" to novoEmail,
                    "acesso" to novoAcesso
                )

                userRef.update(updates).await()
                onSuccess()
                loadUsers() // recarrega a lista
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}