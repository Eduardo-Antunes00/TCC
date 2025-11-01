package com.example.tcc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.tcc.ui.theme.TCCTheme
import com.example.tcc.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel = AuthViewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TCCTheme {
                val navController = rememberNavController()

                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}
