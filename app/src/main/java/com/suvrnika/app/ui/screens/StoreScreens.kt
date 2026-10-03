package com.suvrnika.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.CatalogueResponse
import com.suvrnika.app.data.Item
import com.suvrnika.app.data.Offer
import com.suvrnika.app.data.Post
import com.suvrnika.app.ui.components.Chip
import com.suvrnika.app.ui.components.Divider
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.Heading
import com.suvrnika.app.ui.components.HeroCarousel
import com.suvrnika.app.ui.components.ItemImage
import com.suvrnika.app.ui.components.ItemRail
import com.suvrnika.app.ui.components.JournalCard
import com.suvrnika.app.ui.components.KeyValue
import com.suvrnika.app.ui.components.LoadGrid
import com.suvrnika.app.ui.components.LoadList
import com.suvrnika.app.ui.components.LocalBagChanged
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalPayments
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.OfferCard
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.ProductCard
import com.suvrnika.app.ui.components.Runner
import com.suvrnika.app.ui.components.SectionHeader
import com.suvrnika.app.ui.components.Slide
import com.suvrnika.app.ui.components.SlideStyle
import com.suvrnika.app.ui.components.Tone
import com.suvrnika.app.ui.components.endsIn
import com.suvrnika.app.ui.components.full
import com.suvrnika.app.ui.components.payWithMessage
import com.suvrnika.app.ui.components.rememberLoad
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.count
import com.suvrnika.app.util.grams
import com.suvrnika.app.util.inr
import com.suvrnika.app.util.metalLabel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Toggle wishlist then refresh the screen. */
private fun Runner.toggleWish(repo: com.suvrnika.app.data.Repository, item: Item, after: () -> Unit) = run(onDone = after) {
    val added = repo.call { toggleWish(item.id) }.added == true
    if (added) "Saved to your wishlist." else "Removed from your wishlist."
}

/**
 * Keeps one Idempotency-Key per customer action until the server has answered, so re-tapping after a
 * timeout retries the same action rather than placing a second order.
 */
class ActionKey(private val fresh: () -> String) {
    var value by mutableStateOf(fresh())
        private set
    fun settle(e: Throwable? = null) { if (e !is ApiException || e.status != 0) value = fresh() }
}

@Composable
fun rememberActionKey(): ActionKey {
    val repo = LocalRepo.current
    return remember { ActionKey(repo::newIdempotencyKey) }
}

// ---------------------------------------------------------------- home
private class HomeData(val catalogue: CatalogueResponse, val offers: List<Offer>, val posts: List<Post>)

