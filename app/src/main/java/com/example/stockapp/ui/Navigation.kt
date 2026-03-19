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

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as StockApplication
    val stockViewModel: StockViewModel = viewModel(
        factory = StockViewModelFactory(application.repository)
    )
    val navigateAndClearBackStack: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginResult by stockViewModel.loginResult.collectAsState()
            val loginInProgress by stockViewModel.loginInProgress.collectAsState()
            var showError by remember { mutableStateOf(false) }
            var currentUid by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                stockViewModel.clearActiveUser()
            }

            LaunchedEffect(loginResult) {
                when (loginResult) {
                    true -> {
                        showError = false
                        stockViewModel.setActiveUser(currentUid)
                        navigateAndClearBackStack("home/$currentUid")
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
                showError = showError,
                isLoading = loginInProgress
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
            LaunchedEffect(uid) {
                stockViewModel.setActiveUser(uid)
            }
            HomeScreen(
                loggedInUser = uid,
                onCreateStockCard = { navController.navigate("create_stock/$uid") },
                onViewStockCard = { navController.navigate("view_stock_card/$uid") },
                onShare = { navController.navigate("view_stock_share/$uid") },
                onLogout = {
                    stockViewModel.clearActiveUser()
                    stockViewModel.resetLoginResult()
                    navigateAndClearBackStack("login")
                }
            )
        }
        composable("create_stock/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            LaunchedEffect(uid) {
                stockViewModel.setActiveUser(uid)
            }
            CreateStockCardScreen(
                loggedInUser = uid,
                onBack = { navController.popBackStack() },
                createStockViewModel = viewModel(
                    factory = CreateStockViewModelFactory(application.repository, uid)
                )
            )
        }
        composable("view_stock_card/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            LaunchedEffect(uid) {
                stockViewModel.setActiveUser(uid)
            }
            ViewStockCardScreen(
                stockViewModel = stockViewModel,
                onBack = { navController.popBackStack() },
                shareMode = false
            )
        }
        composable("view_stock_share/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            LaunchedEffect(uid) {
                stockViewModel.setActiveUser(uid)
            }
            ViewStockCardScreen(
                stockViewModel = stockViewModel,
                onBack = { navController.popBackStack() },
                shareMode = true
            )
        }
    }
}
