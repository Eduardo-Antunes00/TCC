package com.example.tcc

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.tcc.telas.HomeScreen
import com.example.tcc.telas_adm.HomeScreenAdm
import com.example.tcc.telas.RegisterScreen
import com.example.tcc.telas.LoginScreen
import com.example.tcc.telas.ProfileScreen
import com.example.tcc.telas_adm.RouteEditScreenAdm
import com.example.tcc.telas.RouteScreen
import com.example.tcc.telas_adm.UsersScreenAdm
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
        // Tela de Login
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        // Tela de Registro – agora aceita parâmetro opcional ?fromAdmin=true
        composable(
            route = "register?fromAdmin={fromAdmin}",
            arguments = listOf(
                navArgument("fromAdmin") {
                    defaultValue = false
                    type = androidx.navigation.NavType.BoolType
                }
            )
        ) { backStackEntry ->
            val fromAdmin = backStackEntry.arguments?.getBoolean("fromAdmin") ?: false
            RegisterScreen(
                navController = navController,
                authViewModel = authViewModel,
                fromAdmin = fromAdmin
            )
        }

        // Home do usuário comum
        composable("home") {
            HomeScreen(navController = navController, mapViewModel = mapViewModel)
        }

        // Home do administrador
        composable("homeAdm") {
            HomeScreenAdm(navController = navController)
        }

        // Tela de rota detalhada (usuário comum e admin)
        composable(
            route = "route/{routeId}",
            arguments = listOf(navArgument("routeId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: "1"
            RouteScreen(navController = navController, routeId = routeId)
        }

        // Edição/criação de rota (apenas admin)
        composable(
            route = "routeEditAdm/{routeId}",
            arguments = listOf(navArgument("routeId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: "new"
            RouteEditScreenAdm(routeId = routeId, navController = navController)
        }

        // Atalho direto para criar nova rota
        composable("routeEditAdm/new") {
            RouteEditScreenAdm(routeId = "new", navController = navController)
        }

        // Tela de usuários (apenas admin)
        composable("usersAdm") {
            UsersScreenAdm(navController = navController)
        }

        // Tela de perfil do usuário
        composable("profile") {
            ProfileScreen(navController = navController)
        }
    }
}