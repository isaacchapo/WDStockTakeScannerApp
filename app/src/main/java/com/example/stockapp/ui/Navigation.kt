package com.example.stockapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stockapp.StockApplication
import com.example.stockapp.ui.home.CreateStockCardScreen
import com.example.stockapp.ui.home.CreateStockViewModelFactory
import com.example.stockapp.ui.home.HomeScreen
import com.example.stockapp.ui.home.ViewStockCardScreen
import com.example.stockapp.ui.login.CreateAccountScreen
import com.example.stockapp.ui.login.LoginScreen
import com.example.stockapp.ui.sharing.shareStockDataAsPdf

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as StockApplication
    val stockViewModel: StockViewModel = viewModel(
        factory = StockViewModelFactory(application.repository)
    )

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginResult by stockViewModel.loginResult.collectAsState()
            var showError by remember { mutableStateOf(false) }
            var currentUid by remember { mutableStateOf("") }

            LaunchedEffect(loginResult) {
                when (loginResult) {
                    true -> {
                        showError = false
                        navController.navigate("home/$currentUid") { 
                            popUpTo("login") { inclusive = true }
                        }
                        stockViewModel.resetLoginResult()
                    }
                    false -> {
                        showError = true
                    }
                    null -> { /* Do nothing */ }
                }
            }

            LoginScreen(
                onLogin = { uid, password ->
                    currentUid = uid
                    stockViewModel.loginUser(uid, password)
                },
                onCreateAccount = { 
                    showError = false
                    stockViewModel.resetLoginResult()
                    navController.navigate("create_account") 
                },
                showError = showError
            )
        }
        composable("create_account") {
            CreateAccountScreen { uid, password ->
                stockViewModel.createUser(uid, password)
                navController.popBackStack()
            }
        }
        composable("home/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val context = LocalContext.current
            val stockItems by stockViewModel.allStockItems.collectAsState()
            HomeScreen(
                loggedInUser = uid,
                onCreateStockCard = { navController.navigate("create_stock/$uid") },
                onViewStockCard = { navController.navigate("view_stock_card") },
                onShare = { shareStockDataAsPdf(context, stockItems) },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("create_stock/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            CreateStockCardScreen(
                loggedInUser = uid,
                createStockViewModel = viewModel(factory = CreateStockViewModelFactory(application.repository))
            )
        }
        composable("view_stock_card") {
            ViewStockCardScreen(stockViewModel = stockViewModel)
        }
    }
}