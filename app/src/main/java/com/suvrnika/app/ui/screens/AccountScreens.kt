package com.suvrnika.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.util.rupeesToPaise
import com.suvrnika.app.util.metalLabel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.LedgerEntry
import com.suvrnika.app.ui.components.Chip
import com.suvrnika.app.ui.components.Divider
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.Heading
import com.suvrnika.app.ui.components.KeyValue
import com.suvrnika.app.ui.components.LoadList
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalPayments
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.LocalToast
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.SignInPrompt
import com.suvrnika.app.ui.components.Tone
import com.suvrnika.app.ui.components.payWithMessage
import com.suvrnika.app.ui.components.rememberLoad
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.count
import com.suvrnika.app.util.date
import com.suvrnika.app.util.delta
import com.suvrnika.app.util.grams
import com.suvrnika.app.util.inr
import com.suvrnika.app.util.mobile
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- account hub
@Composable
fun AccountScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val runner = rememberRunner()
    val signedIn by repo.tokens.signedIn.collectAsState()
    // Trading appears only when the store has switched it on, like the website's account tab.
    val trading = LocalConfig.current?.trading?.available == true
    val links: List<Triple<String, String, ImageVector>> = listOfNotNull(
        Triple("wallet", "Wallet & digital gold", Icons.Outlined.AccountBalanceWallet),
        if (trading) Triple("trading", "Trading", Icons.AutoMirrored.Outlined.ShowChart) else null,
        Triple("plans", "Savings plans", Icons.Outlined.Savings),
        Triple("orders", "Orders", Icons.Outlined.ShoppingBag),
        Triple("invoices", "Invoices", Icons.AutoMirrored.Outlined.ReceiptLong),
        Triple("history", "Complete history", Icons.Outlined.History),
        Triple("payments", "Payment methods", Icons.Outlined.CreditCard),
        Triple("profile", "Profile & KYC", Icons.Outlined.Person),
    )
    val more: List<Triple<String, String, ImageVector>> = listOf(
        Triple("stores", "Find a showroom", Icons.Outlined.LocationOn),
        Triple("calculator", "Metal value calculator", Icons.Outlined.Calculate),
        Triple("offers", "Current offers", Icons.Outlined.LocalOffer),
        Triple("journal", "Journal", Icons.Outlined.MenuBook),
        Triple("help", "Help & support", Icons.Outlined.HelpOutline),
        Triple("settings", "App settings & updates", Icons.Outlined.Settings),
    )
    if (!signedIn) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Box(Modifier.weight(1f)) { SignInPrompt("Sign in to see your wallet, plans, orders and invoices — the same account as the website.") }
            LinkList(more) { nav.navigate(it) }
        }
        return
    }
    val me = rememberLoad(signedIn) { call { me() } }
    LoadList(me) { c ->
        item {
            Column {
                Eyebrow("My account")
                Text("Hello, ${c.name.substringBefore(' ')}", style = MaterialTheme.typography.displaySmall)
                Muted("${c.mobile?.let(::mobile) ?: c.email ?: ""} · Member since ${c.memberSince?.take(4) ?: ""}")
            }
        }
        item { LinkList(links) { nav.navigate(it) } }
        item { LinkList(more) { nav.navigate(it) } }
        item {
            GhostButton("Sign out", modifier = Modifier.fillMaxWidth(), enabled = !runner.busy, onClick = {
                runner.run { repo.signOut(); "Signed out." }
            })
        }
    }
}

