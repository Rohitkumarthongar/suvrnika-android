package com.suvrnika.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.Item
import com.suvrnika.app.ui.components.Chip
import com.suvrnika.app.ui.components.Divider
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.Heading
import com.suvrnika.app.ui.components.ItemImage
import com.suvrnika.app.ui.components.KeyValue
import com.suvrnika.app.ui.components.LoadList
import com.suvrnika.app.ui.components.LocalBagChanged
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalPayments
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.ProductCard
import com.suvrnika.app.ui.components.Runner
import com.suvrnika.app.ui.components.Tone
import com.suvrnika.app.ui.components.payWithMessage
import com.suvrnika.app.ui.components.rememberLoad
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.count
import com.suvrnika.app.util.grams
import com.suvrnika.app.util.inr
import com.suvrnika.app.util.metalLabel

/** Two-column product grid inside a LazyColumn. */
fun LazyListScope.itemGrid(list: List<Item>, onOpen: (Item) -> Unit, onWish: ((Item) -> Unit)?) {
    items(list.chunked(2)) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { i -> ProductCard(i, onOpen = { onOpen(i) }, onWish = onWish?.let { f -> { f(i) } }, modifier = Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

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
@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val runner = rememberRunner()
    val state = rememberLoad { call { catalogue() } }
    LoadList(state) { cat ->
        val company = cfg?.company
        if (!company?.announce.isNullOrBlank()) item {
            Text(company!!.announce, Modifier.fillMaxWidth().background(Brand.Emerald).padding(12.dp), color = Brand.Cream, fontSize = 13.sp)
        }
        item {
            Column(Modifier.fillMaxWidth().background(Brand.Sand).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("The festive edit")
                Text(company?.hero1?.ifBlank { null } ?: "Heirlooms,", style = MaterialTheme.typography.displaySmall)
                Text(company?.hero2?.ifBlank { null } ?: "made for every day.", style = MaterialTheme.typography.displaySmall, fontStyle = FontStyle.Italic, color = Brand.Gold)
                Text("Hallmarked gold, certified diamonds and sterling silver — priced openly at today's rate.", color = Brand.Body)
                PrimaryButton("Shop the collection", onClick = { nav.navigate("shop") })
            }
        }
        cfg?.rates?.let { r ->
            item {
                Panel {
                    Eyebrow("Today's rate")
                    KeyValue("Gold 24K", "${inr(r.goldPaisePerGram)}/g")
                    KeyValue("Silver 999", "${inr(r.silverPaisePerGram)}/g")
                    TextButton(onClick = { nav.navigate("calculator") }) { Text("Metal value calculator →", color = Brand.Gold) }
                }
            }
        }
        if (!cfg?.categories.isNullOrEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Shop by category")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cfg!!.categories.forEach { c -> FilterChip(selected = false, onClick = { nav.navigate("shop?cat=${c.id}") }, label = { Text(c.name) }) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GOLD" to Brand.GoldPale, "DIAMOND" to Color(0xFFF0E3C8), "SILVER" to Color(0xFFE3E8EE)).forEach { (m, bg) ->
                    Column(Modifier.weight(1f).background(bg).clickable { nav.navigate("shop?metal=$m") }.padding(14.dp)) {
                        Text(metalLabel(m), style = MaterialTheme.typography.titleLarge)
                        Muted("${cat.items.count { it.metal == m }} pieces →")
                    }
                }
            }
        }
        item { Row(verticalAlignment = Alignment.Bottom) { Heading("Loved this season", Modifier.weight(1f)); TextButton(onClick = { nav.navigate("shop") }) { Text("View all", color = Brand.Gold) } } }
        itemGrid(cat.items.take(4), onOpen = { nav.navigate("product/${it.sku}") }, onWish = { runner.toggleWish(repo, it, state::refresh) })
        cfg?.popup?.takeIf { it.headline.isNotBlank() }?.let { p ->
            item {
                Column(Modifier.fillMaxWidth().background(Brand.Emerald).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Savings plans", Brand.GoldLight)
                    Text(p.headline, style = MaterialTheme.typography.headlineMedium, color = Brand.Cream)
                    Text(p.sub, color = Brand.EmeraldText)
                    PrimaryButton("See plans", onClick = { nav.navigate("plans") }, gold = true)
                }
            }
        }
        item { Heading("New arrivals") }
        itemGrid(cat.items.reversed().take(4), onOpen = { nav.navigate("product/${it.sku}") }, onWish = { runner.toggleWish(repo, it, state::refresh) })
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Showrooms", onClick = { nav.navigate("stores") }, modifier = Modifier.weight(1f))
                GhostButton("Offers", onClick = { nav.navigate("offers") }, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------- shop
private val METALS = listOf(null to "All", "GOLD" to "Gold", "DIAMOND" to "Diamond", "SILVER" to "Silver")
private val SORTS = listOf(null to "Featured", "newest" to "Newest", "price_asc" to "Price ↑", "price_desc" to "Price ↓")

@Composable
fun ShopScreen(metal0: String?, cat0: String?, tag0: String?, band0: String?, q0: String?) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val runner = rememberRunner()
    var metal by rememberSaveable { mutableStateOf(metal0) }
    var cat by rememberSaveable { mutableStateOf(cat0) }
    var band by rememberSaveable { mutableStateOf(band0) }
    var sort by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf(q0.orEmpty()) }
    var applied by rememberSaveable { mutableStateOf(q0.orEmpty()) }
    val state = rememberLoad(metal, cat, band, sort, applied, tag0) {
        call { catalogue(metal, cat, tag0, band, applied.ifBlank { null }, sort) }
    }
    LoadList(state) { res ->
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Search rings, necklaces, bangles…") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { applied = query.trim() }),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipRow(METALS, metal) { metal = it }
                cfg?.categories?.takeIf { it.isNotEmpty() }?.let { cats -> ChipRow(listOf<Pair<String?, String>>(null to "All categories") + cats.map { it.id to it.name }, cat) { cat = it } }
                ChipRow(listOf<Pair<String?, String>>(null to "Any price") + res.bands.map { it.key to it.label }, band) { band = it }
                ChipRow(SORTS, sort) { sort = it }
            }
        }
        item {
            val title = tag0?.let { "$it jewellery" } ?: applied.takeIf { it.isNotBlank() }?.let { "Results for “$it”" } ?: "All jewellery"
            Column { Heading(title); Muted("${res.items.size} pieces · priced at today's rate") }
        }
        if (res.items.isEmpty()) item { Muted("Nothing matches these filters yet.") }
        itemGrid(res.items, onOpen = { nav.navigate("product/${it.sku}") }, onWish = { runner.toggleWish(repo, it, state::refresh) })
    }
}

