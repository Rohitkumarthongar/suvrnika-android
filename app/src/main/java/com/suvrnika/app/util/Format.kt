package com.suvrnika.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Same formatting as the website (src/lib/money.ts): Indian digit grouping, IST dates. */
private val IN = Locale("en", "IN")
private val IST = ZoneId.of("Asia/Kolkata")
private val group = NumberFormat.getIntegerInstance(IN)

fun inr(paise: Long): String = "₹" + group.format((paise / 100.0).roundToLong())
fun grams(mg: Long): String = String.format(Locale.US, "%.3f g", mg / 1000.0)
fun count(n: Long): String = group.format(n)
fun mobile(m: String?): String = if (m.isNullOrBlank()) "—" else "+91 ${m.take(5)} ${m.drop(5)}"

fun date(iso: String?): String = iso?.let {
    runCatching { DateTimeFormatter.ofPattern("dd MMM yyyy", IN).format(Instant.parse(it).atZone(IST)) }.getOrNull()
} ?: "—"

/** Signed ledger movement: +1,041 pts, −0.500 g, +₹2,000. */
fun delta(account: String, value: Long, signed: Boolean = true): String {
    val s = if (!signed) "" else if (value > 0) "+" else "−"
    val a = abs(value)
    return s + when (account) {
        "POINTS" -> "${group.format(a)} pts"
        "GOLD_MG", "SILVER_MG" -> grams(a)
        else -> inr(a)
    }
}

fun metalLabel(metal: String) = when (metal) { "SILVER" -> "Silver"; "DIAMOND" -> "Diamond"; else -> "Gold" }

/** Rupees typed by the customer → paise (rounded like the website's toPaise). */
fun rupeesToPaise(text: String): Long? = text.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }?.let { (it * 100).roundToLong() }
fun gramsToMg(text: String): Long? = text.trim().toDoubleOrNull()?.takeIf { it > 0 }?.let { (it * 1000).roundToLong() }