@Composable
private fun LinkList(links: List<Triple<String, String, ImageVector>>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).border(1.dp, Brand.Line)) {
        links.forEachIndexed { n, (route, label, icon) ->
            if (n > 0) Divider()
            Row(Modifier.fillMaxWidth().clickable { onOpen(route) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Brand.Gold)
                Spacer(Modifier.width(14.dp))
                Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("›", color = Brand.Muted, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun LedgerRow(x: LedgerEntry) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(x.kind, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Muted("${date(x.createdAt)} · ${x.description}")
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(delta(x.account, x.delta), color = if (x.delta > 0) Brand.Ok else Brand.Bad, fontWeight = FontWeight.SemiBold)
            Muted("bal " + delta(x.account, x.balanceAfter, signed = false))
        }
    }
}

@Composable
private fun Options(values: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { (k, label) ->
            FilterChip(selected = selected == k, onClick = { onPick(k) }, label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand.Ink, selectedLabelColor = Color.White))
        }
    }
}

// ---------------------------------------------------------------- wallet
/**
 * Digital metals the store deals in, from Admin → Company setup → Metals: gold when it sells gold or diamond
 * jewellery (diamond pieces are priced on the gold rate), silver when it sells silver.
 */
@Composable
fun walletMetals(): List<String> {
    val sold = offeredMetals()
    return listOfNotNull("GOLD".takeIf { "GOLD" in sold || "DIAMOND" in sold }, "SILVER".takeIf { "SILVER" in sold })
}

