package com.suvrnika.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvrnika.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suvrnika.app.data.Repository
import com.suvrnika.app.payment.PaymentCoordinator
import com.suvrnika.app.ui.components.LocalBagChanged
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalPayments
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.LocalToast
import com.suvrnika.app.ui.screens.AccountScreen
import com.suvrnika.app.ui.screens.BagScreen
import com.suvrnika.app.ui.screens.CalculatorScreen
import com.suvrnika.app.ui.screens.HelpScreen
import com.suvrnika.app.ui.screens.HistoryScreen
import com.suvrnika.app.ui.screens.HomeScreen
import com.suvrnika.app.ui.screens.InvoicesScreen
import com.suvrnika.app.ui.screens.JournalScreen
import com.suvrnika.app.ui.screens.LoginScreen
import com.suvrnika.app.ui.screens.OfferScreen
import com.suvrnika.app.ui.screens.OffersScreen
import com.suvrnika.app.ui.screens.OrderScreen
import com.suvrnika.app.ui.screens.OrdersScreen
import com.suvrnika.app.ui.screens.PaymentMethodsScreen
import com.suvrnika.app.ui.screens.PlansScreen
import com.suvrnika.app.ui.screens.PostScreen
import com.suvrnika.app.ui.screens.ProductScreen
import com.suvrnika.app.ui.screens.ProfileScreen
import com.suvrnika.app.ui.screens.ShopScreen
import com.suvrnika.app.ui.screens.StoresScreen
import com.suvrnika.app.ui.screens.WalletScreen
import com.suvrnika.app.ui.screens.WishlistScreen
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.ui.theme.SuvrnikaTheme
import com.suvrnika.app.util.inr
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("home", "Home", Icons.Outlined.Home),
    Tab("shop", "Shop", Icons.Outlined.Storefront),
    Tab("wishlist", "Wishlist", Icons.Outlined.FavoriteBorder),
    Tab("bag", "Bag", Icons.Outlined.ShoppingBag),
    Tab("account", "Account", Icons.Outlined.Person),
)

private val TITLES = mapOf(
    "product" to "", "login" to "Sign in", "wallet" to "Wallet", "history" to "Complete history", "plans" to "Savings plans",
    "orders" to "Orders", "order" to "Order", "invoices" to "Invoices", "payments" to "Payment methods", "profile" to "Profile & KYC",
    "stores" to "Find a showroom", "calculator" to "Metal value calculator", "offers" to "Current offers", "offer" to "Offer",
    "journal" to "Journal", "post" to "Journal", "help" to "Help & support",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuvrnikaRoot(repo: Repository, payments: PaymentCoordinator) = SuvrnikaTheme {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = remember { { msg -> scope.launch { snack.showSnackbar(msg) } } }
    val signedIn by repo.tokens.signedIn.collectAsState()
    val bagTick = remember { mutableIntStateOf(0) }
    val config by androidx.compose.runtime.produceState<com.suvrnika.app.data.AppConfig?>(null, signedIn) {
        value = runCatching { repo.call { config() } }.getOrNull()
    }
    val bagCount by androidx.compose.runtime.produceState(0, signedIn, bagTick.intValue) {
        value = runCatching { repo.call { bag() }.count }.getOrDefault(0)
    }

    // Coming back to the app: refresh the badge and report any payment that finished while we were away.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) scope.launch {
                bagTick.intValue++
                payments.reconcilePending()?.let { s ->
                    toast(if (s.status == "PAID") "Your payment of ${inr(s.amountPaise)} was successful." else "A payment was not completed. Nothing was charged.")
                }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val mock by payments.mockRequest.collectAsState()
    mock?.let { m ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Development payment") },
            text = { Text("${m.description}\n\nAmount: ${inr(m.amountPaise)}\n\nThis server runs in mock-payment mode; no money moves.") },
            confirmButton = { TextButton(onClick = { payments.answerMock(true) }) { Text("Pay ${inr(m.amountPaise)}") } },
            dismissButton = { TextButton(onClick = { payments.answerMock(false) }) { Text("Cancel") } },
        )
    }

    CompositionLocalProvider(
        LocalRepo provides repo, LocalPayments provides payments, LocalNav provides nav, LocalToast provides toast,
        LocalConfig provides config, LocalBagChanged provides { bagTick.intValue++ },
    ) {
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route?.substringBefore("/")?.substringBefore("?") ?: "home"
        val isTab = TABS.any { it.route == route }
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                if (isTab) TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = "Suvrnika Logo",
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Text(config?.company?.name ?: "SUVRNIKA", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Ivory),
                ) else TopAppBar(
                    title = { Text(TITLES[route] ?: "") },
                    navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Ivory),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (t.route == "bag" && bagCount > 0) BadgedBox(badge = { Badge(containerColor = Brand.Gold) { Text("$bagCount") } }) { Icon(t.icon, t.label) }
                                else Icon(t.icon, t.label)
                            },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Brand.GoldPale, selectedIconColor = Brand.Ink, selectedTextColor = Brand.Ink),
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                composable("home") { HomeScreen() }
                composable(
                    "shop?metal={metal}&cat={cat}&tag={tag}&band={band}&q={q}",
                    arguments = listOf("metal", "cat", "tag", "band", "q").map { navArgument(it) { type = NavType.StringType; nullable = true; defaultValue = null } },
                ) { e ->
                    val a = e.arguments
                    ShopScreen(a?.getString("metal"), a?.getString("cat"), a?.getString("tag"), a?.getString("band"), a?.getString("q"))
                }
                composable("product/{sku}") { e -> ProductScreen(e.arguments?.getString("sku").orEmpty()) }
                composable("bag") { BagScreen() }
                composable("wishlist") { WishlistScreen() }
                composable("account") { AccountScreen() }
                composable("login") { LoginScreen() }
                composable("wallet") { WalletScreen() }
                composable("history") { HistoryScreen() }
                composable("plans") { PlansScreen() }
                composable("orders") { OrdersScreen() }
                composable("order/{id}") { e -> OrderScreen(e.arguments?.getString("id").orEmpty()) }
                composable("invoices") { InvoicesScreen() }
                composable("payments") { PaymentMethodsScreen() }
                composable("profile") { ProfileScreen() }
                composable("stores") { StoresScreen() }
                composable("calculator") { CalculatorScreen() }
                composable("offers") { OffersScreen() }
                composable("offer/{slug}") { e -> OfferScreen(e.arguments?.getString("slug").orEmpty()) }
                composable("journal") { JournalScreen() }
                composable("post/{slug}") { e -> PostScreen(e.arguments?.getString("slug").orEmpty()) }
                composable("help") { HelpScreen() }
            }
        }
    }
}
