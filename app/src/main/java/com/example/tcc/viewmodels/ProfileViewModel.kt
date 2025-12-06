package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

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
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            _error.value = "Usuário não autenticado"
            return
        }

        _isLoading.value = true
        _error.value = null

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("email", currentUser.email)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                _isLoading.value = false
                if (docs.isEmpty) {
                    _error.value = "Perfil não encontrado"
                } else {
                    _userProfile.value = docs.first().toObject(User::class.java)
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _error.value = "Erro ao carregar: ${it.message}"
            }
    }

    fun updateName(newName: String, onComplete: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios")
            .whereEqualTo("email", user.email)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    docs.first().reference.update("nome", newName)
                        .addOnSuccessListener {
                            loadUserProfile()
                            onComplete()
                        }
                }
            }
    }

    fun updateEmailWithPassword(newEmail: String, currentPassword: String, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updateEmail(newEmail)
                    .addOnSuccessListener {
                        // Atualiza no Firestore também
                        FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .whereEqualTo("email", user.email)
                            .get()
                            .addOnSuccessListener { docs ->
                                if (!docs.isEmpty) {
                                    docs.first().reference.update("email", newEmail)
                                        .addOnSuccessListener {
                                            loadUserProfile()
                                            onComplete(true)
                                        }
                                }
                            }
                    }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun updatePassword(oldPassword: String, newPassword: String, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}