@Composable
fun WalletScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val payments = LocalPayments.current
    val runner = rememberRunner()
    val buyKey = rememberActionKey()
    val state = rememberLoad { call { wallet() } }
    val metals = walletMetals()
    var picked by rememberSaveable { mutableStateOf("GOLD") }
    val metal = picked.takeIf { it in metals } ?: metals.firstOrNull() ?: "GOLD"
    var amount by rememberSaveable { mutableStateOf(5000L) }
    var pts by rememberSaveable { mutableStateOf("1000") }
    var sellMg by rememberSaveable { mutableStateOf(500L) }

    LoadList(state) { w ->
        val b = w.balances
        item {
            Column(Modifier.fillMaxWidth().background(Brand.Emerald).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Eyebrow("Total wallet value", Brand.GoldLight)
                Text(inr(w.totalValuePaise), style = MaterialTheme.typography.displaySmall, color = Brand.Cream)
                Text("At today's rate · cash, points, metal and plan balance", color = Brand.EmeraldText, fontSize = 12.sp)
                // A metal the store no longer deals in is still shown while the customer holds some.
                val tiles = listOfNotNull(
                    Triple("Gold 24K", grams(b.goldMg), "≈ " + inr(w.values.goldPaise)).takeIf { "GOLD" in metals || b.goldMg > 0 },
                    Triple("Silver 999", grams(b.silverMg), "≈ " + inr(w.values.silverPaise)).takeIf { "SILVER" in metals || b.silverMg > 0 },
                    Triple("Points", count(b.points), "1 point = ${inr(w.points.pointValuePaise)}" + if (w.points.expiryMonths > 0) " · valid ${w.points.expiryMonths} mo" else ""),
                    Triple("Plan balance", inr(b.schemeCreditPaise), "For jewellery"),
                    Triple("Cash", inr(b.cashPaise), "From sales & refunds"),
                )
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (l, v, s) ->
                            Column(Modifier.weight(1f)) {
                                Text(l.uppercase(), color = Brand.GoldLight, style = MaterialTheme.typography.labelSmall)
                                Text(v, color = Brand.Cream, style = MaterialTheme.typography.titleLarge)
                                Text(s, color = Brand.EmeraldText, fontSize = 11.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (metals.isNotEmpty()) item {
            Panel {
                Text(if (metals.size == 1) "Buy digital ${metals[0].lowercase()}" else "Buy digital metal", style = MaterialTheme.typography.headlineSmall)
                if (metals.size > 1) Options(metals.map { it to metalLabel(it) }, metal) { picked = it }
                Options(listOf(1000L, 5000L, 10000L).map { it.toString() to inr(it * 100) }, amount.toString()) { amount = it.toLong() }
                val rate = if (metal == "GOLD") w.rates.goldPaisePerGram else w.rates.silverPaisePerGram
                Text("You receive ${grams(if (rate > 0) amount * 100 * 1000 / rate else 0L)} of ${metal.lowercase()}, rate locked at ${inr(rate)}/g.", color = Brand.Body, fontSize = 14.sp)
                if (w.kycLocked) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KYC required to buy digital metal.", color = Brand.Bad, fontSize = 13.sp)
                    TextButton(onClick = { nav.navigate("profile") }) { Text("Verify now", color = Brand.Bad) }
                }
                PrimaryButton("Pay ${inr(amount * 100)}", gold = true, busy = runner.busy, enabled = !w.kycLocked, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = state::refresh) {
                        val start = try {
                            repo.call { buyMetal(buyKey.value, com.suvrnika.app.data.BuyMetalRequest(metal, amount * 100)) }.also { buyKey.settle() }
                        } catch (e: ApiException) { buyKey.settle(e); throw e }
                        payments.payWithMessage(start.payment, "${if (metal == "GOLD") "Gold" else "Silver"} added to your wallet.").second
                    }
                })
            }
        }
        if ("GOLD" in metals && w.points.convertToGold) item {
            Panel {
                Text("Points into gold", style = MaterialTheme.typography.headlineSmall)
                Options(listOf("500" to "500", "1000" to "1,000", "all" to "All"), pts) { pts = it }
                val n = if (pts == "all") b.points else minOf(pts.toLong(), b.points)
                val mg = if (w.rates.goldPaisePerGram > 0) n * w.points.pointValuePaise * 1000 / w.rates.goldPaisePerGram else 0L
                Text("${count(n)} points become ${grams(mg)} of 24K gold (1 point = ${inr(w.points.pointValuePaise)}).", color = Brand.Body, fontSize = 14.sp)
                if (!w.points.convertToGold) Text("Points-to-gold conversion is currently turned off.", color = Brand.Bad, fontSize = 13.sp)
                PrimaryButton("Convert to gold", busy = runner.busy, enabled = n > 0 && w.points.convertToGold, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = state::refresh) { repo.call { convertPoints(repo.newIdempotencyKey(), com.suvrnika.app.data.PointsRequest(n)) }.message }
                })
            }
        }
        if ("GOLD" in metals || b.goldMg > 0) item {
            Panel {
                Text("Sell gold to cash", style = MaterialTheme.typography.headlineSmall)
                Options(listOf(100L, 500L, 1000L).map { it.toString() to "%.1f g".format(it / 1000.0) }, sellMg.toString()) { sellMg = it.toLong() }
                val n = minOf(sellMg, b.goldMg)
                Text("Selling ${grams(n)} adds ${inr(n * w.rates.goldPaisePerGram / 1000)} to your wallet cash.", color = Brand.Body, fontSize = 14.sp)
                GhostButton("Sell gold", enabled = !runner.busy && n > 0 && !w.kycLocked, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = state::refresh) { repo.call { sellGold(repo.newIdempotencyKey(), com.suvrnika.app.data.SellGoldRequest(n)) }.message }
                })
            }
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Heading("Recent activity", Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("history") }) { Text("Complete history", color = Brand.Gold) }
            }
        }
        if (w.recent.isEmpty()) item { Muted("No activity yet.") }
        items(w.recent, key = { it.id }) { LedgerRow(it) }
    }
}

// ---------------------------------------------------------------- history
private val FILTERS = listOf("all" to "Everything", "orders" to "Money", "plans" to "Plan balance", "metal" to "Gold & silver", "points" to "Points")

