package com.suvrnika.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvrnika.app.data.MyTestimonial
import com.suvrnika.app.data.Testimonial
import com.suvrnika.app.data.TestimonialRequest
import com.suvrnika.app.data.TestimonialsResponse
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.date

private const val MIN_CHARS = 20
private const val MAX_CHARS = 600

/** Read-only star rating (gold, follows the Men / Women theme). */
@Composable
fun Stars(rating: Double, size: Dp = 16.dp) {
    Row(Modifier.semantics { contentDescription = "%.1f out of 5 stars".format(rating) }) {
        repeat(5) { i -> Icon(if (i < Math.round(rating).toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = Brand.Gold, modifier = Modifier.size(size)) }
    }
}

@Composable
private fun TestimonialCard(t: Testimonial, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Brand.Line, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Stars(t.rating.toDouble())
        Text(t.body, color = Brand.Body, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(" · Verified customer", color = Brand.Ok, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(date(t.createdAt).substringAfter(' '), color = Brand.Muted, fontSize = 11.sp)
        }
    }
}

/**
 * "Loved by our customers": verified customers' testimonials on the home page, with Write / Edit / Delete for your own.
 * Same rules as the website: one testimonial each, only verified (KYC) customers can write, and our team can hide or
 * delete but never write one. [onChanged] reloads the page after a save or delete; [signIn] / [openProfile] go to those screens.
 */
@Composable
fun TestimonialsSection(data: TestimonialsResponse, onChanged: () -> Unit, signIn: () -> Unit, openProfile: () -> Unit) {
    // Nothing to show and nothing the visitor can do about it: leave the section out rather than show an empty box.
    if (data.count == 0 && !data.canWrite) return
    val repo = LocalRepo.current
    val runner = rememberRunner()
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Eyebrow("Testimonials")
            Text("Loved by our customers", style = MaterialTheme.typography.headlineMedium)
            if (data.count > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stars(data.average, 18.dp)
                Muted("%.1f from %d verified customer%s".format(data.average, data.count, if (data.count == 1) "" else "s"))
            }
        }
        if (data.items.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
            items(data.items, key = { it.id }) { t -> TestimonialCard(t, Modifier.width(280.dp)) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                !data.signedIn -> TextButton(onClick = signIn) { Text("Sign in to share your own experience", color = Brand.Gold) }
                !data.canWrite -> {
                    Muted("Only verified customers can write a testimonial.")
                    TextButton(onClick = openProfile) { Text("Complete your KYC to share yours", color = Brand.Gold) }
                }
                else -> {
                    data.mine?.takeIf { it.hidden }?.let {
                        Text("Your testimonial is currently hidden by our team.", Modifier.fillMaxWidth().background(Brand.Sand).padding(10.dp), color = Brand.Muted, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(if (data.mine != null) "Edit my testimonial" else "Write a testimonial", onClick = { editing = true })
                        if (data.mine != null) GhostButton("Delete", enabled = !runner.busy, onClick = { confirmDelete = true })
                    }
                }
            }
        }
    }

    if (editing) TestimonialDialog(
        mine = data.mine, busy = runner.busy, onDismiss = { editing = false },
        onSave = { rating, body ->
            runner.run(onDone = { editing = false; onChanged() }) {
                repo.call { saveTestimonial(TestimonialRequest(rating, body)) }.message
            }
        },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete your testimonial?") },
        text = { Text("It will be removed from the website and the app. You can write a new one any time.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                runner.run(onDone = onChanged) { repo.call { deleteTestimonial() }.message }
            }) { Text("Delete", color = Brand.Bad) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it") } },
    )
}

/** Star picker and text box with a live character count; Save stays off until the testimonial is valid. */
@Composable
private fun TestimonialDialog(mine: MyTestimonial?, busy: Boolean, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(mine?.rating ?: 0) }
    var body by rememberSaveable { mutableStateOf(mine?.body.orEmpty()) }
    val length = body.trim().length
    val valid = rating in 1..5 && length in MIN_CHARS..MAX_CHARS
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (mine != null) "Edit your testimonial" else "Share your experience") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Your rating", Brand.Muted)
                Row {
                    (1..5).forEach { n ->
                        IconButton(onClick = { rating = n }, modifier = Modifier.size(44.dp)) {
                            Icon(if (n <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder, "$n star${if (n == 1) "" else "s"}", tint = Brand.Gold, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = body, onValueChange = { body = it.take(MAX_CHARS) }, modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("What did you buy, and how was the experience?") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Brand.Line),
                )
                Text(
                    if (length < MIN_CHARS) "${MIN_CHARS - length} more characters needed" else "$length/$MAX_CHARS",
                    color = if (length in 1 until MIN_CHARS) Brand.Bad else Brand.Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(enabled = valid && !busy, onClick = { onSave(rating, body.trim()) }) { Text(if (busy) "Saving…" else if (mine != null) "Save changes" else "Post", color = if (valid) Brand.Gold else Brand.Muted) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } },
    )
}
