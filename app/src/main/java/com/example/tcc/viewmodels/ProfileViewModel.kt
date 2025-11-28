package com.example.tcc.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tcc.database.model.User
import com.example.tcc.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _error.value = "Usuário não está autenticado"
            return
        }

        val email = currentUser.email
        if (email.isNullOrEmpty()) {
            _error.value = "Email do usuário não encontrado"
            return
        }

        _isLoading.value = true

        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios")                   // <-- nome da coleção no Firestore
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                _isLoading.value = false

                if (documents.isEmpty) {
                    _error.value = "Perfil não encontrado"
                    _userProfile.value = null
                    return@addOnSuccessListener
                }

                var document = documents.first()
                val profile = document.toObject(User::class.java).apply {
                }

                _userProfile.value = profile
                _error.value = null
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _error.value = "Erro: ${exception.localizedMessage}"
                exception.printStackTrace() // <--- isso mostra o erro real no Logcat!
            }
    }
}