@Composable
fun HistoryScreen() {
    val repo = LocalRepo.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf("all") }
    val more = remember(filter) { mutableStateListOf<LedgerEntry>() }
    var cursor by remember(filter) { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    val state = rememberLoad(filter) { call { history(filter) }.also { more.clear(); cursor = it.nextCursor } }
    LoadList(state) { h ->
        item {
            val s = h.stats
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf("Orders placed" to count(s.ordersPlaced), "Paid into plans" to inr(s.paidIntoPlansPaise)),
                    listOf("Gold owned" to grams(s.goldMg), "Points balance" to count(s.points)),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (l, v) -> Panel(Modifier.weight(1f)) { Text(l.uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Muted); Text(v, style = MaterialTheme.typography.headlineSmall) } }
                    }
                }
                Muted("${inr(s.ordersTotalPaise)} across orders · ${s.plansCount} plan(s) · gold ≈ ${inr(s.goldValuePaise)} today")
            }
        }
        item { Options(FILTERS, filter) { filter = it } }
        val rows = h.entries + more
        if (rows.isEmpty()) item { Muted("Nothing here yet.") }
        items(rows, key = { it.id }) { LedgerRow(it) }
        cursor?.let { c ->
            item {
                GhostButton(if (loadingMore) "Loading…" else "Load more", enabled = !loadingMore, modifier = Modifier.fillMaxWidth(), onClick = {
                    loadingMore = true
                    scope.launch {
                        try { val next = repo.call { history(filter, c) }; more.addAll(next.entries); cursor = next.nextCursor }
                        catch (e: ApiException) { toast(e.message ?: "") }
                        finally { loadingMore = false }
                    }
                })
            }
        }
    }
}

// ---------------------------------------------------------------- plans
private val MISS = mapOf("EXTEND" to "a missed EMI extends the plan", "PRORATA" to "a missed EMI reduces the bonus pro-rata", "FORFEIT" to "a missed EMI forfeits the bonus")

@Composable
fun PlansScreen() {
    val repo = LocalRepo.current
    val payments = LocalPayments.current
    val nav = LocalNav.current
    val runner = rememberRunner()
    val joinKey = rememberActionKey()
    val signedIn by repo.tokens.signedIn.collectAsState()
    if (!signedIn) return SignInPrompt("Sign in to join a savings plan.")
    val state = rememberLoad { call { plans() } }
    val choice = remember { mutableStateOf(mapOf<String, Long>()) }

    LoadList(state) { p ->
        if (p.enrollments.isEmpty()) item { Muted("You haven't joined a savings plan yet. Choose one below.") }
        items(p.enrollments, key = { it.id }) { e ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.scheme.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Chip(when (e.status) { "ACTIVE" -> "Active"; "MATURED" -> "Matured"; else -> "Redeemed" }, if (e.status == "ACTIVE") Tone.Warn else Tone.Ok)
                }
                Muted("${inr(e.instalmentPaise)} every month · ${if (e.scheme.autopay) "UPI Autopay" else "Pay manually"}" + if (e.scheme.graceDays > 0) " · ${e.scheme.graceDays}-day grace" else "")
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(e.tenure) { k ->
                        Box(Modifier.weight(1f).height(26.dp).background(if (k < e.paidCount) Brand.Emerald else Color.White).border(1.dp, if (k < e.paidCount) Brand.Emerald else Brand.Line), Alignment.Center) {
                            Text("${k + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (k < e.paidCount) Brand.GoldSoft else Brand.Muted)
                        }
                    }
                }
                KeyValue("Paid so far", inr(e.paidSoFarPaise))
                KeyValue("Our bonus", if (e.bonusPaise > 0) inr(e.bonusPaise) else "—", valueColor = Brand.Gold)
                if (e.accrual == "WEIGHT") KeyValue("Gold accrued", grams(e.accruedMg)) else KeyValue("You redeem", inr(e.redeemValuePaise))
                if (e.status == "ACTIVE") PrimaryButton("Pay instalment ${e.paidCount + 1} of ${e.tenure} · ${inr(e.instalmentPaise)}", gold = true, busy = runner.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = state::refresh) {
                        val start = repo.call { payInstalment(repo.newIdempotencyKey(), e.id) }
                        payments.payWithMessage(start.payment, "Instalment received. Thank you.").second
                    }
                }) else Row(Modifier.fillMaxWidth().background(Brand.OkBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${inr(e.redeemValuePaise)} added to your plan balance", Modifier.weight(1f), color = Brand.Ok, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    TextButton(onClick = { nav.navigate("shop") }) { Text("Shop now", color = Brand.Ok) }
                }
            }
        }
        item { Column { Heading("Choose a plan"); Muted("Every plan completes within 12 months and is redeemed against jewellery.") } }
        items(p.available, key = { it.id }) { s ->
            val opts = listOf(s.minPaise, s.minPaise * 2, s.minPaise * 5)
                .filter { s.maxPaise == null || it <= s.maxPaise }.filter { s.stepPaise == null || s.stepPaise == 0L || (it - s.minPaise) % s.stepPaise == 0L }
            val amt = choice.value[s.id] ?: s.minPaise
            Panel {
                Eyebrow(if (s.type == "MONTHLY") "Monthly plan" else "One-time advance")
                Text(s.name, style = MaterialTheme.typography.headlineSmall)
                s.tagline?.let { Text(it, style = MaterialTheme.typography.titleLarge, color = Brand.Body) }
                val bonus = when {
                    s.bonusMonths == 1.0 -> "We add one full month as bonus"
                    s.bonusMonths == 0.5 -> "We add half a month as bonus"
                    s.bonusMonths > 0 -> "We add ${s.bonusMonths} months as bonus"
                    s.accrual == "WEIGHT" -> "Gold weight locked at each payment"
                    else -> "Rate protection"
                }
                Text(
                    "${s.tenureMonths} months · from ${inr(s.minPaise)}${if (s.type == "MONTHLY") " a month" else ""}\n$bonus" +
                        (if (s.makingOffPct > 0) " · ${s.makingOffPct}% off making at redemption" else "") +
                        "\n${if (s.graceDays > 0) "${s.graceDays}-day grace · " else ""}${MISS[s.missRule] ?: ""}",
                    color = Brand.Body, fontSize = 14.sp,
                )
                Options(opts.map { it.toString() to inr(it) }, amt.toString()) { choice.value = choice.value + (s.id to it.toLong()) }
                GhostButton("Join with ${inr(amt)}/month", enabled = !runner.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = state::refresh) {
                        val start = try {
                            repo.call { joinPlan(joinKey.value, com.suvrnika.app.data.JoinPlanRequest(s.id, amt)) }.also { joinKey.settle() }
                        } catch (e: ApiException) { joinKey.settle(e); throw e }
                        payments.payWithMessage(start.payment, "Welcome to ${s.name}! Your first instalment is received.").second
                    }
                })
            }
        }
    }
}

