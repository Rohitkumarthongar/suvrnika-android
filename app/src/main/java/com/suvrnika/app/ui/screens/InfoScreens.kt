package com.suvrnika.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvrnika.app.data.Branch
import com.suvrnika.app.ui.components.Chip
import com.suvrnika.app.ui.components.Divider
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.Heading
import com.suvrnika.app.ui.components.JournalCard
import com.suvrnika.app.ui.components.JournalCover
import com.suvrnika.app.ui.components.KeyValue
import com.suvrnika.app.ui.components.LoadGrid
import com.suvrnika.app.ui.components.LoadList
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.OfferCard
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.SectionHeader
import com.suvrnika.app.ui.components.Tone
import com.suvrnika.app.ui.components.endsIn
import com.suvrnika.app.ui.components.full
import com.suvrnika.app.ui.components.openStoreLink
import com.suvrnika.app.ui.components.readMinutes
import com.suvrnika.app.ui.components.rememberLoad
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.date
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private fun openUri(context: android.content.Context, uri: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// ---------------------------------------------------------------- showrooms
@Composable
fun StoresScreen() {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val runner = rememberRunner()
    val signedIn by repo.tokens.signedIn.collectAsState()
    var city by rememberSaveable { mutableStateOf<String?>(null) }
    val state = rememberLoad(signedIn) { call { branches() } }
    LoadList(state) { res ->
        item { Column { Eyebrow("Showrooms"); Heading("Find a showroom") } }
        val cities = res.branches.map { it.city }.distinct()
        if (cities.size > 1) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(city == null, { city = null }, label = { Text("All") })
                cities.forEach { c -> FilterChip(city == c, { city = c }, label = { Text(c) }) }
            }
        }
        items(res.branches.filter { city == null || it.city == city }, key = Branch::id) { b ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    if (res.homeBranchId == b.id) Chip("My store", Tone.Ok)
                }
                Muted(b.city)
                b.address?.let { Text(it, color = Brand.Body, fontSize = 14.sp) }
                b.hours?.let { Muted(it) }
                Row {
                    TextButton(onClick = {
                        openUri(context, if (b.lat != null && b.lng != null) "https://www.google.com/maps/dir/?api=1&destination=${b.lat},${b.lng}"
                        else "https://www.google.com/maps/search/?api=1&query=" + Uri.encode("${b.name} ${b.city}"))
                    }) { Text("Directions", color = Brand.Gold) }
                    b.phone?.takeIf { Regex("^[+\\d\\s-]{8,}$").matches(it) }?.let { ph ->
                        TextButton(onClick = { openUri(context, "tel:" + ph.replace(" ", "")) }) { Text("Call", color = Brand.Gold) }
                    }
                    if (signedIn && res.homeBranchId != b.id) TextButton(enabled = !runner.busy, onClick = {
                        runner.run(onDone = state::refresh) { repo.call { setHomeBranch(com.suvrnika.app.data.HomeBranchRequest(b.id)) }.message }
                    }) { Text("Set as my store", color = Brand.Ink) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- calculator
private val money2 = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
private fun money(paise: Long) = "₹" + money2.format(paise / 100.0)

/**
 * The website's metal value calculator (src/lib/metal-calculator.ts): an estimate tool only.
 * It never changes catalogue prices or checkout totals, which always come from the server.
 */
@Composable
fun CalculatorScreen() {
    val cfg = LocalConfig.current
    val rates = cfg?.rates
    var metal by rememberSaveable { mutableStateOf("GOLD") }
    var weight by rememberSaveable { mutableStateOf("10") }
    var purity by rememberSaveable { mutableStateOf("916") }
    var gold by rememberSaveable(rates) { mutableStateOf(((rates?.goldPaisePerGram ?: 0) / 100.0).toString()) }
    var silver by rememberSaveable(rates) { mutableStateOf(((rates?.silverPaisePerGram ?: 0) / 100.0).toString()) }
    var making by rememberSaveable { mutableStateOf("10") }
    var stone by rememberSaveable { mutableStateOf("0") }
    var qty by rememberSaveable { mutableStateOf("1") }
    var gst by rememberSaveable(rates) { mutableStateOf(((rates?.gstBasisPoints ?: 300) / 100.0).toString()) }

    fun n(s: String) = max(0.0, s.toDoubleOrNull() ?: 0.0)
    val quantity = max(1.0, floor(s2d(qty))).toLong()
    val rate = ((if (metal == "SILVER") n(silver) else n(gold)) * 100).roundToLong()
    val metalPaise = (n(weight) * rate * (n(purity) / 1000)).roundToLong()
    val makingPaise = (metalPaise * n(making) / 100).roundToLong()
    val stonePaise = (n(stone) * 100).roundToLong()
    val base = metalPaise + makingPaise + stonePaise
    val gstPer = (base * n(gst) / 100).roundToLong()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("Plan with confidence")
        Heading("Metal value calculator")
        Muted("Estimate jewellery value using your own metal rates, weight, purity and making charge.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("GOLD" to "Gold", "DIAMOND" to "Diamond (gold)", "SILVER" to "Silver").forEach { (k, l) -> FilterChip(metal == k, { metal = k }, label = { Text(l) }) }
        }
        @Composable fun field(label: String, value: String, set: (String) -> Unit, modifier: Modifier) =
            OutlinedTextField(value, { set(it.filter { c -> c.isDigit() || c == '.' }.take(12)) }, label = { Text(label) }, singleLine = true, modifier = modifier,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { field("Weight (g)", weight, { weight = it }, Modifier.weight(1f)); field("Purity (‰)", purity, { purity = it }, Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { field("Making (%)", making, { making = it }, Modifier.weight(1f)); field("Stone / piece (₹)", stone, { stone = it }, Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { field("Gold rate (₹/g)", gold, { gold = it }, Modifier.weight(1f)); field("Silver rate (₹/g)", silver, { silver = it }, Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { field("Quantity", qty, { qty = it }, Modifier.weight(1f)); field("GST (%)", gst, { gst = it }, Modifier.weight(1f)) }
        Muted("Rates start with today's store rates, but you can replace them for your own estimate. This calculator does not change catalogue prices or checkout totals.")
        Panel {
            Text("Estimated value", style = MaterialTheme.typography.headlineSmall)
            KeyValue("Metal value / piece", money(metalPaise))
            KeyValue("Making / piece", money(makingPaise))
            KeyValue("Stone value / piece", money(stonePaise))
            KeyValue("GST total", money(gstPer * quantity))
            Divider()
            Eyebrow("Total for $quantity piece(s)", Brand.Muted)
            Text(money((base + gstPer) * quantity), style = MaterialTheme.typography.displaySmall, color = Brand.Gold)
            Muted("Using ${money(rate)} per gram × ${n(weight)} g at ${n(purity).toLong()}‰ purity.")
        }
    }
}

private fun s2d(s: String) = s.toDoubleOrNull() ?: 1.0

// ---------------------------------------------------------------- offers & journal
private val AUDIENCE = mapOf("ALL" to "Everyone", "NEW_CUSTOMERS" to "New customers", "EXISTING_CUSTOMERS" to "Existing customers", "IN_STORE" to "In store")

private fun share(context: android.content.Context, title: String, path: String) {
    val url = com.suvrnika.app.BuildConfig.API_BASE_URL.trimEnd('/') + path
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    runCatching { context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Article/offer text: blank lines separate paragraphs. */
@Composable
private fun Paragraphs(text: String) {
    text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { para ->
        Text(para, color = Brand.Ink.copy(alpha = 0.86f), fontSize = 16.sp, lineHeight = 26.sp, modifier = Modifier.padding(bottom = 12.dp))
    }
}

@Composable
fun OffersScreen() {
    val nav = LocalNav.current
    val state = rememberLoad { call { offers() } }
    LoadGrid(state, minCell = 300.dp) { res ->
        full("head") { Column { Eyebrow("Promotional announcements"); Heading("Current offers"); Muted("Each offer applies only as its terms state.") } }
        if (res.offers.isEmpty()) full("empty") { Muted("There are no active promotional offers right now. Check back soon.") }
        items(res.offers, key = { it.id }) { o -> OfferCard(o, onOpen = { nav.navigate("offer/${o.slug}") }) }
    }
}

@Composable
fun OfferScreen(slug: String) {
    val nav = LocalNav.current
    val context = LocalContext.current
    val state = rememberLoad(slug) { call { offer(slug) }.offer }
    var terms by rememberSaveable { mutableStateOf(false) }
    LoadList(state) { o ->
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFFB88A3E), Brand.Gold, Brand.GoldDark))).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Eyebrow(endsIn(o.endsAt) ?: "Offer", Brand.GoldSoft)
                Text(o.title, style = MaterialTheme.typography.displaySmall, color = Color.White)
                if (o.summary.isNotBlank()) Text(o.summary, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 21.sp)
                Text("${date(o.startsAt)} – ${date(o.endsAt)} · For ${AUDIENCE[o.audience] ?: "everyone"}", color = Brand.GoldSoft, fontSize = 12.sp)
            }
        }
        item { Paragraphs(o.body) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(o.ctaLabel.ifBlank { "Shop now" }, gold = true, modifier = Modifier.weight(1f), onClick = {
                    if (o.ctaUrl.isNotBlank()) openStoreLink(nav, context, o.ctaUrl) else nav.navigate("shop")
                })
                GhostButton("Share", onClick = { share(context, o.title, "/journal/offers/${o.slug}") })
            }
        }
        item {
            Panel {
                Row(Modifier.fillMaxWidth().clickable { terms = !terms }, verticalAlignment = Alignment.CenterVertically) {
                    Text("Terms & conditions", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(if (terms) "Hide" else "Read", color = Brand.Gold, fontWeight = FontWeight.SemiBold)
                }
                if (terms) Text(o.terms, color = Brand.Body, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
fun JournalScreen() {
    val nav = LocalNav.current
    val state = rememberLoad { call { journal() } }
    LoadGrid(state, minCell = 280.dp) { res ->
        full("head") { Column { Eyebrow("Journal"); Heading("Stories & guides"); Muted("Buying guides, care tips and the stories behind our pieces.") } }
        if (res.posts.isEmpty()) full("empty") { Muted("No articles yet. Check back soon.") }
        res.posts.firstOrNull()?.let { first -> full(first.id) { JournalCard(first, onOpen = { nav.navigate("post/${first.slug}") }, featured = true) } }
        items(res.posts.drop(1), key = { it.id }) { p -> JournalCard(p, onOpen = { nav.navigate("post/${p.slug}") }) }
    }
}

private class Article(val post: com.suvrnika.app.data.Post, val more: List<com.suvrnika.app.data.Post>)

@Composable
fun PostScreen(slug: String) {
    val nav = LocalNav.current
    val context = LocalContext.current
    val state = rememberLoad(slug) {
        coroutineScope {
            val post = async { call { post(slug) }.post }
            val all = async { runCatching { call { journal() }.posts }.getOrDefault(emptyList()) }
            Article(post.await(), all.await().filter { it.slug != slug }.take(3))
        }
    }
    LoadList(state) { a ->
        val p = a.post
        item { JournalCover(p, Modifier.fillMaxWidth().aspectRatio(2f), big = true) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(listOfNotNull(date(p.publishedAt).takeIf { it != "—" }, "${readMinutes(p.body)} min read").joinToString(" · "))
                Text(p.title, style = MaterialTheme.typography.displaySmall)
                if (p.author.isNotBlank()) Muted("By ${p.author}")
                if (p.summary.isNotBlank()) Text(p.summary, style = MaterialTheme.typography.titleLarge, fontStyle = FontStyle.Italic, color = Brand.Body)
                Divider()
            }
        }
        item { Paragraphs(p.body) }
        item { GhostButton("Share this story", onClick = { share(context, p.title, "/journal/${p.slug}") }, modifier = Modifier.fillMaxWidth()) }
        if (a.more.isNotEmpty()) {
            item { SectionHeader("Keep reading", "More from the journal", "All stories") { nav.navigate("journal") } }
            items(a.more, key = { it.id }) { m -> JournalCard(m, onOpen = { nav.navigate("post/${m.slug}") }) }
        }
    }
}

// ---------------------------------------------------------------- help
@Composable
fun HelpScreen() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val cfg = LocalConfig.current
    val s = cfg?.support
    val c = cfg?.company
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow("Customer care")
        Heading("How can we help?")
        s?.helpContent?.takeIf { it.isNotBlank() }?.let { Text(it, color = Brand.Body) }
        Panel {
            Text("Track an order", fontWeight = FontWeight.Bold)
            Muted("See every order's live status in My account → Orders.")
            TextButton(onClick = { nav.navigate("orders") }) { Text("Open orders →", color = Brand.Gold) }
        }
        listOf("Delivery" to s?.deliveryInfo, "Payments" to s?.paymentInfo, "Returns & exchanges" to s?.returnsInfo).forEach { (title, body) ->
            if (!body.isNullOrBlank()) Panel { Text(title, fontWeight = FontWeight.Bold); Text(body, color = Brand.Body, fontSize = 14.sp) }
        }
        if (!s?.faqItems.isNullOrEmpty()) Panel {
            Text("Frequently asked questions", fontWeight = FontWeight.Bold)
            s!!.faqItems.forEach { f ->
                var open by rememberSaveable(f.question) { mutableStateOf(false) }
                Divider()
                Text(f.question, Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 6.dp), fontWeight = FontWeight.SemiBold)
                if (open) Text(f.answer, color = Brand.Body, fontSize = 14.sp)
            }
        }
        if (c != null && (c.phone.isNotBlank() || c.email.isNotBlank() || c.whatsapp.isNotBlank())) Panel {
            Text("Contact us", fontWeight = FontWeight.Bold)
            s?.contactContent?.takeIf { it.isNotBlank() }?.let { Text(it, color = Brand.Body, fontSize = 14.sp) }
            if (c.phone.isNotBlank()) TextButton(onClick = { openUri(context, "tel:" + c.phone.replace(" ", "")) }) { Text("Call ${c.phone}", color = Brand.Gold) }
            if (c.whatsapp.isNotBlank()) TextButton(onClick = { openUri(context, "https://wa.me/" + c.whatsapp.filter(Char::isDigit)) }) { Text("WhatsApp", color = Brand.Gold) }
            if (c.email.isNotBlank()) TextButton(onClick = { openUri(context, "mailto:" + c.email) }) { Text(c.email, color = Brand.Gold) }
            if (c.hours.isNotBlank()) Muted(c.hours)
            if (c.address.isNotBlank()) Muted(c.address)
        }
        TextButton(onClick = { nav.navigate("stores") }) { Text("Find a showroom →", color = Brand.Gold) }
    }
}
