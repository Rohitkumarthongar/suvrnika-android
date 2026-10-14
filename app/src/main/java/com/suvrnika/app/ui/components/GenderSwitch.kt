package com.suvrnika.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.ui.theme.StoreView

private const val DOUBLE_TAP_MS = 400L

/**
 * Men | Women switch, the same as the website's store header (src/components/GenderSwitch.tsx).
 * One tap on Men or Women: the whole app switches to that view (its theme, and only its items plus unisex ones).
 * Tapping the chosen one again, or double-tapping either, goes back to the common view with every item.
 */
@Composable
fun GenderSwitch(modifier: Modifier = Modifier) {
    val toast = LocalToast.current
    val view = StoreView.gender
    val lastTap = remember { longArrayOf(0L) }
    fun apply(next: String?) {
        StoreView.set(next)
        toast(next?.let { "Showing ${StoreView.LABEL[it]!!.lowercase()}'s jewellery. Tap again to see everything." } ?: "Showing all jewellery.")
    }
    fun onTap(g: String) {
        val now = System.currentTimeMillis()
        val double = now - lastTap[0] < DOUBLE_TAP_MS
        lastTap[0] = if (double) 0L else now
        when {
            double || g == StoreView.gender -> if (StoreView.gender != null) apply(null)
            else -> apply(g)
        }
    }
    Row(
        modifier.clip(RoundedCornerShape(50)).border(1.dp, Brand.Line, RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.55f)).padding(3.dp)
            .semantics { contentDescription = view?.let { "Shop for: ${StoreView.LABEL[it]}" } ?: "Shop for: all jewellery" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        StoreView.GENDERS.forEach { g ->
            val on = view == g
            val bg by animateColorAsState(if (on) Brand.Ink else Color.Transparent, tween(250), label = "switch-bg")
            val fg by animateColorAsState(if (on) Brand.Ivory else Brand.Muted, tween(250), label = "switch-fg")
            Box(
                Modifier.height(32.dp).widthIn(min = 70.dp).clip(RoundedCornerShape(50)).background(bg)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { onTap(g) }.padding(horizontal = 14.dp),
                Alignment.Center,
            ) {
                Text(StoreView.LABEL[g]!!.uppercase(), color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            }
        }
    }
}

/**
 * "Find your kind of beautiful": the For her / For him cards on the home page. Tapping one switches the
 * app to that view and opens the shop, like the website's home page.
 */
@Composable
fun GenderEditCards(herImage: String?, himImage: String?, onPicked: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow("Curated for you")
            Text("Find your kind of beautiful", style = MaterialTheme.typography.headlineMedium)
            Muted("Choose an edit to update your shop view.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GenderCard("WOMEN", "For her", "Quietly iconic", "Jewellery that moves with you.", "Explore women's edit →", herImage, Color(0xFFEFE2C8), Modifier.weight(1f), onPicked)
            GenderCard("MEN", "For him", "Modern heirlooms", "Considered details, made to keep.", "Explore men's edit →", himImage, Color(0xFFDFE5E7), Modifier.weight(1f), onPicked)
        }
    }
}

@Composable
private fun GenderCard(
    gender: String, kicker: String, title: String, body: String, cta: String, image: String?, tint: Color, modifier: Modifier, onPicked: () -> Unit,
) {
    val repo = LocalRepo.current
    // An arch, like `.home-gender-card` (border-radius 50% 50% 0 0 / 22% 22% 0 0).
    val arch = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 0, bottomEndPercent = 0)
    val chosen = StoreView.gender == gender
    Box(
        modifier.height(250.dp).clip(arch).background(tint)
            .then(if (chosen) Modifier.border(3.dp, Brand.Gold, arch) else Modifier)
            .clickable { StoreView.set(gender); onPicked() },
    ) {
        val url = repo.imageUrl(image)
        if (url != null) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Outlined.Diamond, null, tint = Color(0xFF8C5E14).copy(alpha = 0.35f), modifier = Modifier.align(Alignment.Center).padding(bottom = 50.dp).size(72.dp))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xBD100C06))))
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(kicker.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 22.sp))
            Text(body, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 14.sp)
            Text(cta.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (chosen) Box(Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(10.dp).clip(CircleShape).background(Brand.Gold))
    }
}