// ---------------------------------------------------------------- orders
@Composable
fun OrdersScreen() {
    val nav = LocalNav.current
    val state = rememberLoad { call { orders() } }
    LoadList(state) { res ->
        if (res.orders.isEmpty()) item { Muted("No orders yet.") }
        items(res.orders, key = { it.id }) { o ->
            Panel(Modifier.clickable { nav.navigate("order/${o.id}") }) {
                Text(o.lines.joinToString { "${it.name} × ${it.qty}" }, style = MaterialTheme.typography.titleLarge)
                Muted("${o.number} · Ordered ${date(o.createdAt)}" + (o.branch?.let { " · from ${it.city}" } ?: ""))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(inr(o.payablePaise), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Chip(o.statusLabel, when (o.status) { "CANCELLED" -> Tone.Bad; "PENDING_PAYMENT" -> Tone.Warn; "DELIVERED" -> Tone.Ok; else -> Tone.Neutral })
                }
                if (o.stageIndex >= 0) StageBar(o.stages.map { it.label }, o.stageIndex)
                o.shipment?.let { sh -> Muted(listOfNotNull(sh.courier, "AWB ${sh.awb}", sh.status).joinToString(" · ")) }
            }
        }
    }
}

/** Courier, AWB, latest tracking status and events, with a link to the courier's tracking page. */
@Composable
private fun ShipmentPanel(sh: com.suvrnika.app.data.Shipment) {
    val context = LocalContext.current
    Panel {
        Eyebrow("Shipment", Brand.Muted)
        Text(listOfNotNull(sh.courier, "AWB ${sh.awb}").joinToString(" · "), fontWeight = FontWeight.SemiBold)
        sh.status?.let { Muted(it + (sh.deliveredAt?.let { d -> " · ${date(d)}" } ?: "")) }
        sh.events.take(6).forEach { e ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(Brand.Gold))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.status + (e.location?.let { " · $it" } ?: ""), fontSize = 13.sp)
                    Muted(date(e.at))
                }
            }
        }
        sh.trackingUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { url ->
            GhostButton("Track shipment ↗", modifier = Modifier.fillMaxWidth(), onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
    }
}

