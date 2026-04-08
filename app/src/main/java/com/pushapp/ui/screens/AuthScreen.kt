package com.pushapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.ui.theme.*
import com.pushapp.viewmodel.AuthState
import com.pushapp.viewmodel.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    var isLogin  by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    val pillShape = RoundedCornerShape(50.dp)
    val fieldShape = RoundedCornerShape(20.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor    = AppAccent,
        unfocusedBorderColor  = AppSurfaceBright,
        focusedLabelColor     = AppAccent,
        unfocusedLabelColor   = AppOnSurfaceVar,
        cursorColor           = AppAccent,
        focusedTextColor      = AppOnBackground,
        unfocusedTextColor    = AppOnBackground,
        focusedContainerColor = AppSurfaceVariant,
        unfocusedContainerColor = AppSurfaceVariant,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Декоративный градиент сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppAccentDim, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(80.dp))

            Text(text = "💪", fontSize = 80.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "PushApp",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = AppAccent
            )
            Text(
                text = "трекер тренировок",
                style = MaterialTheme.typography.bodyLarge,
                color = AppOnSurfaceVar
            )

            Spacer(Modifier.height(56.dp))

            OutlinedTextField(
                value       = username,
                onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                label       = { Text("Логин") },
                modifier    = Modifier.fillMaxWidth(),
                singleLine  = true,
                shape       = fieldShape,
                colors      = fieldColors
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value       = password,
                onValueChange = { password = it },
                label       = { Text("Пароль") },
                modifier    = Modifier.fillMaxWidth(),
                singleLine  = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape       = fieldShape,
                colors      = fieldColors
            )

            if (authState is AuthState.Error) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = (authState as AuthState.Error).message,
                    color = AppError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(32.dp))

            // Основная кнопка — заливка лаймом
            Button(
                onClick = {
                    if (isLogin) authViewModel.login(username, password)
                    else authViewModel.register(username, password)
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                enabled  = authState !is AuthState.Loading && username.isNotBlank() && password.isNotBlank(),
                shape    = pillShape,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = AppAccent,
                    contentColor           = AppBackground,
                    disabledContainerColor = AppAccentDim,
                    disabledContentColor   = AppOnSurfaceVar
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color       = AppBackground
                    )
                } else {
                    Text(
                        text       = if (isLogin) "Войти" else "Зарегистрироваться",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Второстепенная кнопка — обводка
            OutlinedButton(
                onClick  = { isLogin = !isLogin; authViewModel.resetState() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = pillShape,
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppOnSurfaceVar
                ),
                border   = androidx.compose.foundation.BorderStroke(1.dp, AppSurfaceBright)
            ) {
                Text(
                    text     = if (isLogin) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти",
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
