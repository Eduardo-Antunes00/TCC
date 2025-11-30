// arquivo: com/example/tcc/telas/UsersScreenAdm.kt
package com.example.tcc.telas_adm

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.R
import com.example.tcc.database.model.User
import com.example.tcc.viewmodels.UsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var userToDelete by remember { mutableStateOf<User?>(null) }

    // Estado da barra de pesquisa
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val vermelhoExclusao = Color(0xFFE53935)

    // Filtra usuários ativos + aplica busca
    val filteredUsers = users.filter { it.ativo == true }
        .filter {
            searchQuery.isBlank() ||
                    (it.nome?.contains(searchQuery, ignoreCase = true) == true) ||
                    (it.email?.contains(searchQuery, ignoreCase = true) == true)
        }

    // Campos do formulário
    var editNome by rememberSaveable { mutableStateOf("") }
    var editEmail by rememberSaveable { mutableStateOf("") }
    var editAcesso by rememberSaveable { mutableStateOf(1L) }

    LaunchedEffect(userBeingEdited) {
        userBeingEdited?.let { user ->
            editNome = user.nome.orEmpty()
            editEmail = user.email.orEmpty()
            editAcesso = user.acesso ?: 1L
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

        Column(modifier = Modifier.padding(paddingValues)) {

            // BARRA DE PESQUISA (adicionada aqui)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou e-mail...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Buscar",
                        tint = azulPrincipal
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpar",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = azulPrincipal,
                    unfocusedBorderColor = Color.Black.copy(0.3f),
                    cursorColor = azulPrincipal,

                    // AQUI ESTÃO AS CORES DO TEXTO DIGITADO
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.large)
            )

            Box(modifier = Modifier.weight(1f)) {

                if (isLoading) {
                    CircularProgressIndicator(
                        color = azulPrincipal,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty())
                                "Nenhum usuário encontrado"
                            else "Nenhum usuário encontrado para \"$searchQuery\"",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredUsers,
                            key = { it.id ?: it.hashCode() }
                        ) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (user.acesso) {
                                        1L -> Icon(Icons.Default.Person, "Usuário", tint = azulPrincipal, modifier = Modifier.size(48.dp))
                                        2L -> Image(painterResource(R.drawable.outline_driver), "Motorista", modifier = Modifier.size(40.dp), colorFilter = ColorFilter.tint(azulEscuro))
                                        3L -> Image(painterResource(R.drawable.outline_adm_icon), "Administrador", modifier = Modifier.size(40.dp), colorFilter = ColorFilter.tint(Color(0xFFFF6D00)))
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.nome.orEmpty(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = user.email.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when (user.acesso) {
                                                3L -> "Administrador"
                                                2L -> "Motorista"
                                                else -> "Usuário comum"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when (user.acesso) {
                                                3L -> Color(0xFFFF6D00)
                                                2L -> azulEscuro
                                                else -> azulPrincipal
                                            },
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { viewModel.selectUserForEdit(user.id ?: return@IconButton) }) {
                                            Icon(Icons.Default.Create, "Editar", tint = azulEscuro)
                                        }
                                        IconButton(onClick = { userToDelete = user }) {
                                            Icon(Icons.Default.Delete, "Excluir", tint = vermelhoExclusao)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // === Dialog de Exclusão e BottomSheet permanecem iguais ===
                userToDelete?.let { user ->
                    AlertDialog(
                        onDismissRequest = { userToDelete = null },
                        title = { Text("Excluir usuário?", color = vermelhoExclusao, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Tem certeza que deseja excluir permanentemente:")
                                Spacer(Modifier.height(8.dp))
                                Text(user.nome.orEmpty(), fontWeight = FontWeight.SemiBold)
                                Text(user.email.orEmpty(), color = Color.Black.copy(0.7f))
                                Spacer(Modifier.height(12.dp))
                                Text("Esta ação não pode ser desfeita.", color = vermelhoExclusao)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteUser(
                                        uid = user.id ?: return@TextButton,
                                        onSuccess = { userToDelete = null }
                                    )
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = vermelhoExclusao)
                            ) {
                                Text("Excluir permanentemente")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { userToDelete = null }) { Text("Cancelar") }
                        }
                    )
                }

                userBeingEdited?.let { user ->
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.clearUserBeingEdited() },
                        sheetState = sheetState,
                        containerColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Editar Usuário", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                            if (isLoadingEdit) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = azulPrincipal)
                                }
                            } else {
                                OutlinedTextField(value = editNome, onValueChange = { editNome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())

                                var expanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                    OutlinedTextField(
                                        value = when (editAcesso) {
                                            1L -> "Usuário comum"
                                            2L -> "Motorista"
                                            3L -> "Administrador"
                                            else -> "Usuário comum"
                                        },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Nível de acesso") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("Usuário comum") }, onClick = { editAcesso = 1L; expanded = false })
                                        DropdownMenuItem(text = { Text("Motorista") }, onClick = { editAcesso = 2L; expanded = false })
                                        DropdownMenuItem(text = { Text("Administrador") }, onClick = { editAcesso = 3L; expanded = false })
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.clearUserBeingEdited() }) { Text("Cancelar") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateUser(
                                                id = user.id ?: return@Button,
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
}