@Composable
private fun ChipRow(options: List<Pair<String?, String>>, selected: String?, onPick: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (k, label) ->
            FilterChip(
                selected = selected == k, onClick = { onPick(k) }, label = { Text(label) },
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
            item { Heading("You may also like") }
            itemGrid(p.related, onOpen = { nav.navigate("product/${it.sku}") }, onWish = null)
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
    LoadList(state) { w ->
        item { Column { Eyebrow("Saved for later"); Heading("Your wishlist"); Muted("${w.items.size} pieces · prices at today's rate") } }
        if (!w.signedIn && w.items.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().background(Brand.GoldPale).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sign in to keep your wishlist on every device.", Modifier.weight(1f), fontSize = 13.sp)
                TextButton(onClick = { nav.navigate("login") }) { Text("Sign in", color = Brand.Gold) }
            }
        }
        if (w.items.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall)
                Muted("Tap the heart on any piece to save it here.")
                PrimaryButton("Browse jewellery", onClick = { nav.navigate("shop") })
            }
        }
        items(w.items, key = { it.id }) { i ->
            Row(Modifier.fillMaxWidth().border(1.dp, Brand.Line).background(Color.White).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemImage(i.imageUrl, i.metal, Modifier.size(96.dp).clickable { nav.navigate("product/${i.sku}") })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Muted("${i.purityLabel} · ${"%.1f".format(i.netMg / 1000.0)} g")
                    Text(i.name, style = MaterialTheme.typography.titleLarge)
                    Text(inr(i.price.basePaise), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (i.stock > 0) TextButton(onClick = { runner.run(onDone = { state.refresh(); bagChanged() }) { repo.call { moveToBag(i.id) }.message } }) { Text("Move to bag", color = Brand.Gold) }
                        else Chip("Sold out", Tone.Neutral)
                        TextButton(onClick = { runner.toggleWish(repo, i, state::refresh) }) { Text("Remove", color = Brand.Muted) }
                    }
                }
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }
}
