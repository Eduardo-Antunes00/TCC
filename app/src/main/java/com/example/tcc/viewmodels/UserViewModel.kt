package com.example.tcc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tcc.database.dao.UserDao
import com.example.tcc.database.entities.UserEntity
import kotlinx.coroutines.launch

class UsuarioViewModel(private val userDao: UserDao) : ViewModel() {

    // 🔹 Inserir novo usuário
    fun inserirUsuario(usuario: UserEntity, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = userDao.inserirUsuario(usuario)
            onSuccess(id)
        }
    }

    // 🔹 Buscar usuário pelo e-mail (para login)
    fun buscarPorEmail(email: String, onResult: (UserEntity?) -> Unit) {
        viewModelScope.launch {
            val usuario = userDao.buscarPorEmail(email)
            onResult(usuario)
        }
    }

    // 🔹 Listar todos os usuários (opcional)
    fun listarUsuarios(onResult: (List<UserEntity>) -> Unit) {
        viewModelScope.launch {
            val lista = userDao.listarUsuarios()
            onResult(lista)
        }
    }
}
