package com.suvrnika.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Calculate
import com.suvrnika.app.ui.components.GenderSwitch
import com.suvrnika.app.ui.components.LocalUpdates
import com.suvrnika.app.ui.components.UpdateController
import com.suvrnika.app.ui.components.UpdateDialog
import com.suvrnika.app.ui.screens.SettingsScreen
import com.suvrnika.app.ui.components.OfferPopupHost
import com.suvrnika.app.ui.theme.Display
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
import com.suvrnika.app.ui.screens.TradingScreen
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
    "journal" to "Journal", "post" to "Journal", "help" to "Help & support", "trading" to "Trading", "settings" to "App settings",
)

/** Screens that show the Men | Women switch under the header (the storefront views). */
private val VIEW_ROUTES = setOf("home", "shop")

/** The Shop destination's full route (its filters are optional arguments). */
private const val SHOP_ROUTE = "shop?metal={metal}&cat={cat}&tag={tag}&band={band}&q={q}"
private fun routePattern(tab: String) = if (tab == "shop") SHOP_ROUTE else tab

/** Screens with the floating calculator button. */
private val FAB_ROUTES = setOf("home", "shop", "wishlist")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuvrnikaRoot(repo: Repository, payments: PaymentCoordinator) = SuvrnikaTheme {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = remember { { msg -> scope.launch { snack.showSnackbar(msg) } } }
    val updates = remember { UpdateController(repo.updater, scope, toast) }
    LaunchedEffect(Unit) { updates.autoCheck() }
    val signedIn by repo.tokens.signedIn.collectAsState()
    val bagTick = remember { mutableIntStateOf(0) }
    // Company setup (logo, name, metals, popup, rules…) is fetched again every time the app comes back to
    // the foreground, so changes made in Admin show up without reinstalling. A failed refresh keeps the last value.
    val configTick = remember { mutableIntStateOf(0) }
    val config by androidx.compose.runtime.produceState<com.suvrnika.app.data.AppConfig?>(null, signedIn, configTick.intValue) {
        runCatching { repo.call { config() } }.getOrNull()?.let { value = it }
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
                configTick.intValue++
                updates.onResume()
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
        LocalConfig provides config, LocalBagChanged provides { bagTick.intValue++ }, LocalUpdates provides updates,
    ) {
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route?.substringBefore("/")?.substringBefore("?") ?: "home"
        val isTab = TABS.any { it.route == route }
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            // Metal value calculator, one tap away on the storefront screens.
            floatingActionButton = {
                if (route in FAB_ROUTES) androidx.compose.material3.FloatingActionButton(
                    onClick = { nav.navigate("calculator") { launchSingleTop = true } },
                    containerColor = Brand.Ink, contentColor = Brand.Ivory, shape = androidx.compose.foundation.shape.CircleShape,
                ) { Icon(androidx.compose.material.icons.Icons.Outlined.Calculate, contentDescription = "Metal value calculator") }
            },
            topBar = { Column {
                if (isTab) TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // The logo from Admin → Company setup; the built-in mark until one is uploaded (or while it loads).
                            val logo = repo.imageUrl(config?.company?.logoUrl)
                            if (logo != null) coil.compose.AsyncImage(
                                model = logo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                placeholder = painterResource(R.drawable.ic_app_logo), error = painterResource(R.drawable.ic_app_logo),
                                modifier = Modifier.size(width = 56.dp, height = 40.dp),
                            ) else Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Column {
                                Text(config?.company?.name ?: "SUVRNIKA", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 3.sp)
                                Text((config?.company?.tag?.ifBlank { null } ?: "Fine jewellery").uppercase(), fontSize = 9.sp, letterSpacing = 2.sp, color = Brand.Gold, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    actions = {
                        if (route != "shop") IconButton(onClick = { nav.navigate("shop") { launchSingleTop = true } }) { Icon(Icons.Outlined.Search, "Search") }
                        IconButton(onClick = { nav.navigate("offers") }) { Icon(Icons.Outlined.LocalOffer, "Offers") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Ivory),
                ) else TopAppBar(
                    title = { Text(TITLES[route] ?: "") },
                    navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Ivory),
                )
                if (route in VIEW_ROUTES) Row(
                    Modifier.fillMaxWidth().background(Brand.Ivory).padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { GenderSwitch() }
            } },
            bottomBar = {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    TABS.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                // Home always clears back to the home page. Restoring Home's "saved state" would put back
                                // the screens opened from it (a category, a product), so tapping Home seemed to do nothing.
                                if (t.route == "home") nav.popBackStack("home", inclusive = false)
                                // Tapping the tab you are in (e.g. Shop while on a product opened from Shop) returns to its first screen.
                                else if (runCatching { nav.getBackStackEntry(routePattern(t.route)) }.isSuccess) nav.popBackStack(routePattern(t.route), inclusive = false)
                                // Other tabs keep their place, like the website keeps each page.
                                else nav.navigate(t.route) {
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
                    SHOP_ROUTE,
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
                composable("trading") { TradingScreen() }
                composable("settings") { SettingsScreen() }
            }
        }
        // The update pop-up wins over the promotional pop-up.
        OfferPopupHost(currentRoute = route, blocked = mock != null || updates.showDialog)
        UpdateDialog(updates)
    }
}