@Composable
private fun StageBar(labels: List<String>, index: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { i, l ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (i <= index) Brand.Gold else Brand.Line))
                Text(l, fontSize = 9.sp, lineHeight = 11.sp, color = if (i <= index) Brand.Ink else Brand.Muted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun OrderScreen(id: String) {
    val state = rememberLoad(id) { call { order(id) }.order }
    val invoice = rememberInvoiceOpener()
    LoadList(state) { o ->
        item {
            Column {
                Heading(o.number)
                Muted("Ordered ${date(o.createdAt)}" + (o.paidAt?.let { " · paid ${date(it)}" } ?: "") + (o.branch?.let { " · from ${it.name}, ${it.city}" } ?: ""))
            }
        }
        item { Chip(o.statusLabel, if (o.status == "CANCELLED") Tone.Bad else if (o.status == "PENDING_PAYMENT") Tone.Warn else Tone.Ok) }
        if (o.stageIndex >= 0) item { StageBar(o.stages.map { it.label }, o.stageIndex) }
        item {
            Panel {
                o.lines.forEach { l ->
                    KeyValue("${l.name} × ${l.qty}", inr(l.unitPricePaise * l.qty))
                    if (l.discountPaise > 0) KeyValue("   Offer discount", "−" + inr(l.discountPaise), valueColor = Brand.Ok)
                }
                Divider()
                o.offer?.takeIf { it.discountPaise > 0 }?.let { f ->
                    KeyValue("Special offer" + (f.title?.let { " · $it" } ?: "") + (f.code?.let { " ($it)" } ?: ""), "−" + inr(f.discountPaise), valueColor = Brand.Ok)
                }
                KeyValue("Subtotal", inr(o.subtotalPaise))
                KeyValue("GST", inr(o.gstPaise))
                if (o.pointsUsed > 0) KeyValue("Points (${count(o.pointsUsed)})", "−" + inr(o.pointsValuePaise), valueColor = Brand.Ok)
                if (o.schemeUsedPaise > 0) KeyValue("Plan balance", "−" + inr(o.schemeUsedPaise), valueColor = Brand.Ok)
                KeyValue("Paid", inr(o.payablePaise), bold = true)
                if (o.pointsEarned > 0 && o.status != "CANCELLED" && o.status != "PENDING_PAYMENT") Muted("+${count(o.pointsEarned)} points earned")
            }
        }
        o.shipment?.let { sh -> item { ShipmentPanel(sh) } }
        val huids = o.lines.flatMap { l -> l.huids.map { l.name to it } }
        if (huids.isNotEmpty()) item {
            Panel {
                Eyebrow("BIS hallmark · HUID", Brand.Muted)
                huids.forEach { (name, h) -> KeyValue(name, h) }
            }
        }
        item {
            Panel {
                Eyebrow("Order history", Brand.Muted)
                if (o.history.isEmpty()) Muted("No status history is available yet.")
                o.history.reversed().forEach { h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.Gold))
                        Spacer(Modifier.width(10.dp))
                        Text(h.label, Modifier.weight(1f), fontSize = 14.sp)
                        Muted(date(h.timestamp))
                    }
                }
            }
        }
        if (o.invoiceAvailable) item {
            PrimaryButton("Download invoice (PDF)", busy = invoice.busy, modifier = Modifier.fillMaxWidth(), onClick = { invoice.open(o.id, o.number) })
        }
    }
}

