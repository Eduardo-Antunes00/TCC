package com.example.tcc.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tcc.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val fundoTela = Color(0xFFF0F7FF)

    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Diálogos
    var showEditName by remember { mutableStateOf(false) }
    var showEditEmail by remember { mutableStateOf(false) }
    var showEditPassword by remember { mutableStateOf(false) }

    // Campos temporários
    var tempName by remember { mutableStateOf("") }
    var tempEmail by remember { mutableStateOf("") }
    var currentPasswordForEmail by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Erros
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Perfil do Usuário",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = fundoTela
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = azulPrincipal, strokeWidth = 6.dp)
                    }
                }

                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Close, tint = Color.Red, modifier = Modifier.size(64.dp), contentDescription = null)
                            Spacer(Modifier.height(16.dp))
                            Text("Erro ao carregar perfil", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(error ?: "", color = Color.Gray, fontSize = 14.sp)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.loadUserProfile() }, colors = ButtonDefaults.buttonColors(azulPrincipal)) {
                                Text("Tentar novamente", color = Color.White)
                            }
                        }
                    }
                }

                userProfile == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum perfil encontrado", color = azulEscuro, fontSize = 18.sp)
                    }
                }

                else -> {
                    val profile = userProfile!!

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(12.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                ProfileItemRow(
                                    label = "Nome",
                                    value = profile.nome.orEmpty(),
                                    onEditClick = {
                                        tempName = profile.nome.orEmpty()
                                        nameError = null
                                        showEditName = true
                                    }
                                )
                                HorizontalDivider(color = Color.LightGray.copy(0.3f))
                                ProfileItemRow(
                                    label = "E-mail",
                                    value = profile.email.orEmpty(),
                                    onEditClick = {
                                        tempEmail = profile.email.orEmpty()
                                        currentPasswordForEmail = ""
                                        emailError = null
                                        showEditEmail = true
                                    }
                                )
                                HorizontalDivider(color = Color.LightGray.copy(0.3f))
                                ProfileItemRow(
                                    label = "Senha",
                                    value = "••••••••",
                                    onEditClick = {
                                        oldPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        passwordError = null
                                        showEditPassword = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // DIÁLOGOS DE EDIÇÃO
            if (showEditName) {
                EditDialog(
                    title = "Editar Nome",
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = "Seu nome completo",
                    error = nameError,
                    onConfirm = {
                        if (tempName.trim().length < 3) {
                            nameError = "Nome muito curto"
                        } else {
                            viewModel.updateName(tempName.trim()) { showEditName = false }
                        }
                    },
                    onDismiss = { showEditName = false }
                )
            }

            if (showEditEmail) {
                EmailEditDialog(
                    newEmail = tempEmail,
                    currentPassword = currentPasswordForEmail,
                    onEmailChange = { tempEmail = it },
                    onPasswordChange = { currentPasswordForEmail = it },
                    error = emailError,
                    onConfirm = {
                        when {
                            tempEmail.isBlank() -> emailError = "Digite o novo e-mail"
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(tempEmail).matches() -> emailError = "E-mail inválido"
                            currentPasswordForEmail.isBlank() -> emailError = "Digite sua senha atual"
                            else -> {
                                viewModel.updateEmailWithPassword(tempEmail.trim(), currentPasswordForEmail) { success ->
                                    if (success) showEditEmail = false
                                    else emailError = "Senha incorreta"
                                }
                            }
                        }
                    },
                    onDismiss = { showEditEmail = false }
                )
            }

            if (showEditPassword) {
                PasswordEditDialog(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onOldPasswordChange = { oldPassword = it },
                    onNewPasswordChange = { newPassword = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    error = passwordError,
                    onConfirm = {
                        when {
                            oldPassword.isBlank() -> passwordError = "Digite a senha atual"
                            newPassword.length < 6 -> passwordError = "Nova senha muito curta"
                            newPassword != confirmPassword -> passwordError = "As senhas não coincidem"
                            else -> {
                                viewModel.updatePassword(oldPassword, newPassword) { success ->
                                    if (success) showEditPassword = false
                                    else passwordError = "Senha atual incorreta"
                                }
                            }
                        }
                    },
                    onDismiss = { showEditPassword = false }
                )
            }
        }
    }
}

@Composable
private fun ProfileItemRow(label: String, value: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(
                value,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF0066FF))
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White, // FUNDO BRANCO!
        title = { Text(title, fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color.Black.copy(0.5f)) },
                colors = blackTextFieldColors(),
                isError = error != null,
                supportingText = { error?.let { Text(it, color = Color.Red) } },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Salvar", color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Red) }
        }
    )
}

@Composable
private fun EmailEditDialog(
    newEmail: String,
    currentPassword: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Alterar E-mail", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = onEmailChange,
                    label = { Text("Novo e-mail") },
                    placeholder = { Text("novo@email.com") },
                    colors = blackTextFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Sua senha atual") },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Default.Close else Icons.Default.Close, null, tint = Color.Gray)
                        }
                    },
                    colors = blackTextFieldColors(),
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = Color.Red) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Alterar", color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Red) }
        }
    )
}

@Composable
private fun PasswordEditDialog(
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Alterar Senha", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = onOldPasswordChange,
                    label = { Text("Senha atual") },
                    visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { VisibilityIcon(showOld) { showOld = !showOld } },
                    colors = blackTextFieldColors()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nova senha") },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { VisibilityIcon(showNew) { showNew = !showNew } },
                    colors = blackTextFieldColors()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar nova senha") },
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { VisibilityIcon(showConfirm) { showConfirm = !showConfirm } },
                    colors = blackTextFieldColors(),
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = Color.Red) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Alterar", color = Color(0xFF0066FF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun VisibilityIcon(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Default.Close else Icons.Default.Close,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun blackTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black.copy(alpha = 0.6f),
    errorTextColor = Color.Red,

    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White,

    cursorColor = Color.Black,
    errorCursorColor = Color.Red,

    focusedIndicatorColor = Color.Black,
    unfocusedIndicatorColor = Color.Black,
    disabledIndicatorColor = Color.Black.copy(alpha = 0.4f),
    errorIndicatorColor = Color.Red,

    focusedLabelColor = Color.Black,
    unfocusedLabelColor = Color.Black.copy(alpha = 0.7f),
    disabledLabelColor = Color.Black.copy(alpha = 0.6f),
    errorLabelColor = Color.Red,

    focusedLeadingIconColor = Color.Black,
    unfocusedLeadingIconColor = Color.Black.copy(alpha = 0.7f),
    focusedTrailingIconColor = Color.Black,
    unfocusedTrailingIconColor = Color.Black.copy(alpha = 0.7f)
)