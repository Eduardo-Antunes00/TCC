// arquivo: com/example/tcc/telas/UsersScreenAdm.kt
package com.example.tcc.telas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.R
import com.example.tcc.viewmodels.UsersViewModel@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreenAdm(
    navController: NavController,
    viewModel: UsersViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userBeingEdited by viewModel.userBeingEdited.collectAsState()
    val isLoadingEdit by viewModel.isLoadingEdit.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // CORES
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)

    // Campos do formulário
    var editNome by rememberSaveable { mutableStateOf("") }
    var editEmail by rememberSaveable { mutableStateOf("") }
    var editAcesso by rememberSaveable { mutableStateOf(1L) }

    // Quando um usuário for selecionado, preenche os campos
    LaunchedEffect(userBeingEdited) {
        userBeingEdited?.let { user ->
            editNome = user.nome ?: "Indefinido"
            editEmail = user.email ?: "Indefinido"
            editAcesso = user.acesso ?: 1
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Usuários", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = Color(0xFFF0F7FF)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = azulPrincipal,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum usuário encontrado", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ícone
                                    if (user.acesso == 1L) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Usuário",
                                            tint = azulPrincipal,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.outline_adm_icon),
                                            contentDescription = "Administrador",
                                            modifier = Modifier.size(48.dp),
                                            colorFilter = ColorFilter.tint(Color(0xFFFF6D00))
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            user.nome ?: "Indefinido",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = azulEscuro
                                        )
                                        Text(
                                            user.email ?: "Indefinido",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = if (user.acesso == 2L) "Administrador" else "Usuário comum",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (user.acesso == 2L) Color(0xFFFF6D00) else azulClaro,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // BOTÃO DE EDITAR NO CANTO INFERIOR DIREITO
                                IconButton(
                                    onClick = { viewModel.selectUserForEdit(user.id ?: "1") },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.Create, "Editar", tint = azulEscuro)
                                }
                            }
                        }
                    }
                }

                // Loading geral
                if (isLoading) {
                    CircularProgressIndicator(
                        color = azulPrincipal,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // POPUP DE EDIÇÃO (só abre se tiver usuário selecionado) Pedir ajuda do Spies para entender
            userBeingEdited?.let { user ->
                ModalBottomSheet(
                    onDismissRequest = { viewModel.clearUserBeingEdited() },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    scrimColor = Color.Black.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Editar Usuário",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black  // Título preto
                        )

                        if (isLoadingEdit) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = azulPrincipal)
                            }
                        } else {
                            OutlinedTextField(
                                value = editNome,
                                onValueChange = { editNome = it },
                                label = { Text("Nome") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                                    focusedLabelColor = Color.Black,
                                    unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
                                    cursorColor = Color.Black,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )

                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("E-mail") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Black,
                                    unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                                    focusedLabelColor = Color.Black,
                                    unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
                                    cursorColor = Color.Black,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                )
                            )

                            // Dropdown de acesso (com texto preto)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = if (editAcesso == 2L) "Administrador" else "Usuário comum",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Nível de acesso") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = expanded
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Black,
                                        unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                                        focusedLabelColor = Color.Black,
                                        unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
                                        cursorColor = Color.Black,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Usuário comum", color = Color.Black) },
                                        onClick = { editAcesso = 1L; expanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Administrador", color = Color.Black) },
                                        onClick = { editAcesso = 2L; expanded = false }
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { viewModel.clearUserBeingEdited() }) {
                                    Text("Cancelar", color = Color.Black.copy(alpha = 0.8f))
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateUser(
                                            id = user.id?: "1",  // CORRIGIDO: user.uid (não user.id)
                                            novoNome = editNome,
                                            novoEmail = editEmail,
                                            novoAcesso = editAcesso,
                                            onSuccess = { viewModel.clearUserBeingEdited() }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                                ) {
                                    Text("Salvar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
