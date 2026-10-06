package com.suvrnika.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.suvrnika.app.BuildConfig
import com.suvrnika.app.data.Item
import com.suvrnika.app.data.Offer
import com.suvrnika.app.data.Post
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.date
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

// ---------------------------------------------------------------- responsive grid screens
/** Spans the full row of a grid (headings, banners) — the grid equivalent of a LazyColumn item. */
fun LazyGridScope.full(key: Any? = null, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

/**
 * Like [LoadList], but as a grid whose column count follows the screen width:
 * 2 columns on a phone, 3 on a large phone in landscape, 4–5 on a tablet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadGrid(state: Load<T>, minCell: Dp = 158.dp, content: LazyGridScope.(T) -> Unit) {
    val data = state.data
    val err = state.error
    when {
        data == null && err == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Brand.Gold) }
        data == null && err != null -> ErrorState(err, onRetry = state::refresh)
        data != null -> PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = state::refresh, modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val side = maxOf(16.dp, (maxWidth - 1200.dp) / 2)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minCell), modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = side, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { content(data) }
            }
        }
    }
}

/** Card width for horizontal rails: about 2.3 cards visible on a phone, more on tablets. */
@Composable
fun railCardWidth(): Dp {
    val w = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    return (w / 2.3f).coerceIn(150.dp, 210.dp)
}

// ---------------------------------------------------------------- sections & rails
@Composable
fun SectionHeader(eyebrow: String?, title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            eyebrow?.let { Eyebrow(it) }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text("$action →", color = Brand.Gold, fontWeight = FontWeight.SemiBold) }
    }
}

/** Swipeable row of product cards. Rails bleed to the screen edge so the next card peeks in. */
@Composable
fun ItemRail(pieces: List<Item>, onOpen: (Item) -> Unit, onWish: ((Item) -> Unit)?) {
    val w = railCardWidth()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
        items(pieces, key = { it.id }) { i -> ProductCard(i, onOpen = { onOpen(i) }, onWish = onWish?.let { f -> { f(i) } }, modifier = Modifier.width(w)) }
    }
}

