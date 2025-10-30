package com.example.tcc

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tcc.telas.LoginScreen
import com.example.tcc.telas.RegisterScreen
import com.example.tcc.viewmodels.UsuarioViewModel

@Composable
fun AppNavigation(navController: NavHostController, usuarioViewModel: UsuarioViewModel) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController, usuarioViewModel)
        }
        composable("register") {
            RegisterScreen(navController, usuarioViewModel)
        }
    }
}
