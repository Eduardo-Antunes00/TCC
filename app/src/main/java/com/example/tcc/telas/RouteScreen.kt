package com.example.tcc.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
@Composable
fun RouteScreen(navController: NavController, id: String){
    Column (modifier = Modifier.fillMaxSize(),Arrangement.Center,Alignment.CenterHorizontally)
    { Text(id) }
}