@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val runner = rememberRunner()
    val state = rememberLoad {
        coroutineScope {
            val cat = async { call { catalogue() } }
            // Offers and articles are extras: the home page still loads if either is unavailable.
            val offers = async { runCatching { call { offers() }.offers }.getOrDefault(emptyList()) }
            val posts = async { runCatching { call { journal() }.posts }.getOrDefault(emptyList()) }
            HomeData(cat.await(), offers.await(), posts.await())
        }
    }
    val open = { i: Item -> nav.navigate("product/${i.sku}") }
    val wish = { i: Item -> runner.toggleWish(repo, i, state::refresh) }

    LoadList(state) { home ->
        val pieces = home.catalogue.items
        val company = cfg?.company
        if (!company?.announce.isNullOrBlank()) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Brand.Emerald).padding(horizontal = 16.dp, vertical = 9.dp), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Brand.GoldLight, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(company!!.announce, color = Brand.Cream, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            val newest = pieces.lastOrNull()
            val slides = buildList {
                add(Slide(
                    eyebrow = "The festive edit · ${java.time.Year.now().value}", title = company?.hero1?.ifBlank { null } ?: "Heirlooms,",
                    emphasis = company?.hero2?.ifBlank { null } ?: "made for every day.",
                    body = "Hallmarked gold, certified diamonds and sterling silver — priced openly at today's rate.",
                    cta = "Shop the collection", imageUrl = company?.heroImg?.ifBlank { null }, onClick = { nav.navigate("shop") },
                ))
                home.offers.take(3).forEach { o ->
                    add(Slide(eyebrow = endsIn(o.endsAt) ?: "Offer", title = o.title, body = o.summary, cta = o.ctaLabel.ifBlank { "View offer" },
                        style = SlideStyle.Gold, onClick = { nav.navigate("offer/${o.slug}") }))
                }
                cfg?.popup?.takeIf { it.headline.isNotBlank() }?.let { p ->
                    add(Slide(eyebrow = "Savings plans", title = p.headline, body = p.sub, cta = "Explore plans", imageUrl = p.imageUrl.ifBlank { null },
                        style = SlideStyle.Sand, onClick = { nav.navigate("plans") }))
                }
                newest?.let { i ->
                    add(Slide(eyebrow = "New arrival", title = i.name, emphasis = inr(i.price.basePaise), body = "${metalLabel(i.metal)} · ${i.purityLabel} · ${grams(i.netMg)}",
                        cta = "See the piece", imageUrl = i.imageUrl, style = SlideStyle.Ink, onClick = { open(i) }))
                }
            }
            HeroCarousel(slides)
        }
        cfg?.rates?.takeIf { it.goldPaisePerGram > 0 }?.let { r ->
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Brand.Line, RoundedCornerShape(16.dp))
                        .clickable { nav.navigate("calculator") }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) { Eyebrow("Today's rate"); Muted("Tap for the metal calculator") }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gold 24K  ${inr(r.goldPaisePerGram)}/g", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Silver 999  ${inr(r.silverPaisePerGram)}/g", color = Brand.Body, fontSize = 13.sp)
                    }
                }
            }
        }
        if (!cfg?.categories.isNullOrEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Shop by category", "Find your piece")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cfg!!.categories, key = { it.id }) { c ->
                        Column(Modifier.width(78.dp).clickable { nav.navigate("shop?cat=${c.id}") }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(68.dp).clip(CircleShape).background(Brand.GoldPale).border(1.dp, Brand.GoldSoft, CircleShape), Alignment.Center) {
                                val url = repo.imageUrl(c.imageUrl)
                                if (url != null) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else Text(c.name.take(1), style = MaterialTheme.typography.headlineMedium, color = Brand.Gold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(c.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Triple("GOLD", Brand.GoldPale, "Hallmarked"), Triple("DIAMOND", Color(0xFFF0E3C8), "Certified"), Triple("SILVER", Color(0xFFE3E8EE), "Sterling")).forEach { (m, bg, sub) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg).clickable { nav.navigate("shop?metal=$m") }.padding(12.dp)) {
                        Icon(Icons.Outlined.Diamond, null, tint = Brand.Gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(metalLabel(m), style = MaterialTheme.typography.titleLarge)
                        Muted("$sub · ${pieces.count { it.metal == m }}")
                    }
                }
            }
        }
        if (pieces.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Bestsellers", "Loved this season", "View all") { nav.navigate("shop") }
                ItemRail(pieces.take(8), open, wish)
            }
        }
        cfg?.popup?.takeIf { it.headline.isNotBlank() }?.let { p ->
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF2C5A4D), Brand.Emerald))).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Eyebrow("Savings plans", Brand.GoldLight)
                    Text(p.headline, style = MaterialTheme.typography.headlineMedium, color = Brand.Cream)
                    Text(p.sub, color = Brand.EmeraldText, fontSize = 14.sp)
                    PrimaryButton("See plans", onClick = { nav.navigate("plans") }, gold = true)
                }
            }
        }
        if (home.offers.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Limited time", "Current offers", "All offers") { nav.navigate("offers") }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(home.offers, key = { it.id }) { o -> OfferCard(o, onOpen = { nav.navigate("offer/${o.slug}") }, modifier = Modifier.width(280.dp)) }
                }
            }
        }
        if (pieces.size > 1) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Just in", "New arrivals", "Shop new") { nav.navigate("shop") }
                ItemRail(pieces.reversed().take(8), open, wish)
            }
        }
        if (home.posts.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Stories & guides", "From the journal", "Read more") { nav.navigate("journal") }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(home.posts.take(6), key = { it.id }) { p -> JournalCard(p, onOpen = { nav.navigate("post/${p.slug}") }, modifier = Modifier.width(260.dp)) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Showrooms", Icons.Outlined.LocationOn, "stores"),
                    Triple("Calculator", Icons.Outlined.Calculate, "calculator"),
                    Triple("Help", Icons.Outlined.SupportAgent, "help"),
                ).forEach { (label, icon, route) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Brand.Line, RoundedCornerShape(14.dp))
                            .clickable { nav.navigate(route) }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(icon, null, tint = Brand.Gold)
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shop
private val METALS = listOf(null to "All", "GOLD" to "Gold", "DIAMOND" to "Diamond", "SILVER" to "Silver")
private val SORTS = listOf(null to "Featured", "newest" to "Newest", "price_asc" to "Price: low to high", "price_desc" to "Price: high to low")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(metal0: String?, cat0: String?, tag0: String?, band0: String?, q0: String?) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val runner = rememberRunner()
    var metal by rememberSaveable { mutableStateOf(metal0) }
    var cat by rememberSaveable { mutableStateOf(cat0) }
    var tag by rememberSaveable { mutableStateOf(tag0) }
    var band by rememberSaveable { mutableStateOf(band0) }
    var sort by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf(q0.orEmpty()) }
    var applied by rememberSaveable { mutableStateOf(q0.orEmpty()) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    val state = rememberLoad(metal, cat, band, sort, applied, tag) {
        call { catalogue(metal, cat, tag, band, applied.ifBlank { null }, sort) }
    }
    val categories = cfg?.categories.orEmpty()
    val bands = state.data?.bands.orEmpty()
    val activeCount = listOfNotNull(cat, band, sort, tag).size

    LoadGrid(state) { res ->
        full("search") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(50),
                    placeholder = { Text("Search rings, necklaces…", fontSize = 14.sp) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (applied.isNotBlank()) IconButton(onClick = { query = ""; applied = "" }) { Icon(Icons.Filled.Close, "Clear search") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { applied = query.trim() }),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Brand.Line),
                )
                BadgedBox(badge = { if (activeCount > 0) Badge(containerColor = Brand.Gold) { Text("$activeCount") } }) {
                    FilledTonalIconButton(onClick = { sheet = true }, modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Brand.Ink, contentColor = Color.White)) {
                        Icon(Icons.Outlined.Tune, "Filters")
                    }
                }
            }
        }
        full("metals") { ChipRow(METALS, metal) { metal = it } }
        full("title") {
            val title = tag?.let { "$it jewellery" } ?: applied.takeIf { it.isNotBlank() }?.let { "Results for “$it”" }
                ?: categories.find { it.id == cat }?.name ?: metal?.let { "${metalLabel(it)} jewellery" } ?: "All jewellery"
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Muted("${res.items.size} pieces · priced at today's rate")
                if (activeCount > 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.find { it.id == cat }?.let { c -> RemovableChip(c.name) { cat = null } }
                    bands.find { it.key == band }?.let { b -> RemovableChip(b.label) { band = null } }
                    tag?.let { t -> RemovableChip(t) { tag = null } }
                    SORTS.find { it.first == sort && sort != null }?.let { s -> RemovableChip(s.second) { sort = null } }
                }
            }
        }
        if (res.items.isEmpty()) full("empty") {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nothing matches these filters yet", style = MaterialTheme.typography.headlineSmall)
                GhostButton("Clear filters", onClick = { cat = null; band = null; sort = null; tag = null; metal = null; query = ""; applied = "" })
            }
        }
        items(res.items, key = { it.id }) { i -> ProductCard(i, onOpen = { nav.navigate("product/${i.sku}") }, onWish = { runner.toggleWish(repo, i, state::refresh) }) }
    }

    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = Brand.Ivory) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Filters", style = MaterialTheme.typography.headlineMedium)
            if (categories.isNotEmpty()) FilterGroup("Category", listOf<Pair<String?, String>>(null to "All") + categories.map { it.id to it.name }, cat) { cat = it }
            if (bands.isNotEmpty()) FilterGroup("Price", listOf<Pair<String?, String>>(null to "Any price") + bands.map { it.key to it.label }, band) { band = it }
            FilterGroup("Sort by", SORTS, sort) { sort = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Clear all", onClick = { cat = null; band = null; sort = null; tag = null }, modifier = Modifier.weight(1f))
                PrimaryButton("Show ${state.data?.items?.size ?: 0} pieces", onClick = { sheet = false }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, options: List<Pair<String?, String>>, selected: String?, onPick: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(title, Brand.Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (k, label) ->
                FilterChip(
                    selected = selected == k, onClick = { onPick(k) }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand.Ink, selectedLabelColor = Color.White),
                )
            }
        }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Brand.GoldPale).clickable(onClick = onRemove).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand.GoldDark)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Close, "Remove $label", tint = Brand.GoldDark, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ChipRow(options: List<Pair<String?, String>>, selected: String?, onPick: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (k, label) ->
            FilterChip(
                selected = selected == k, onClick = { onPick(k) }, label = { Text(label) }, shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand.Ink, selectedLabelColor = Color.White),
            )
        }
    }
}

