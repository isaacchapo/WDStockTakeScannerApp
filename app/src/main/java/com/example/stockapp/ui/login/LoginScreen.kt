package com.example.stockapp.ui.login

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.R
import com.example.stockapp.ui.common.FooterBranding
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.StockAppTopBar
import com.example.stockapp.ui.common.TidyTextField

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onInputChanged: () -> Unit,
    onCreateAccount: (String, String, (Boolean) -> Unit) -> Unit,
    showError: Boolean,
    loginLoading: Boolean,
    createLoading: Boolean
) {
    var isCreateAccountMode by rememberSaveable { mutableStateOf(false) }

    var loginUid by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var showLoginInputError by rememberSaveable { mutableStateOf(false) }

    var createUid by rememberSaveable { mutableStateOf("") }
    var createPassword by rememberSaveable { mutableStateOf("") }
    var showCreateInputError by rememberSaveable { mutableStateOf(false) }
    var createAccountError by rememberSaveable { mutableStateOf<String?>(null) }

    var lastLoginActionAtMs by remember { mutableLongStateOf(0L) }
    var lastCreateActionAtMs by remember { mutableLongStateOf(0L) }
    var lastModeSwitchActionAtMs by remember { mutableLongStateOf(0L) }

    Column(modifier = Modifier.fillMaxSize()) {
        StockAppTopBar(title = "STOCK TAKE", logoResId = R.drawable.logo)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isCreateAccountMode) "Create Account" else "Welcome Back",
                        color = StockAppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCreateAccountMode) {
                            "Set your credentials to manage stock data"
                        } else {
                            "Sign in to manage your stock data"
                        },
                        color = StockAppColors.TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isCreateAccountMode) {
                                createAccountError?.let { error ->
                                    Text(
                                        text = error,
                                        color = StockAppColors.Danger,
                                        fontSize = 13.sp
                                    )
                                }
                                if (showCreateInputError) {
                                    Text(
                                        text = "UID and password are required",
                                        color = StockAppColors.Danger,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                if (showError) {
                                    Text(
                                        text = "Incorrect UID or password",
                                        color = StockAppColors.Danger,
                                        fontSize = 13.sp
                                    )
                                }
                                if (showLoginInputError) {
                                    Text(
                                        text = "UID and password are required",
                                        color = StockAppColors.Danger,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            AuthFieldLabel(
                                icon = Icons.Filled.Person,
                                label = "UID"
                            )
                            TidyTextField(
                                value = if (isCreateAccountMode) createUid else loginUid,
                                onValueChange = { value ->
                                    if (isCreateAccountMode) {
                                        createUid = value
                                        showCreateInputError = false
                                        createAccountError = null
                                    } else {
                                        loginUid = value
                                        showLoginInputError = false
                                        onInputChanged()
                                    }
                                },
                                placeholder = "Enter UID",
                                modifier = Modifier.fillMaxWidth(),
                                matchCardSurface = true
                            )

                            AuthFieldLabel(
                                icon = Icons.Filled.Lock,
                                label = "Password"
                            )
                            TidyTextField(
                                value = if (isCreateAccountMode) createPassword else loginPassword,
                                onValueChange = { value ->
                                    if (isCreateAccountMode) {
                                        createPassword = value
                                        showCreateInputError = false
                                        createAccountError = null
                                    } else {
                                        loginPassword = value
                                        showLoginInputError = false
                                        onInputChanged()
                                    }
                                },
                                placeholder = "Enter Password",
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                matchCardSurface = true
                            )

                            if (isCreateAccountMode) {
                                Button(
                                    onClick = {
                                        val trimmedUid = createUid.trim()
                                        val trimmedPassword = createPassword.trim()
                                        if (trimmedUid.isBlank() || trimmedPassword.isBlank()) {
                                            showCreateInputError = true
                                            return@Button
                                        }

                                        val nowMs = SystemClock.elapsedRealtime()
                                        if (nowMs - lastCreateActionAtMs < 300L) return@Button
                                        lastCreateActionAtMs = nowMs
                                        showCreateInputError = false
                                        createAccountError = null

                                        onCreateAccount(trimmedUid, trimmedPassword) { created ->
                                            if (created) {
                                                isCreateAccountMode = false
                                                createAccountError = null
                                                showCreateInputError = false
                                            } else {
                                                createAccountError = "That UID already exists. Use a different UID."
                                            }
                                        }
                                    },
                                    enabled = !createLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(50),
                                            ambientColor = StockAppColors.SoftGlowCyan.copy(alpha = 0.2f),
                                            spotColor = StockAppColors.SoftGlowCyan.copy(alpha = 0.35f)
                                        ),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StockAppColors.AccentCyan,
                                        disabledContainerColor = StockAppColors.DisabledSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonAdd,
                                        contentDescription = null,
                                        tint = StockAppColors.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Create Account",
                                        color = StockAppColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val nowMs = SystemClock.elapsedRealtime()
                                        if (nowMs - lastModeSwitchActionAtMs < 300L) return@OutlinedButton
                                        lastModeSwitchActionAtMs = nowMs
                                        isCreateAccountMode = false
                                        showCreateInputError = false
                                        createAccountError = null
                                        onInputChanged()
                                    },
                                    enabled = !createLoading,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, StockAppColors.CardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = StockAppColors.CardSurface,
                                        contentColor = StockAppColors.AccentCyan
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back To Login",
                                        tint = StockAppColors.AccentCyan,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "Back To Login",
                                        color = StockAppColors.AccentCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val trimmedUid = loginUid.trim()
                                        val trimmedPassword = loginPassword.trim()
                                        if (trimmedUid.isBlank() || trimmedPassword.isBlank()) {
                                            showLoginInputError = true
                                            return@Button
                                        }

                                        val nowMs = SystemClock.elapsedRealtime()
                                        if (nowMs - lastLoginActionAtMs < 300L) return@Button
                                        lastLoginActionAtMs = nowMs
                                        showLoginInputError = false
                                        onLogin(trimmedUid, trimmedPassword)
                                    },
                                    enabled = !loginLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(50),
                                            ambientColor = StockAppColors.SoftGlowCyan.copy(alpha = 0.2f),
                                            spotColor = StockAppColors.SoftGlowCyan.copy(alpha = 0.35f)
                                        ),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StockAppColors.AccentCyan,
                                        disabledContainerColor = StockAppColors.DisabledSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Login,
                                        contentDescription = null,
                                        tint = StockAppColors.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Login",
                                        color = StockAppColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val nowMs = SystemClock.elapsedRealtime()
                                        if (nowMs - lastModeSwitchActionAtMs < 300L) return@OutlinedButton
                                        lastModeSwitchActionAtMs = nowMs
                                        showLoginInputError = false
                                        createAccountError = null
                                        onInputChanged()
                                        isCreateAccountMode = true
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, StockAppColors.CardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = StockAppColors.CardSurface,
                                        contentColor = StockAppColors.AccentCyan
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonAdd,
                                        contentDescription = null,
                                        tint = StockAppColors.AccentCyan,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "Create Account",
                                        color = StockAppColors.AccentCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

            FooterBranding()
        }
    }
@Composable
private fun AuthFieldLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StockAppColors.AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = StockAppColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
