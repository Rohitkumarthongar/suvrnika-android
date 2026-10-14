package com.suvrnika.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.AppConfig
import com.suvrnika.app.data.Item
import com.suvrnika.app.data.PaymentStart
import com.suvrnika.app.data.Repository
import com.suvrnika.app.payment.PaymentCoordinator
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.inr
import com.suvrnika.app.util.metalLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- app-wide locals
val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository not provided") }
val LocalPayments = staticCompositionLocalOf<PaymentCoordinator> { error("Payments not provided") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("NavController not provided") }
val LocalToast = staticCompositionLocalOf<(String) -> Unit> { {} }
val LocalConfig = compositionLocalOf<AppConfig?> { null }
/** Ask the app shell to refresh the bag badge after bag/wishlist changes. */
val LocalBagChanged = staticCompositionLocalOf<() -> Unit> { {} }

/** Widest a column of text/cards grows on tablets before it is centred. */
val MAX_CONTENT_WIDTH = 760.dp

// ---------------------------------------------------------------- loading server data
/**
 * Server data for one screen. Loaded on first show, reloaded whenever the app comes back to the
 * foreground and on pull-to-refresh — so a change made on the website appears here without restarting.
 */
@Stable
class Load<T>(private val scope: CoroutineScope, private val fetch: suspend () -> T) {
    var data by mutableStateOf<T?>(null); private set
    var error by mutableStateOf<ApiException?>(null); private set
    var refreshing by mutableStateOf(false); private set

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                data = fetch()
                error = null
            } catch (e: ApiException) {
                error = e
            } finally {
                refreshing = false
            }
        }
    }
}

@Composable
fun <T> rememberLoad(vararg keys: Any?, fetch: suspend Repository.() -> T): Load<T> {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(fetch)
    val state = remember(*keys) { Load(scope) { repo.latest() } }
    LaunchedEffect(state) { state.refresh() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, state) {
        var first = true
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { if (first) first = false else state.refresh() }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return state
}

/** Spinner on first load, an error with retry (or sign-in), then a pull-to-refresh list of [content]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadList(state: Load<T>, modifier: Modifier = Modifier, content: LazyListScope.(T) -> Unit) {
    val data = state.data
    val err = state.error
    when {
        data == null && err == null -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Brand.Gold) }
        data == null && err != null -> ErrorState(err, onRetry = state::refresh)
        data != null -> PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = state::refresh, modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Phones use the full width; on tablets the column stays at a comfortable reading width.
                val side = maxOf(16.dp, (maxWidth - MAX_CONTENT_WIDTH) / 2)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = side, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    content(data)
                }
            }
        }
    }
}

@Composable
fun ErrorState(e: ApiException, onRetry: () -> Unit) {
    if (e.needsSignIn) return SignInPrompt("Sign in to see this.")
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(e.message ?: "Something went wrong.", color = Brand.Body)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun SignInPrompt(message: String) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("My account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Brand.Muted)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Sign in", onClick = { nav.navigate("login") })
    }
}

// ---------------------------------------------------------------- running actions
/** Runs a server action with a busy flag; shows the server's message (or error) as a snackbar. */
@Stable
class Runner(private val scope: CoroutineScope, private val toast: (String) -> Unit) {
    var busy by mutableStateOf(false); private set

    fun run(onDone: () -> Unit = {}, block: suspend () -> String?) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()?.let(toast)
                onDone()
            } catch (e: ApiException) {
                toast(e.message ?: "Something went wrong.")
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberRunner(): Runner {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    return remember { Runner(scope, toast) }
}

/** Collects a payment and turns the outcome into a customer message. */
suspend fun PaymentCoordinator.payWithMessage(start: PaymentStart, success: String): Pair<Boolean, String> {
    val s = collect(start)
    return when (s.status) {
        "PAID" -> true to success
        "FAILED" -> false to "Payment not completed. Nothing was charged; any reserved items were released."
        else -> false to "Your payment is being confirmed. We'll update this as soon as it completes."
    }
}

// ---------------------------------------------------------------- visual pieces
@Composable
fun Eyebrow(text: String, color: Color = Brand.Gold) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) = Text(text, style = MaterialTheme.typography.headlineMedium, modifier = modifier)

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) = Text(text, color = Brand.Muted, fontSize = 13.sp, modifier = modifier)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, busy: Boolean = false, gold: Boolean = false) {
    Button(
        onClick = onClick, enabled = enabled && !busy, modifier = modifier.height(48.dp), shape = Brand.ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (gold) Brand.Gold else Brand.Ink, contentColor = Color.White),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp), shape = Brand.ButtonShape, border = androidx.compose.foundation.BorderStroke(1.dp, Brand.Line)) {
        Text(text, color = Brand.Ink, fontWeight = FontWeight.SemiBold)
    }
}

