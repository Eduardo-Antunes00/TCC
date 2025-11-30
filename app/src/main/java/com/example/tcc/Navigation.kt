package com.example.tcc

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tcc.telas.HomeScreen
import com.example.tcc.telas.HomeScreenAdm
import com.example.tcc.telas.RegisterScreen
import com.example.tcc.telas.LoginScreen
import com.example.tcc.telas.ProfileScreen
import com.example.tcc.telas.RouteEditScreenAdm
import com.example.tcc.telas.RouteScreen
import com.example.tcc.telas.UsersScreenAdm
import com.example.tcc.viewmodels.AuthViewModel
import com.example.tcc.viewmodels.MapViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    mapViewModel: MapViewModel,
    startDestination: String = "login"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("home") {
            HomeScreen(navController = navController, mapViewModel = mapViewModel)
        }
        composable("route/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: "1"
            RouteScreen(navController = navController, routeId = id)
        }
        composable("routeEditAdm/{routeId}") { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: "new"
            RouteEditScreenAdm(routeId = routeId, navController = navController)
        }
        composable("usersAdm") { UsersScreenAdm(navController = navController) }
        composable("routeEditAdm/new") {
            RouteEditScreenAdm(routeId = "new", navController = navController)
        }
        composable("profile") { // Rota para a tela de perfil
            ProfileScreen(navController = navController)
        }
        composable("homeAdm") { // Rota para a tela de perfil
            HomeScreenAdm(navController = navController)
        }
    }
}