/** Downloads an invoice PDF with the session token and hands it to the device's PDF viewer. */
class InvoiceOpener(val busy: Boolean, val open: (orderId: String, name: String) -> Unit)

@Composable
fun rememberInvoiceOpener(): InvoiceOpener {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val runner = rememberRunner()
    return InvoiceOpener(runner.busy) { orderId, name ->
        runner.run {
            val file = repo.downloadInvoice(orderId, name)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { context.startActivity(Intent.createChooser(view, "Open invoice").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); null }
            catch (e: ActivityNotFoundException) { "Install a PDF viewer to open invoices." }
        }
    }
}

@Composable
fun InvoicesScreen() {
    val state = rememberLoad { call { invoices() } }
    val invoice = rememberInvoiceOpener()
    LoadList(state) { res ->
        item { Column { Heading("Invoices"); Muted("Open an invoice to view, print or share it as PDF.") } }
        if (res.invoices.isEmpty()) item { Muted("No paid-order invoices yet.") }
        items(res.invoices, key = { it.orderId }) { inv ->
            Panel {
                Text(inv.invoiceNumber, style = MaterialTheme.typography.headlineSmall)
                Muted("${inv.orderNumber} · paid ${date(inv.paidAt)}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(inr(inv.payablePaise), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(enabled = !invoice.busy, onClick = { invoice.open(inv.orderId, inv.invoiceNumber) }) { Text("Download PDF", color = Brand.Gold) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- trading
private val TRADE_STATUS = mapOf("PENDING" to "Pending", "EXECUTED" to "Confirmed", "REJECTED" to "Rejected", "CANCELLED" to "Cancelled")

/**
 * Same as /account/trading: buy digital gold/silver through the store's broker. The customer pays now; staff
 * confirm the request at the rate actually dealt and credit the wallet. A pending request can be cancelled
 * for a refund.
 */
@Composable
fun TradingScreen() {
    val repo = LocalRepo.current
    val payments = LocalPayments.current
    val nav = LocalNav.current
    val runner = rememberRunner()
    val key = rememberActionKey()
    val signedIn by repo.tokens.signedIn.collectAsState()
    if (!signedIn) return SignInPrompt("Sign in to trade digital gold and silver.")
    val state = rememberLoad { call { trading() } }
    val metals = walletMetals()
    var picked by rememberSaveable { mutableStateOf("GOLD") }
    val metal = picked.takeIf { it in metals } ?: metals.firstOrNull() ?: "GOLD"
    var amount by rememberSaveable { mutableStateOf("") }
    var cancelling by remember { mutableStateOf<com.suvrnika.app.data.TradeRequest?>(null) }

    LoadList(state) { t ->
        item {
            Column {
                Heading("Trading")
                Muted("Buy digital gold/silver through our broker. You pay now; our team confirms your request at the rate actually dealt, and credits your wallet.")
            }
        }
        val win = t.window
        if (!win.available) {
            item { Panel { Muted("Trading isn't available right now — the store hasn't connected a trading account yet.") } }
        } else item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Buy at today's rate", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Chip(if (win.open) "Open now" else if (win.weekend) "Closed · weekend" else "Closed · opens ${win.start}", if (win.open) Tone.Ok else Tone.Bad)
                }
                Muted("Trading hours ${win.start}–${win.end} IST, Monday–Friday.")
                if (metals.size > 1) Options(metals.map { it to metalLabel(it) }, metal) { picked = it }
                val quick = listOf(1000L, 5000L, 10000L)
                val paise = rupeesToPaise(amount) ?: maxOf(100000L, t.minAmountPaise)
                Options(quick.map { it.toString() to inr(it * 100) }, (paise / 100).toString()) { amount = it }
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' }.take(10) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Or enter an amount (${inr(t.minAmountPaise)}–${inr(t.maxAmountPaise)})") }, prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Brand.Line),
                )
                val rate = if (metal == "GOLD") t.goldPaisePerGram else t.silverPaisePerGram
                Column(Modifier.fillMaxWidth().background(Brand.GoldPale).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "About ${grams(if (rate > 0) paise * 1000 / rate else 0L)} of ${metal.lowercase()} at our store rate of ${inr(rate)}/g" +
                            (t.rateUpdatedAt?.let { " (updated ${date(it)})" } ?: "") + ".",
                        color = Brand.OnGoldPale, fontSize = 14.sp,
                    )
                    Text("This is an estimate. The final quantity uses the market rate at which we place your order, so it may be slightly more or less.", color = Brand.OnGoldPale, fontSize = 12.sp)
                }
                val inRange = paise in t.minAmountPaise..t.maxAmountPaise
                if (t.kycLocked) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KYC required to trade digital metal.", color = Brand.Bad, fontSize = 13.sp)
                    TextButton(onClick = { nav.navigate("profile") }) { Text("Verify now", color = Brand.Bad) }
                }
                if (!inRange) Text("Enter an amount between ${inr(t.minAmountPaise)} and ${inr(t.maxAmountPaise)}.", color = Brand.Bad, fontSize = 13.sp)
                PrimaryButton(
                    if (win.open) "Request · ${inr(paise)}" else "Trading closed", gold = true, busy = runner.busy,
                    enabled = win.open && inRange && !t.kycLocked, modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runner.run(onDone = state::refresh) {
                            val start = try {
                                repo.call { tradeRequest(key.value, com.suvrnika.app.data.TradeRequestBody(metal, "BUY", paise)) }.also { key.settle() }
                            } catch (e: ApiException) { key.settle(e); throw e }
                            payments.payWithMessage(start.payment, "Request placed — our team will confirm it at the dealt rate shortly.").second
                        }
                    },
                )
            }
        }
        item { Heading("Your requests") }
        if (t.requests.isEmpty()) item { Muted("No trade requests yet.") }
        items(t.requests, key = { it.id }) { r ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${if (r.side == "BUY") "Buy" else "Sell"} ${r.metal.lowercase()} · ${inr(r.amountPaise)}", fontWeight = FontWeight.SemiBold)
                        Muted(date(r.createdAt) + " · " + if (r.status == "EXECUTED" && r.executedMg != null) "${grams(r.executedMg)} @ ${inr(r.executedRatePaisePerGram ?: 0L)}/g" else "ref. ${inr(r.refRatePaisePerGram)}/g")
                    }
                    Chip(TRADE_STATUS[r.status] ?: r.status, when (r.status) { "EXECUTED" -> Tone.Ok; "REJECTED" -> Tone.Bad; "PENDING" -> Tone.Warn; else -> Tone.Neutral })
                }
                if (r.status == "EXECUTED" && r.executedMg != null) Text("${grams(r.executedMg)} added to your wallet.", color = Brand.Ok, fontSize = 13.sp)
                if (r.status == "REJECTED" || r.status == "CANCELLED") Muted(
                    (if (r.status == "REJECTED" && !r.reason.isNullOrBlank()) "${r.reason}. " else "") +
                        if (r.refundStatus == "initiated") "Refund started to your original payment method (usually 5–7 working days)." else "Our team will process your refund shortly.",
                )
                if (r.status == "PENDING") TextButton(enabled = !runner.busy, onClick = { cancelling = r }) { Text("Cancel request", color = Brand.Bad) }
            }
        }
    }

    cancelling?.let { r ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("Cancel this request?") },
            text = { Text("Your payment of ${inr(r.amountPaise)} will be refunded.") },
            confirmButton = {
                TextButton(onClick = {
                    cancelling = null
                    runner.run(onDone = state::refresh) { repo.call { cancelTrade(r.id) }.message ?: "Request cancelled." }
                }) { Text("Cancel request", color = Brand.Bad) }
            },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Keep it") } },
        )
    }
}
