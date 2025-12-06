// arquivo: com/example/tcc/telas_adm/UsersScreenAdm.kt
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Estados do formulário
    var isCreatingNewUser by rememberSaveable { mutableStateOf(false) }
    var editNome by rememberSaveable { mutableStateOf("") }
    var editEmail by rememberSaveable { mutableStateOf("") }
    var editSenha by rememberSaveable { mutableStateOf("") } // Novo campo
    var editAcesso by rememberSaveable { mutableStateOf(1L) }

// Crie esta extensão uma única vez (pode colocar fora do composable)
    val OutlinedTextFieldCoresPretas = TextFieldDefaults.colors(
        // Cores do texto digitado
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black,
        errorTextColor = Color.Red,

        // Cores do container (fundo) ← ESSAS DUAS RESOLVEM O FUNDO PRETO
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        errorContainerColor = Color.White,

        // Cursor
        cursorColor = Color.Black,
        errorCursorColor = Color.Red,

        // Borda / indicador (linha de baixo)
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Black,
        disabledIndicatorColor = Color.Black.copy(alpha = 0.4f),
        errorIndicatorColor = Color.Red,

        // Label
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
        disabledLabelColor = Color.Black.copy(alpha = 0.6f),
        errorLabelColor = Color.Red,

        // Ícones (leading/trailing)
        focusedLeadingIconColor = Color.Black,
        unfocusedLeadingIconColor = Color.Black.copy(alpha = 0.7f),
        focusedTrailingIconColor = Color.Black,
        unfocusedTrailingIconColor = Color.Black.copy(alpha = 0.7f),
    )
    // Cores
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val vermelhoExclusao = Color(0xFFE53935)

    // Filtragem
    val filteredUsers = users.filter { it.ativo == true }
        .filter {
            searchQuery.isBlank() ||
                    it.nome?.contains(searchQuery, ignoreCase = true) == true ||
                    it.email?.contains(searchQuery, ignoreCase = true) == true
        }

    // Limpar campos ao criar novo
    LaunchedEffect(isCreatingNewUser) {
        if (isCreatingNewUser) {
            editNome = ""
            editEmail = ""
            editSenha = "" // limpa a senha
            editAcesso = 1L
        }
    }

    // Preencher ao editar
    LaunchedEffect(userBeingEdited, isCreatingNewUser) {
        if (!isCreatingNewUser && userBeingEdited != null) {
            editNome = userBeingEdited!!.nome.orEmpty()
            editEmail = userBeingEdited!!.email.orEmpty()
            editAcesso = userBeingEdited!!.acesso ?: 1L
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isCreatingNewUser = true
                    viewModel.clearUserBeingEdited()
                },
                containerColor = azulPrincipal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar usuário")
            }
        },
        containerColor = Color(0xFFF0F7FF)
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues)) {

            // Barra de pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou e-mail...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Person, "Buscar", tint = azulPrincipal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Limpar", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = azulPrincipal,
                    unfocusedBorderColor = Color.Black.copy(0.3f),
                    cursorColor = azulPrincipal,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.large)
            )

            Box(modifier = Modifier.weight(1f)) {
                if(isLoading){
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = azulPrincipal)
                    }
                }
                 if (filteredUsers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Nenhum usuário encontrado"
                            else "Nenhum usuário para \"$searchQuery\"",
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
                        items(filteredUsers, key = { it.id ?: it.hashCode() }) { user ->
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
                                        Text(user.nome.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(user.email.orEmpty(), color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = when (user.acesso) { 3L -> "Administrador" ; 2L -> "Motorista" ; else -> "Usuário comum" },
                                            color = when (user.acesso) { 3L -> Color(0xFFFF6D00) ; 2L -> azulEscuro ; else -> azulPrincipal },
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            isCreatingNewUser = false
                                            viewModel.selectUserForEdit(user.id ?: return@IconButton)
                                        }) {
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

                // Dialog de exclusão
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
                                    val id = user.id ?: return@TextButton

                                    // 1. Fecha o dialog imediatamente
                                    userToDelete = null

                                    // 2. Depois dispara a exclusão (em background)
                                    viewModel.deleteUser(id) {
                                        // Aqui você pode mostrar um SnackBar de sucesso, se quiser
                                        // ex: scaffoldState.snackbarHostState.showSnackbar("Usuário excluído")
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = vermelhoExclusao)
                            ) {
                                Text("Excluir permanentemente")
                            }
                        },
                        dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancelar") } }
                    )
                }

                // BottomSheet: Criação ou Edição
                if (userBeingEdited != null || isCreatingNewUser) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            viewModel.clearUserBeingEdited()
                            isCreatingNewUser = false
                        },
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
                            Text(
                                text = if (isCreatingNewUser) "Novo Usuário" else "Editar Usuário",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (isLoadingEdit) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = azulPrincipal)
                                }
                            } else {
                                OutlinedTextField(
                                    value = editNome,
                                    onValueChange = { editNome = it },
                                    label = { Text("Nome") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldCoresPretas

                                )

                                OutlinedTextField(
                                    value = editEmail,
                                    onValueChange = { editEmail = it },
                                    label = { Text("E-mail") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldCoresPretas
                                )

                                // Campo de senha SOMENTE na criação
                                if (isCreatingNewUser) {
                                    var senhaVisivel by remember { mutableStateOf(false) }
                                    OutlinedTextField(
                                        value = editSenha,
                                        onValueChange = { editSenha = it },
                                        label = { Text("Senha (deixe vazio para usar 123456)") },
                                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldCoresPretas,
                                    )
                                }

                                // Dropdown de nível de acesso
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
                                        colors = OutlinedTextFieldCoresPretas
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("Usuário comum") }, onClick = { editAcesso = 1L; expanded = false })
                                        DropdownMenuItem(text = { Text("Motorista") }, onClick = { editAcesso = 2L; expanded = false })
                                        DropdownMenuItem(text = { Text("Administrador") }, onClick = { editAcesso = 3L; expanded = false })
                                    }
                                }

                                // Botões
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(onClick = {
                                        viewModel.clearUserBeingEdited()
                                        isCreatingNewUser = false
                                    }) {
                                        Text("Cancelar")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (isCreatingNewUser) {
                                                val senhaFinal = if (editSenha.trim().isEmpty()) "123456" else editSenha.trim()
                                                viewModel.createUser(
                                                    nome = editNome.trim(),
                                                    email = editEmail.trim(),
                                                    senha = senhaFinal,
                                                    acesso = editAcesso,
                                                    onSuccess = { isCreatingNewUser = false },
                                                    onError = { /* opcional: mostrar erro */ }
                                                )
                                            } else {
                                                viewModel.updateUser(
                                                    id = userBeingEdited?.id ?: return@Button,
                                                    novoNome = editNome.trim(),
                                                    novoEmail = editEmail.trim(),
                                                    novoAcesso = editAcesso,
                                                    onSuccess = { viewModel.clearUserBeingEdited() }
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                                    ) {
                                        Text(
                                            text = if (isCreatingNewUser) "Criar Usuário" else "Salvar Alterações",
                                            color = Color.White
                                        )
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