package com.example.tcc.telas_adm

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

    var userToDelete by remember { mutableStateOf<User?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Estados para edição
    var editNome by rememberSaveable { mutableStateOf("") }
    var editAcesso by rememberSaveable { mutableStateOf(1L) }

    // Cores
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val vermelhoExclusao = Color(0xFFE53935)

    // Filtragem
    val filteredUsers = users
        .filter { it.ativo == true }
        .filter {
            searchQuery.isBlank() ||
                    it.nome.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    it.email.orEmpty().contains(searchQuery, ignoreCase = true)
        }

    // Preenche ao editar
    LaunchedEffect(userBeingEdited) {
        userBeingEdited?.let {
            editNome = it.nome.orEmpty()
            editAcesso = it.acesso ?: 1L
        }
    }

    // Cores padrão dos TextFields (tudo preto, fundo branco)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black,
        errorTextColor = Color.Red,

        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,

        cursorColor = Color.Black,
        errorCursorColor = Color.Red,

        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
        disabledBorderColor = Color.Black.copy(alpha = 0.3f),

        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
        disabledLabelColor = Color.Black.copy(alpha = 0.6f),
    )

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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("register?fromAdmin=true") },
                containerColor = azulPrincipal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Novo usuário", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color(0xFFF0F7FF)
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues)) {

            // Barra de busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou e-mail...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = azulPrincipal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = textFieldColors.copy(
                    cursorColor = azulPrincipal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White, MaterialTheme.shapes.large)
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = azulPrincipal)
                        }
                    }
                    filteredUsers.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Nenhum usuário encontrado"
                                else "Nenhum resultado para \"$searchQuery\"",
                                color = Color.Gray,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredUsers, key = { it.id!! }) { user ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(8.dp),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Ícone do tipo
                                        when (user.acesso) {
                                            1L -> Icon(Icons.Default.Person, null, tint = azulPrincipal, modifier = Modifier.size(48.dp))
                                            2L -> Image(
                                                painter = painterResource(R.drawable.outline_driver),
                                                contentDescription = "Motorista",
                                                modifier = Modifier.size(40.dp),
                                                colorFilter = ColorFilter.tint(azulEscuro)
                                            )
                                            3L -> Image(
                                                painter = painterResource(R.drawable.outline_adm_icon),
                                                contentDescription = "Administrador",
                                                modifier = Modifier.size(40.dp),
                                                colorFilter = ColorFilter.tint(Color(0xFFFF6D00))
                                            )
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.nome.orEmpty(),
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = user.email.orEmpty(),
                                                color = Color.DarkGray,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis  // RETICÊNCIAS AQUI
                                            )
                                            Text(
                                                text = when (user.acesso) {
                                                    3L -> "Administrador"
                                                    2L -> "Motorista"
                                                    else -> "Usuário comum"
                                                },
                                                color = when (user.acesso) {
                                                    3L -> Color(0xFFFF6D00)
                                                    2L -> azulEscuro
                                                    else -> azulPrincipal
                                                },
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                viewModel.selectUserForEdit(user.id!!)
                                            }) {
                                                Icon(Icons.Default.Edit, "Editar", tint = azulEscuro)
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
                }

                // Confirmação de exclusão
                userToDelete?.let { user ->
                    AlertDialog(
                        onDismissRequest = { userToDelete = null },
                        title = { Text("Excluir usuário?", color = vermelhoExclusao) },
                        text = {
                            Column {
                                Text("Isso removerá permanentemente:")
                                Spacer(Modifier.height(8.dp))
                                Text(user.nome.orEmpty(), fontWeight = FontWeight.SemiBold)
                                Text(user.email.orEmpty(), color = Color.DarkGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Esta ação não pode ser desfeita.", color = vermelhoExclusao)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // 1. Fecha o diálogo imediatamente
                                    userToDelete = null

                                    // 2. Depois dispara a exclusão (em background)
                                    viewModel.deleteUser(user.id!!) {
                                        // Opcional: mostrar SnackBar ou Toast de sucesso
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = vermelhoExclusao)
                            ) {
                                Text("Excluir permanentemente")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { userToDelete = null }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                // BottomSheet de edição
                if (userBeingEdited != null) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.clearUserBeingEdited() },
                        containerColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Editar Usuário", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                            if (isLoadingEdit) {
                                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                    CircularProgressIndicator(color = azulPrincipal)
                                }
                            } else {
                                OutlinedTextField(
                                    value = editNome,
                                    onValueChange = { editNome = it },
                                    label = { Text("Nome") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors
                                )

                                OutlinedTextField(
                                    value = userBeingEdited!!.email.orEmpty(),
                                    onValueChange = {},
                                    label = { Text("E-mail") },
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(Icons.Default.Lock, "E-mail fixo", tint = Color.Gray)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors.copy(
                                        disabledTextColor = Color.Black,
                                        disabledLabelColor = Color.Black.copy(0.6f)
                                    )
                                )

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
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = textFieldColors
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        listOf("Usuário comum", "Motorista", "Administrador").forEachIndexed { i, texto ->
                                            DropdownMenuItem(
                                                text = { Text(texto) },
                                                onClick = {
                                                    editAcesso = (i + 1).toLong()
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.clearUserBeingEdited() }) {
                                        Text("Cancelar")
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateUser(
                                                id = userBeingEdited!!.id!!,
                                                novoNome = editNome.trim(),
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