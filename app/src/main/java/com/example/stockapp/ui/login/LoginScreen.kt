package com.example.stockapp.ui.login

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.R
import com.example.stockapp.ui.common.FooterBranding
import com.example.stockapp.ui.common.SoftInputAdjustNothingMode
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.StockAppTopBar
import com.example.stockapp.ui.common.TidyTextField
import com.example.stockapp.ui.common.WindowSoftInputModeEffect

private enum class AuthMode { LOGIN, CREATE }

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onCreateAccount: (String, String, String, String, (Result<Unit>) -> Unit) -> Unit,
    showError: Boolean,
    loginLoading: Boolean,
    createLoading: Boolean
) {
    WindowSoftInputModeEffect(SoftInputAdjustNothingMode)

    var authModeName by rememberSaveable { mutableStateOf(AuthMode.LOGIN.name) }
    val authMode = AuthMode.valueOf(authModeName)

    var loginUid by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var loginInputError by rememberSaveable { mutableStateOf(false) }

    var createUid by rememberSaveable { mutableStateOf("") }
    var createEmail by rememberSaveable { mutableStateOf("") }
    var createPassword by rememberSaveable { mutableStateOf("") }
    var createSecurityKey by rememberSaveable { mutableStateOf("") }
    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var createInputError by rememberSaveable { mutableStateOf(false) }

    var lastActionAtMs by remember { mutableLongStateOf(0L) }

    fun switchMode(target: AuthMode) {
        authModeName = target.name
        loginInputError = false
        createInputError = false
        createError = null
        onInputChanged()
    }

    fun shouldThrottle(): Boolean {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastActionAtMs < 300L) return true
        lastActionAtMs = nowMs
        return false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            StockAppTopBar(title = "STOCK TAKE", logoResId = R.drawable.logo)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (authMode) {
                        AuthMode.LOGIN -> "Welcome Back"
                        AuthMode.CREATE -> "Create Account"
                    },
                    color = StockAppColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when (authMode) {
                        AuthMode.LOGIN -> "Sign in to manage your stock data"
                        AuthMode.CREATE -> "Set your credentials to manage stock data"
                    },
                    color = StockAppColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                        .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface)
                ) {
                    val formSpacing = 10.dp
                    val formVerticalPadding = 16.dp
                    val formHorizontalPadding = 20.dp
                    val fieldHeight = 40.dp
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = formHorizontalPadding, vertical = formVerticalPadding),
                        verticalArrangement = Arrangement.spacedBy(formSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (authMode) {
                            AuthMode.LOGIN -> {
                                if (showError) ErrorText("Incorrect UID or password")
                                if (loginInputError) ErrorText("UID and password are required")
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AuthFieldLabel(Icons.Filled.Person, "UID")
                                    TidyTextField(
                                        value = loginUid,
                                        onValueChange = {
                                            loginUid = it
                                            loginInputError = false
                                            onInputChanged()
                                        },
                                        placeholder = "Enter UID",
                                        fieldHeight = fieldHeight,
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AuthFieldLabel(Icons.Filled.Lock, "Password")
                                    TidyTextField(
                                        value = loginPassword,
                                        onValueChange = {
                                            loginPassword = it
                                            loginInputError = false
                                            onInputChanged()
                                        },
                                        placeholder = "Enter Password",
                                        fieldHeight = fieldHeight,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                PrimaryActionButton("Login", Icons.Filled.Login, !loginLoading) {
                                    val uid = loginUid.trim()
                                    val password = loginPassword.trim()
                                    if (uid.isBlank() || password.isBlank()) {
                                        loginInputError = true
                                        return@PrimaryActionButton
                                    }
                                    if (shouldThrottle()) return@PrimaryActionButton
                                    onLogin(uid, password)
                                }
                                SecondaryActionButton("Create Account", Icons.Filled.PersonAdd, !loginLoading) {
                                    if (shouldThrottle()) return@SecondaryActionButton
                                    switchMode(AuthMode.CREATE)
                                }
                            }

                            AuthMode.CREATE -> {
                                createError?.let { ErrorText(it) }
                                if (createInputError) ErrorText("All fields are required")
                                
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    AuthFieldLabel(Icons.Filled.Person, "UID")
                                    TidyTextField(
                                        value = createUid,
                                        onValueChange = {
                                            createUid = it
                                            createInputError = false
                                            createError = null
                                        },
                                        placeholder = "Enter UID",
                                        fieldHeight = fieldHeight,
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    AuthFieldLabel(Icons.Filled.Email, "Email")
                                    TidyTextField(
                                        value = createEmail,
                                        onValueChange = {
                                            createEmail = it
                                            createInputError = false
                                            createError = null
                                        },
                                        placeholder = "Enter Email",
                                        fieldHeight = fieldHeight,
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    AuthFieldLabel(Icons.Filled.Lock, "Password")
                                    TidyTextField(
                                        value = createPassword,
                                        onValueChange = {
                                            createPassword = it
                                            createInputError = false
                                            createError = null
                                        },
                                        placeholder = "Enter Password",
                                        fieldHeight = fieldHeight,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    AuthFieldLabel(Icons.Filled.VpnKey, "Security Key")
                                    TidyTextField(
                                        value = createSecurityKey,
                                        onValueChange = {
                                            createSecurityKey = it
                                            createInputError = false
                                            createError = null
                                        },
                                        placeholder = "Enter Security Key",
                                        fieldHeight = fieldHeight,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        matchCardSurface = true
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                PrimaryActionButton(
                                    text = "Create Account",
                                    icon = Icons.Filled.PersonAdd,
                                    enabled = !createLoading
                                ) {
                                    val uid = createUid.trim()
                                    val email = createEmail.trim()
                                    val password = createPassword.trim()
                                    val securityKey = createSecurityKey.trim()
                                    if (uid.isBlank() || email.isBlank() || password.isBlank() || securityKey.isBlank()) {
                                        createInputError = true
                                        return@PrimaryActionButton
                                    }
                                    if (!looksLikeEmail(email)) {
                                        createError = "Enter a valid email address."
                                        return@PrimaryActionButton
                                    }
                                    if (shouldThrottle()) return@PrimaryActionButton
                                    onCreateAccount(uid, email, password, securityKey) { result ->
                                        result.onSuccess {
                                            createEmail = ""
                                            createSecurityKey = ""
                                            switchMode(AuthMode.LOGIN)
                                        }.onFailure { createError = it.message ?: "Account creation failed." }
                                    }
                                }
                                SecondaryActionButton("Back To Login", Icons.Filled.ArrowBack, !createLoading) {
                                    if (shouldThrottle()) return@SecondaryActionButton
                                    switchMode(AuthMode.LOGIN)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        FooterBranding(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    containerColor: Color = StockAppColors.AccentCyan,
    buttonHeight: Dp = 42.dp,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .shadow(4.dp, RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = StockAppColors.DisabledSurface
        )
    ) {
        Icon(icon, contentDescription = null, tint = StockAppColors.TextPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = StockAppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, StockAppColors.CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = StockAppColors.AccentCyan
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StockAppColors.AccentCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = StockAppColors.AccentCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, color = StockAppColors.Danger, fontSize = 13.sp)
}

@Composable
private fun AuthFieldLabel(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = StockAppColors.AccentCyan, modifier = Modifier.size(16.dp))
        }
        Text(text = label, color = StockAppColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun looksLikeEmail(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return false
    val atIndex = trimmed.indexOf('@')
    val dotIndex = trimmed.lastIndexOf('.')
    return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.lastIndex
}
