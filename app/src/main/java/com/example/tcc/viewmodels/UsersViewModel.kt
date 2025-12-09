// arquivo: com/example/tcc/viewmodels/UsersViewModel.kt
package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.database.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
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
                        acesso = doc.getLong("acesso") ?: 1L,
                        ativo = doc.getBoolean("ativo") ?: true
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

    fun createUser(
        nome: String,
        email: String,
        senha: String = "123456",
        acesso: Long = 1L,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}  // mudamos para String pra mostrar mensagem clara
    ) {
        viewModelScope.launch {
            try {
                // 1. Criar no Firebase Auth SEM logar
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, senha)
                    .await()

                val uid = authResult.user?.uid ?: throw Exception("UID não encontrado")

                // 2. NÃO faz login! Mantém o admin logado

                // 2. Salvar dados no Firestore
                val userData = hashMapOf(
                    "nome" to nome.trim(),
                    "email" to email.trim(),
                    "acesso" to acesso,
                    "ativo" to true
                    // opcional: pode ter criadoEm = FieldValue.serverTimestamp()
                )

                db.collection("usuarios").document(uid).set(userData).await()

                // 3. Opcional: enviar e-mail de verificação
                authResult.user?.sendEmailVerification()

                onSuccess()
                loadUsers() // atualiza a lista

            } catch (e: Exception) {
                val mensagem = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Este e-mail já está sendo usado"
                    e.message?.contains("weak password") == true ->
                        "A senha deve ter pelo menos 6 caracteres"
                    else -> "Erro ao criar: ${e.message}"
                }
                onError(mensagem)
            }
        }
    }
    fun loadUsers() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios")
                    .orderBy("acesso", Query.Direction.DESCENDING)  // maior acesso primeiro
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    val ativo = doc.getBoolean("ativo") // pode ser true, false ou null
                    // Se o campo não existir ou for null → considera ativo (comportamento antigo)
                    if (ativo == false) return@mapNotNull null // só ignora se for explicitamente false

                    try {
                        User(
                            id = doc.id,
                            nome = doc.getString("nome") ?: "Sem nome",
                            email = doc.getString("email") ?: "sem@email.com",
                            acesso = doc.getLong("acesso") ?: 1L,
                            ativo = ativo ?: true // se não tiver o campo, considera true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                _users.value = list
            } catch (e: Exception) {
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
        novoAcesso: Long,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val userRef = db.collection("usuarios").document(id)

                val updates = hashMapOf<String, Any>(
                    "nome" to novoNome,
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

    fun deleteUser(
        uid: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Marca como inativo (não deleta do Auth por segurança no cliente)
                db.collection("usuarios").document(uid)
                    .update("ativo", false)
                    .await()
                loadUsers()
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}