/** 2-column grid section for displaying multiple items cleanly inside lazy lists. */
@Composable
fun ProductGridSection(pieces: List<Item>, onOpen: (Item) -> Unit, onWish: ((Item) -> Unit)?) {
    val rows = pieces.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    ProductCard(item, onOpen = { onOpen(item) }, onWish = onWish?.let { f -> { f(item) } }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- hero carousel
/** One slide of the home carousel. */
data class Slide(
    val eyebrow: String, val title: String, val emphasis: String? = null, val body: String, val cta: String,
    val imageUrl: String? = null, val style: SlideStyle = SlideStyle.Emerald, val onClick: () -> Unit,
)

enum class SlideStyle { Emerald, Gold, Sand, Ink }

private fun slideColors(s: SlideStyle): Triple<Brush, Color, Color> = when (s) {
    SlideStyle.Emerald -> Triple(Brush.linearGradient(listOf(Color(0xFF2C5A4D), Brand.Emerald, Color(0xFF0F241F))), Brand.Cream, Brand.GoldLight)
    SlideStyle.Gold -> Triple(Brush.linearGradient(listOf(Color(0xFFB88A3E), Brand.Gold, Brand.GoldDark)), Color.White, Brand.GoldSoft)
    SlideStyle.Sand -> Triple(Brush.linearGradient(listOf(Brand.GoldPale, Color(0xFFF0E3C8), Brand.Sand)), Brand.Ink, Brand.Gold)
    SlideStyle.Ink -> Triple(Brush.linearGradient(listOf(Color(0xFF34302A), Brand.Ink)), Brand.Cream, Brand.GoldLight)
}

/** Auto-advancing, swipeable carousel with page dots. Pauses advancing while the user is dragging. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(slides: List<Slide>, modifier: Modifier = Modifier) {
    if (slides.isEmpty()) return
    val pager = rememberPagerState { slides.size }
    LaunchedEffect(pager, slides.size) {
        while (slides.size > 1) {
            delay(5000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }
    val tall = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(pager, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().height(if (tall) 320.dp else 236.dp)) { page ->
            val s = slides[page]
            val (bg, fg, accent) = slideColors(s.style)
            val repo = LocalRepo.current
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = s.onClick)) {
                repo.imageUrl(s.imageUrl)?.let { url ->
                    AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent))))
                }
                // decorative rings, echoing the logo
                Box(Modifier.align(Alignment.CenterEnd).shift(70.dp).size(220.dp).border(1.dp, accent.copy(alpha = 0.25f), CircleShape))
                Box(Modifier.align(Alignment.CenterEnd).shift(70.dp).size(150.dp).border(1.dp, accent.copy(alpha = 0.35f), CircleShape))
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow(s.eyebrow, accent)
                    Text(s.title, color = fg, style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, lineHeight = 33.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    s.emphasis?.let { Text(it, color = accent, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text(s.body, color = fg.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 420.dp))
                    Spacer(Modifier.weight(1f))
                    Text(s.cta + "  →", color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
        if (slides.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size) { i ->
                val on = pager.currentPage == i
                val w by animateDpAsState(if (on) 22.dp else 7.dp, label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).height(7.dp).width(w).clip(RoundedCornerShape(50)).background(if (on) Brand.Gold else Brand.Line))
            }
        }
    }
}

/** Moves a decoration partly off its box (rings that bleed past the edge of a card). */
private fun Modifier.shift(x: Dp, y: Dp = 0.dp) = graphicsLayer { translationX = x.toPx(); translationY = y.toPx() }

// ---------------------------------------------------------------- journal
private val COVERS = listOf(
    listOf(Color(0xFF2C5A4D), Brand.Emerald),
    listOf(Color(0xFFB88A3E), Brand.GoldDark),
    listOf(Color(0xFF6B4E3D), Color(0xFF34302A)),
    listOf(Color(0xFF5C6F7E), Color(0xFF2E3B45)),
)

fun readMinutes(body: String) = maxOf(1, body.split(Regex("\\s+")).count { it.isNotBlank() } / 200)

/** Typographic cover for an article (the CMS has no article images): tinted gradient, initial and rings. */
@Composable
fun JournalCover(post: Post, modifier: Modifier = Modifier, big: Boolean = false) {
    val palette = COVERS[abs(post.slug.hashCode()) % COVERS.size]
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(palette))) {
        Box(Modifier.align(Alignment.BottomEnd).size(if (big) 220.dp else 140.dp).shift(40.dp, 40.dp).border(1.dp, Brand.GoldLight.copy(alpha = 0.3f), CircleShape))
        Text(
            post.title.trim().take(1).uppercase(), Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
            color = Brand.GoldLight.copy(alpha = 0.55f), fontStyle = FontStyle.Italic, fontSize = if (big) 120.sp else 76.sp,
            fontFamily = MaterialTheme.typography.displaySmall.fontFamily,
        )
        Text("JOURNAL", Modifier.align(Alignment.TopStart).padding(14.dp), style = MaterialTheme.typography.labelSmall, color = Brand.GoldLight)
    }
}

@Composable
fun JournalCard(post: Post, onOpen: () -> Unit, modifier: Modifier = Modifier, featured: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Brand.Line, RoundedCornerShape(16.dp)).clickable(onClick = onOpen)) {
        JournalCover(post, Modifier.fillMaxWidth().aspectRatio(if (featured) 1.9f else 1.6f), big = featured)
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(listOfNotNull(date(post.publishedAt).takeIf { it != "—" }, "${readMinutes(post.body)} min read").joinToString(" · ").uppercase(),
                style = MaterialTheme.typography.labelSmall, color = Brand.Gold)
            Text(post.title, style = if (featured) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (post.summary.isNotBlank()) Text(post.summary, color = Brand.Body, fontSize = 14.sp, lineHeight = 20.sp, maxLines = if (featured) 3 else 2, overflow = TextOverflow.Ellipsis)
            if (post.author.isNotBlank()) Muted("By ${post.author}")
        }
    }
}

// ---------------------------------------------------------------- offers
fun endsIn(iso: String): String? = runCatching {
    val days = Duration.between(Instant.now(), Instant.parse(iso)).toDays()
    when { days < 0 -> null; days == 0L -> "Ends today"; days == 1L -> "Ends tomorrow"; days <= 14 -> "Ends in $days days"; else -> "Ends ${date(iso)}" }
}.getOrNull()

