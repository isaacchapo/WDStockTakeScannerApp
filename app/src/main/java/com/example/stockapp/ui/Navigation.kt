package com.example.stockapp.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.stockapp.StockApplication
import com.example.stockapp.ui.home.CreateStockCardScreen
import com.example.stockapp.ui.home.CreateStockViewModelFactory
import com.example.stockapp.ui.home.HomeScreen
import com.example.stockapp.ui.home.ViewStockCardScreen
import com.example.stockapp.ui.common.SoftInputAdjustNothingMode
import com.example.stockapp.ui.common.SoftInputAdjustResizeMode
import com.example.stockapp.ui.common.WindowSoftInputModeEffect
import com.example.stockapp.ui.login.CreateAccountScreen
import com.example.stockapp.ui.login.LoginScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val application = LocalContext.current.applicationContext as StockApplication
    val stockViewModel: StockViewModel = viewModel(
        factory = StockViewModelFactory(application.repository)
    )
    val activeUserUid by stockViewModel.activeUserUid.collectAsState()
    var lastNavigationAtMs by remember { mutableLongStateOf(0L) }
    var navigationInProgress by remember { mutableStateOf(false) }
    val softInputMode = when (currentBackStackEntry?.destination?.route) {
        "create_stock/{uid}" -> SoftInputAdjustNothingMode
        else -> SoftInputAdjustResizeMode
    }

    WindowSoftInputModeEffect(softInputMode)

    LaunchedEffect(currentBackStackEntry) {
        navigationInProgress = false
    }

    fun isAlreadyAtRoute(route: String): Boolean {
        val currentEntry = navController.currentBackStackEntry ?: return false
        val currentRoute = currentEntry.destination.route ?: return false
        val currentUid = currentEntry.arguments?.getString("uid")
        return when (currentRoute) {
            "login" -> route == "login"
            "create_account" -> route == "create_account"
            "home/{uid}" -> route.startsWith("home/") && currentUid == route.removePrefix("home/")
            "create_stock/{uid}" -> route.startsWith("create_stock/") && currentUid == route.removePrefix("create_stock/")
            "view_stock_card/{uid}" -> route.startsWith("view_stock_card/") && currentUid == route.removePrefix("view_stock_card/")
            "view_stock_share/{uid}" -> route.startsWith("view_stock_share/") && currentUid == route.removePrefix("view_stock_share/")
            else -> false
        }
    }

    fun canNavigateNow(force: Boolean = false): Boolean {
        val currentEntry = navController.currentBackStackEntry ?: return false
        if (navigationInProgress) return false
        val nowMs = SystemClock.elapsedRealtime()
        if (!force) {
            if (nowMs - lastNavigationAtMs < NAVIGATION_DEBOUNCE_MS) return false
        }
        if (currentEntry.lifecycle.currentState != Lifecycle.State.RESUMED) return false
        lastNavigationAtMs = nowMs
        return true
    }

    fun navigateAndClearBackStack(route: String, force: Boolean = false) {
        if (isAlreadyAtRoute(route)) return
        if (!canNavigateNow(force)) return
        navigationInProgress = true
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    val navigateSingleTop: (String) -> Unit = fun(route: String) {
        if (isAlreadyAtRoute(route)) return
        if (!canNavigateNow()) return
        navigationInProgress = true
        navController.navigate(route) {
            launchSingleTop = true
        }
    }
    val popBackStackSafely: () -> Unit = fun() {
        if (navController.previousBackStackEntry == null) return
        if (!canNavigateNow()) return
        navigationInProgress = true
        if (!navController.popBackStack()) {
            navigationInProgress = false
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
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
                onInputChanged = {
                    showError = false
                    stockViewModel.resetLoginResult()
                },
                onCreateAccount = { 
                    showError = false
                    stockViewModel.resetLoginResult()
                    navigateSingleTop("create_account")
                },
                showError = showError,
                isLoading = loginInProgress
            )
        }
        composable("create_account") {
            val createInProgress by stockViewModel.createInProgress.collectAsState()
            CreateAccountScreen(
                onCreateAccount = { uid, password, onComplete ->
                    stockViewModel.createUser(uid, password) { created ->
                        if (created) {
                            navigateAndClearBackStack("login", force = true)
                        }
                        onComplete(created)
                    }
                },
                onBackToLogin = { navigateAndClearBackStack("login", force = true) },
                isLoading = createInProgress
            )
        }
        composable("home/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val isAuthorized = activeUserUid == uid && uid.isNotBlank()
            LaunchedEffect(uid, activeUserUid) {
                if (!isAuthorized) {
                    navigateAndClearBackStack("login", force = true)
                }
            }
            if (!isAuthorized) return@composable
            HomeScreen(
                loggedInUser = uid,
                onCreateStockCard = { navigateSingleTop("create_stock/$uid") },
                onViewStockCard = { navigateSingleTop("view_stock_card/$uid") },
                onShare = { navigateSingleTop("view_stock_share/$uid") },
                onLogout = {
                    stockViewModel.clearActiveUser()
                    stockViewModel.resetLoginResult()
                    navigateAndClearBackStack("login", force = true)
                }
            )
        }
        composable("create_stock/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val isAuthorized = activeUserUid == uid && uid.isNotBlank()
            LaunchedEffect(uid, activeUserUid) {
                if (!isAuthorized) {
                    navigateAndClearBackStack("login", force = true)
                }
            }
            if (!isAuthorized) return@composable
            CreateStockCardScreen(
                loggedInUser = uid,
                onBack = { popBackStackSafely() },
                createStockViewModel = viewModel(
                    factory = CreateStockViewModelFactory(application.repository, uid)
                )
            )
        }
        composable("view_stock_card/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val isAuthorized = activeUserUid == uid && uid.isNotBlank()
            LaunchedEffect(uid, activeUserUid) {
                if (!isAuthorized) {
                    navigateAndClearBackStack("login", force = true)
                }
            }
            if (!isAuthorized) return@composable
            ViewStockCardScreen(
                stockViewModel = stockViewModel,
                onBack = { popBackStackSafely() },
                shareMode = false
            )
        }
        composable("view_stock_share/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val isAuthorized = activeUserUid == uid && uid.isNotBlank()
            LaunchedEffect(uid, activeUserUid) {
                if (!isAuthorized) {
                    navigateAndClearBackStack("login", force = true)
                }
            }
            if (!isAuthorized) return@composable
            ViewStockCardScreen(
                stockViewModel = stockViewModel,
                onBack = { popBackStackSafely() },
                shareMode = true
            )
        }
    }
}

private const val NAVIGATION_DEBOUNCE_MS = 250L