// ---------------------------------------------------------------- product
@Composable
fun ProductScreen(sku: String) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val bagChanged = LocalBagChanged.current
    val runner = rememberRunner()
    val state = rememberLoad(sku) { call { product(sku) } }
    LoadList(state) { p ->
        val i = p.item
        item { ItemImage(i.imageUrl, i.metal, Modifier.fillMaxWidth().aspectRatio(1f)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("${metalLabel(i.metal)} · ${i.purityLabel}")
                Text(i.name, style = MaterialTheme.typography.displaySmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(inr(i.price.totalPaise), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Muted("incl. ${p.gstBasisPoints / 100.0}% GST · today's rate")
                }
                Text("Net weight ${grams(i.netMg)} (gross ${grams(i.grossMg)}) · Stone ${i.stoneLabel ?: "none"} · SKU ${i.sku}\nBIS hallmarked, HUID printed on your invoice.", color = Brand.Body, fontSize = 14.sp)
                i.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = Brand.Body) }
            }
        }
        item {
            val soldOut = i.stock <= 0
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(if (soldOut) "Sold out" else "Add to bag", enabled = !soldOut, busy = runner.busy, modifier = Modifier.weight(1f), onClick = {
                    runner.run(onDone = bagChanged) { repo.call { addToBag(com.suvrnika.app.data.AddToBagRequest(i.id)) }.message }
                })
                PrimaryButton("Buy now", gold = true, enabled = !soldOut && !runner.busy, modifier = Modifier.weight(1f), onClick = {
                    runner.run(onDone = { bagChanged(); nav.navigate("bag") }) { repo.call { addToBag(com.suvrnika.app.data.AddToBagRequest(i.id)) }; null }
                })
                IconButton(onClick = { runner.toggleWish(repo, i, state::refresh) }) {
                    Icon(if (i.wished) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Wishlist", tint = Brand.Gold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(Brand.GoldPale).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Star, null, tint = Brand.Gold)
                Spacer(Modifier.width(10.dp))
                Text("Earn ${count(p.pointsEarned)} points (worth ${inr(p.pointsEarnedValuePaise)}). Plan balance can be used at checkout.", color = Color(0xFF4A3309), fontSize = 14.sp)
            }
        }
        item {
            Panel {
                Eyebrow("Price breakup", Brand.Ink)
                KeyValue("Metal (${grams(i.netMg)} × rate × ${i.purityPermille / 1000.0})", inr(i.price.metalPaise))
                KeyValue("Making charges (${i.makingBp / 100.0}%)", inr(i.price.makingPaise))
                KeyValue("Stone value", inr(i.price.stonePaise))
                KeyValue("GST ${p.gstBasisPoints / 100.0}%", inr(i.price.gstPaise))
                Divider()
                KeyValue("Total", inr(i.price.totalPaise), bold = true)
            }
        }
        item {
            Panel {
                Text(if (i.stock <= 0) "Sold out" else if (i.stock <= 2) "Only ${i.stock} left" else "In stock", fontWeight = FontWeight.Bold)
                Muted(if (p.availability.isEmpty()) "Made to order" else "In store: " + p.availability.joinToString { "${it.city} (${it.qty})" })
                Muted("Insured delivery · Certified hallmark + HUID")
            }
        }
        if (p.related.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(null, "You may also like")
                    ItemRail(p.related, onOpen = { nav.navigate("product/${it.sku}") }, onWish = null)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- bag & checkout
@Composable
fun BagScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val payments = LocalPayments.current
    val bagChanged = LocalBagChanged.current
    val runner = rememberRunner()
    val key = rememberActionKey()
    var usePoints by rememberSaveable { mutableStateOf(false) }
    var useScheme by rememberSaveable { mutableStateOf(false) }
    var method by rememberSaveable { mutableStateOf<String?>(null) }
    val state = rememberLoad(usePoints, useScheme) { call { bag(usePoints, useScheme) } }
    val reload = { state.refresh(); bagChanged() }

    LoadList(state) { bag ->
        item { Heading("Your bag") }
        if (bag.lines.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your bag is empty", style = MaterialTheme.typography.headlineSmall)
                    Muted("Discover pieces priced openly at today's rate.")
                    PrimaryButton("Continue shopping", onClick = { nav.navigate("shop") })
                }
            }
            return@LoadList
        }
        items(bag.lines, key = { it.lineId }) { l ->
            Row(Modifier.fillMaxWidth().border(1.dp, Brand.Line).background(Color.White).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemImage(l.item.imageUrl, l.item.metal, Modifier.size(84.dp).clickable { nav.navigate("product/${l.item.sku}") })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(l.item.name, style = MaterialTheme.typography.titleLarge)
                    Muted("${l.item.purityLabel} · ${"%.2f".format(l.item.netMg / 1000.0)} g" + if (l.qty > l.stock) " · only ${l.stock} available" else "")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(enabled = !runner.busy, onClick = { runner.run(onDone = reload) { repo.call { setQty(l.lineId, com.suvrnika.app.data.QtyRequest(l.qty - 1)) }; null } }) { Icon(Icons.Filled.Remove, "Less") }
                        Text("${l.qty}", fontWeight = FontWeight.Bold)
                        IconButton(enabled = !runner.busy && l.qty < l.stock, onClick = { runner.run(onDone = reload) { repo.call { setQty(l.lineId, com.suvrnika.app.data.QtyRequest(l.qty + 1)) }; null } }) { Icon(Icons.Filled.Add, "More") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { runner.run(onDone = reload) { repo.call { removeLine(l.lineId) }; "Removed from your bag." } }) { Text("Remove", color = Brand.Muted) }
                    }
                    Text(inr(l.lineBasePaise), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Panel {
                val t = bag.totals
                Text("Order summary", style = MaterialTheme.typography.titleLarge)
                KeyValue("Subtotal", inr(t.subtotalPaise))
                KeyValue("GST ${t.gstBasisPoints / 100.0}%", inr(t.gstPaise))
                if (bag.signedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Use points", fontWeight = FontWeight.SemiBold)
                            Muted(if (bag.redeem.canRedeem) "${count(bag.redeem.points)} available · up to ${bag.redeem.maxRedeemPct}% of the order" else "Minimum ${count(bag.redeem.minRedeem)} points to redeem")
                        }
                        Switch(checked = usePoints, enabled = bag.redeem.canRedeem, onCheckedChange = { usePoints = it })
                    }
                    if (t.pointsUsed > 0) KeyValue("Points (${count(t.pointsUsed)})", "−" + inr(t.pointsValuePaise), valueColor = Brand.Ok)
                    if (bag.redeem.schemeCreditPaise > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Use plan balance", fontWeight = FontWeight.SemiBold); Muted("${inr(bag.redeem.schemeCreditPaise)} available") }
                        Switch(checked = useScheme, onCheckedChange = { useScheme = it })
                    }
                    if (t.schemeUsedPaise > 0) KeyValue("Plan balance", "−" + inr(t.schemeUsedPaise), valueColor = Brand.Ok)
                }
                Divider()
                KeyValue("To pay", inr(t.payablePaise), bold = true)
                Muted("You earn ${count(t.pointsEarned)} points on this order.")
            }
        }
        if (bag.methods.isNotEmpty() && bag.signedIn) item {
            Panel {
                Text("Payment method", fontWeight = FontWeight.SemiBold)
                val chosen = method ?: bag.methods.first().key
                bag.methods.forEach { m ->
                    Row(Modifier.fillMaxWidth().clickable { method = m.key }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen == m.key, onClick = { method = m.key })
                        Column { Text(m.label); Muted(m.sub) }
                    }
                }
            }
        }
        item {
            val blocked = bag.outOfStock.firstOrNull()?.let { "${it.name}: only ${it.available} available — reduce the quantity." }
                ?: if (bag.panBlocked) "Orders above ${inr(bag.panAbovePaise)} need verified PAN. Complete KYC in your profile." else null
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                blocked?.let { Text(it, Modifier.fillMaxWidth().background(Brand.BadBg).padding(12.dp), color = Brand.Bad, fontSize = 13.sp) }
                if (!bag.signedIn) PrimaryButton("Sign in to check out", onClick = { nav.navigate("login") }, modifier = Modifier.fillMaxWidth())
                else PrimaryButton("Pay ${inr(bag.totals.payablePaise)}", gold = true, busy = runner.busy, enabled = blocked == null, modifier = Modifier.fillMaxWidth(), onClick = {
                    val chosen = method ?: bag.methods.firstOrNull()?.key ?: "UPI"
                    runner.run(onDone = reload) {
                        val res = try {
                            repo.call { checkout(key.value, com.suvrnika.app.data.CheckoutRequest(usePoints, useScheme, chosen)) }.also { key.settle() }
                        } catch (e: ApiException) { key.settle(e); throw e }
                        val (paid, msg) = payments.payWithMessage(res.payment, "Order ${res.orderNumber} placed. Thank you!")
                        if (paid) nav.navigate("order/${res.orderId}")
                        msg
                    }
                })
                if (bag.panBlocked) GhostButton("Complete KYC", onClick = { nav.navigate("profile") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------------------------------------------------------------- wishlist
@Composable
fun WishlistScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val bagChanged = LocalBagChanged.current
    val runner = rememberRunner()
    val state = rememberLoad { call { wishlist() } }
    LoadGrid(state) { w ->
        full("head") { Column { Eyebrow("Saved for later"); Heading("Your wishlist"); Muted("${w.items.size} pieces · prices at today's rate") } }
        if (!w.signedIn && w.items.isNotEmpty()) full("guest") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.GoldPale).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sign in to keep your wishlist on every device.", Modifier.weight(1f), fontSize = 13.sp)
                TextButton(onClick = { nav.navigate("login") }) { Text("Sign in", color = Brand.Gold) }
            }
        }
        if (w.items.isEmpty()) full("empty") {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Brand.GoldPale), Alignment.Center) { Icon(Icons.Outlined.FavoriteBorder, null, tint = Brand.Gold, modifier = Modifier.size(32.dp)) }
                Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall)
                Muted("Tap the heart on any piece to save it here.")
                PrimaryButton("Browse jewellery", onClick = { nav.navigate("shop") })
            }
        }
        items(w.items, key = { it.id }) { i ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProductCard(i, onOpen = { nav.navigate("product/${i.sku}") }, onWish = { runner.toggleWish(repo, i, state::refresh) })
                if (i.stock > 0) GhostButton("Move to bag", enabled = !runner.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = { state.refresh(); bagChanged() }) { repo.call { moveToBag(i.id) }.message }
                }) else Chip("Sold out", Tone.Neutral)
            }
        }
    }
}