@Composable
fun OfferCard(offer: Offer, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Brand.GoldPale, Color(0xFFF7EDD8)))).border(1.dp, Brand.GoldSoft, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen).padding(16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("OFFER", Modifier.clip(RoundedCornerShape(50)).background(Brand.Gold).padding(horizontal = 8.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(8.dp))
            endsIn(offer.endsAt)?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.GoldDark) }
        }
        Text(offer.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(offer.summary, color = Brand.Body, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text((offer.ctaLabel.ifBlank { "Details & terms" }) + " →", color = Brand.GoldDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/**
 * Opens a link the store configured (an offer's call-to-action, for example) inside the app when it
 * points at a page the app has, otherwise in the browser. Website paths map to app screens.
 */
fun openStoreLink(nav: NavHostController, context: android.content.Context, link: String) {
    val raw = link.trim()
    if (raw.isBlank()) return
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    val path = when {
        raw.startsWith(base) -> raw.removePrefix(base)
        raw.startsWith("/") -> raw
        else -> null
    }
    if (path == null) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(raw)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        return
    }
    val uri = Uri.parse("app:$path")
    val p = uri.path.orEmpty().trimEnd('/')
    fun q(k: String) = uri.getQueryParameter(k)
    val route = when {
        p == "" || p == "/" -> "home"
        p == "/shop" -> "shop?" + listOf("metal", "cat", "tag", "band", "q").mapNotNull { k -> q(k)?.let { "$k=${Uri.encode(it)}" } }.joinToString("&")
        p.startsWith("/product/") -> "product/" + p.removePrefix("/product/")
        p == "/bag" -> "bag"
        p == "/wishlist" -> "wishlist"
        p == "/stores" -> "stores"
        p == "/calculator" -> "calculator"
        p == "/journal/offers" -> "offers"
        p.startsWith("/journal/offers/") -> "offer/" + p.removePrefix("/journal/offers/")
        p == "/journal" -> "journal"
        p.startsWith("/journal/") -> "post/" + p.removePrefix("/journal/")
        p.startsWith("/help") -> "help"
        p == "/account" || p == "/account/wallet" -> "wallet"
        p.startsWith("/account/") -> mapOf("plans" to "plans", "orders" to "orders", "invoices" to "invoices", "history" to "history", "payments" to "payments", "profile" to "profile")[p.removePrefix("/account/")]
        p == "/login" -> "login"
        else -> null
    }
    if (route != null) nav.navigate(route) { launchSingleTop = true }
    else runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base + path)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// ---------------------------------------------------------------- timed offer pop-up
/**
 * The store's promotional pop-up (Admin → Content → pop-up), shown once per app launch a few seconds
 * after opening. It features the soonest-ending live offer; with no live offer it shows the configured
 * plan promotion. Tapping it opens the offer (or the plans screen).
 */
@Composable
fun OfferPopupHost(currentRoute: String, blocked: Boolean) {
    val cfg = LocalConfig.current ?: return
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val popup = cfg.popup
    var shown by rememberSaveable { mutableStateOf(false) }
    var offer by androidx.compose.runtime.remember { mutableStateOf<Offer?>(null) }
    var open by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(popup.enabled) {
        if (!popup.enabled || shown) return@LaunchedEffect
        // Admin sets the delay; keep it in the 10–15 s window so it never interrupts the first glance or arrives too late.
        delay(popup.delaySeconds.coerceIn(10, 15) * 1000L)
        offer = runCatching { repo.call { offers() }.offers.firstOrNull() }.getOrNull()
        if (offer != null || popup.headline.isNotBlank()) open = true
    }
    if (!open || shown || blocked || currentRoute == "login") return

    val o = offer
    val close = { shown = true; open = false }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand.Ivory),
        ) {
            Box(Modifier.fillMaxWidth().height(170.dp).background(Brush.linearGradient(listOf(Color(0xFF2C5A4D), Brand.Emerald, Color(0xFF0F241F))))) {
                repo.imageUrl(popup.imageUrl.ifBlank { null })?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Box(Modifier.align(Alignment.Center).size(120.dp).border(1.dp, Brand.GoldLight.copy(alpha = 0.4f), CircleShape))
                Box(Modifier.align(Alignment.Center).size(84.dp).border(1.dp, Brand.GoldLight.copy(alpha = 0.6f), CircleShape), Alignment.Center) {
                    Text(if (o != null) "%" else "12", color = Brand.GoldLight, style = MaterialTheme.typography.displaySmall)
                }
                IconButton(onClick = close, modifier = Modifier.align(Alignment.TopEnd)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), Alignment.Center) { Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(if (o != null) (endsIn(o.endsAt) ?: "Limited offer") else "Savings plan")
                Text(o?.title ?: popup.headline, style = MaterialTheme.typography.headlineMedium)
                Text(o?.summary ?: popup.sub, color = Brand.Body, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                PrimaryButton(if (o != null) o.ctaLabel.ifBlank { "View offer" } else "Explore plans", gold = true, modifier = Modifier.fillMaxWidth(), onClick = {
                    close()
                    when {
                        o == null -> nav.navigate("plans")
                        else -> nav.navigate("offer/${o.slug}")
                    }
                })
                TextButton(onClick = close, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Maybe later", color = Brand.Muted) }
            }
        }
    }
}
