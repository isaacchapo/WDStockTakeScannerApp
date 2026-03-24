package com.example.stockapp.ui.login

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Lock
import com.example.stockapp.R
import com.example.stockapp.ui.common.StockAppBackground
import com.example.stockapp.ui.common.ScreenAppear
import com.example.stockapp.ui.common.StockAppTopBar
import com.example.stockapp.ui.common.TidyTextField
import com.example.stockapp.ui.common.StockAppColors

@Composable
fun CreateAccountScreen(
    onCreateAccount: (String, String, (Boolean) -> Unit) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean
) {
    var uid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showInputError by rememberSaveable { mutableStateOf(false) }
    var createAccountError by rememberSaveable { mutableStateOf<String?>(null) }
    var lastCreateActionAtMs by remember { mutableLongStateOf(0L) }
    var lastBackActionAtMs by remember { mutableLongStateOf(0L) }

    StockAppBackground {
        ScreenAppear {
            Column(modifier = Modifier.fillMaxSize()) {
                StockAppTopBar(title = "STOCK TAKE", logoResId = R.drawable.logo)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(StockAppColors.AccentCyan.copy(alpha = 0.2f))
                            .border(2.dp, StockAppColors.AccentCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "User",
                            tint = StockAppColors.AccentCyan,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Create Account",
                        color = StockAppColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set your credentials to manage stock data",
                        color = StockAppColors.TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                            createAccountError?.let { error ->
                                Text(
                                    text = error,
                                    color = StockAppColors.Danger,
                                    fontSize = 13.sp
                                )
                            }
                            if (showInputError) {
                                Text(
                                    text = "UID and password are required",
                                    color = StockAppColors.Danger,
                                    fontSize = 13.sp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = StockAppColors.AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "UID",
                                        color = StockAppColors.TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TidyTextField(
                                    value = uid,
                                    onValueChange = {
                                        uid = it
                                        showInputError = false
                                        createAccountError = null
                                    },
                                    placeholder = "Enter UID",
                                    modifier = Modifier.fillMaxWidth(),
                                    matchCardSurface = true
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = StockAppColors.AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Password",
                                        color = StockAppColors.TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TidyTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        showInputError = false
                                        createAccountError = null
                                    },
                                    placeholder = "Enter Password",
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    matchCardSurface = true
                                )
                            }

                            Button(
                                onClick = {
                                    val trimmedUid = uid.trim()
                                    val trimmedPassword = password.trim()
                                    if (trimmedUid.isBlank() || trimmedPassword.isBlank()) {
                                        showInputError = true
                                        return@Button
                                    }
                                    val nowMs = SystemClock.elapsedRealtime()
                                    if (nowMs - lastCreateActionAtMs < 300L) return@Button
                                    lastCreateActionAtMs = nowMs
                                    showInputError = false
                                    createAccountError = null
                                    onCreateAccount(trimmedUid, trimmedPassword) { created ->
                                        if (!created) {
                                            createAccountError = "That UID already exists. Use a different UID."
                                        }
                                    }
                                },
                                enabled = !isLoading,
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
                                    if (nowMs - lastBackActionAtMs < 300L) return@OutlinedButton
                                    lastBackActionAtMs = nowMs
                                    onBackToLogin()
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally),
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
                        }
                    }
                }
            }
        }
    }
}