/** White bordered panel, the website's basic card. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Color.White).border(1.dp, Brand.Line).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

enum class Tone { Ok, Warn, Bad, Neutral }

@Composable
fun Chip(text: String, tone: Tone = Tone.Neutral) {
    val (bg, fg) = when (tone) {
        Tone.Ok -> Brand.OkBg to Brand.Ok
        Tone.Warn -> Brand.GoldPale to Brand.OnGoldPale
        Tone.Bad -> Brand.BadBg to Brand.Bad
        Tone.Neutral -> Brand.Sand to Brand.Body
    }
    Text(text.uppercase(), Modifier.background(bg).padding(horizontal = 8.dp, vertical = 4.dp), color = fg, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun KeyValue(key: String, value: String, bold: Boolean = false, valueColor: Color = Brand.Ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = if (bold) Brand.Ink else Brand.Body, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun Divider() = HorizontalDivider(color = Brand.Line)

private fun metalBg(metal: String) = when (metal) { "SILVER" -> Color(0xFFE3E8EE); "DIAMOND" -> Brand.Sand; else -> Brand.GoldPale }

/** Product photo, or the storefront's metal-tinted placeholder when there is none. */
@Composable
fun ItemImage(imageUrl: String?, metal: String, modifier: Modifier = Modifier) {
    val repo = LocalRepo.current
    Box(modifier.background(metalBg(metal)), Alignment.Center) {
        val url = repo.imageUrl(imageUrl)
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Outlined.Diamond, contentDescription = null, tint = Brand.Gold.copy(alpha = 0.55f), modifier = Modifier.size(48.dp))
    }
}

@Composable
fun ProductCard(item: Item, onOpen: () -> Unit, onWish: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Brand.CardRadius)
    Column(modifier.clip(shape).background(Color.White).border(1.dp, Brand.Line, shape).clickable(onClick = onOpen)) {
        Box {
            ItemImage(item.imageUrl, item.metal, Modifier.fillMaxWidth().aspectRatio(0.9f))
            Text(
                metalLabel(item.metal).uppercase(), Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, color = Brand.GoldDark,
            )
            if (onWish != null) IconButton(onClick = onWish, modifier = Modifier.align(Alignment.TopEnd)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)), Alignment.Center) {
                    Icon(if (item.wished) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist", tint = Brand.Gold, modifier = Modifier.size(18.dp))
                }
            }
            if (item.stock <= 0) Text("SOLD OUT", Modifier.align(Alignment.BottomStart).background(Brand.Ink).padding(6.dp, 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            else if (item.stock <= 2) Text("ONLY ${item.stock} LEFT", Modifier.align(Alignment.BottomStart).background(Brand.Gold).padding(6.dp, 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${item.purityLabel} · ${"%.1f".format(item.netMg / 1000.0)} g".uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, maxLines = 1)
            Text(item.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(44.dp))
            Text(inr(item.price.basePaise), fontWeight = FontWeight.Bold, color = Brand.Ink)
        }
    }
}
