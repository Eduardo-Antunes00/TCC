package com.example.tcc

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tcc.telas.HomeScreen
import com.example.tcc.telas.RegisterScreen
import com.example.tcc.telas.LoginScreen
import com.example.tcc.telas.RouteScreen
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.MapViewModel

@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel, mapViewModel: MapViewModel) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("register") {
            RegisterScreen(navController, authViewModel)
        }
        composable("home") {
            HomeScreen(navController,mapViewModel)
        }
        composable("route/{id}"){
            backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: "1"
            RouteScreen(navController, id)
        }

    }
}

