package com.suvrnika.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.Profile
import com.suvrnika.app.data.ProfileUpdate
import com.suvrnika.app.data.Upload
import com.suvrnika.app.ui.components.Chip
import com.suvrnika.app.ui.components.Divider
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.Heading
import com.suvrnika.app.ui.components.LoadList
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.SignInPrompt
import com.suvrnika.app.ui.components.Tone
import com.suvrnika.app.ui.components.rememberLoad
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.util.inr
import com.suvrnika.app.util.mobile
import com.suvrnika.app.util.rupeesToPaise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_UPLOAD_BYTES = 15 * 1024 * 1024

// ---------------------------------------------------------------- profile & KYC
@Composable
fun ProfileScreen() {
    val repo = LocalRepo.current
    val signedIn by repo.tokens.signedIn.collectAsState()
    if (!signedIn) return SignInPrompt("Sign in to manage your profile.")
    val state = rememberLoad { call { me() } }
    LoadList(state) { c ->
        item { PersonalDetails(c, onSaved = state::refresh) }
        item { LoginSecurity(c, onChanged = state::refresh) }
        item { KycPanel(c, onSubmitted = state::refresh) }
    }
}

@Composable
private fun PersonalDetails(c: Profile, onSaved: () -> Unit) {
    val repo = LocalRepo.current
    val runner = rememberRunner()
    var name by rememberSaveable(c.id) { mutableStateOf(c.name) }
    var dob by rememberSaveable(c.id) { mutableStateOf(c.dob.orEmpty()) }
    var nominee by rememberSaveable(c.id) { mutableStateOf(c.nominee.orEmpty()) }
    var address by rememberSaveable(c.id) { mutableStateOf(c.address.orEmpty()) }
    Panel {
        Heading("Personal details")
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dob, { dob = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) }, label = { Text("Date of birth (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(nominee, { nominee = it.take(80) }, label = { Text("Nominee") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it.take(300) }, label = { Text("Address") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        PrimaryButton("Save details", busy = runner.busy, modifier = Modifier.fillMaxWidth(), onClick = {
            runner.run(onDone = onSaved) { repo.call { updateMe(ProfileUpdate(name.trim(), dob.trim(), nominee.trim(), address.trim())) }.message }
        })
    }
}

@Composable
private fun LoginSecurity(c: Profile, onChanged: () -> Unit) {
    val repo = LocalRepo.current
    val runner = rememberRunner()
    var email by rememberSaveable(c.id) { mutableStateOf(c.email.orEmpty()) }
    var code by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    var hint by rememberSaveable { mutableStateOf<String?>(null) }
    Panel {
        Heading("Login & security")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Mobile · ${c.mobile?.let(::mobile) ?: "Not added"}", fontWeight = FontWeight.SemiBold); Muted("You sign in with a one-time code sent here") }
            if (c.mobile != null) Chip("Verified", Tone.Ok)
        }
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Email · ${c.email ?: "Not added"}", fontWeight = FontWeight.SemiBold); Muted(if (c.emailVerified) "You can also sign in with this email" else "Verify to sign in with email and get receipts") }
            if (c.email != null) Chip(if (c.emailVerified) "Verified" else "Unverified", if (c.emailVerified) Tone.Ok else Tone.Warn)
        }
        if (!c.emailVerified || c.email == null) {
            OutlinedTextField(email, { email = it.trim().take(120) }, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            if (!sent) GhostButton("Send verification code", enabled = !runner.busy && email.contains('@'), modifier = Modifier.fillMaxWidth(), onClick = {
                runner.run(onDone = { sent = true }) { hint = repo.call { sendEmailCode(com.suvrnika.app.data.EmailRequest(email)) }.devHint; "Code sent to $email." }
            }) else {
                hint?.let { Text(it, color = Brand.Gold, fontSize = 13.sp) }
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = { Text("Code") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                PrimaryButton("Verify email", busy = runner.busy, enabled = code.length >= 4, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = { sent = false; code = ""; onChanged() }) { repo.call { verifyEmail(com.suvrnika.app.data.EmailVerifyRequest(email, code)) }.message }
                })
            }
        }
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Sessions", fontWeight = FontWeight.SemiBold); Muted("Signed in on a device you don't recognise?") }
            TextButton(enabled = !runner.busy, onClick = { runner.run { repo.signOutEverywhere().message } }) { Text("Sign out everywhere", color = Brand.Bad) }
        }
    }
}

private val ADDRESS_PROOFS = listOf("Aadhaar", "Passport", "Voter ID", "Driving licence")

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
}.getOrNull()

/** Longest side of an uploaded photo. A PAN card or Aadhaar stays perfectly readable at this size. */
private const val MAX_PHOTO_EDGE = 2000

private suspend fun readUpload(context: Context, uri: Uri): Upload = withContext(Dispatchers.IO) {
    val cr = context.contentResolver
    var name = "document"
    var size = -1L
    cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cur ->
        if (cur.moveToFirst()) {
            name = cur.getString(0) ?: name
            if (!cur.isNull(1)) size = cur.getLong(1)
        }
    }
    val type = cr.getType(uri) ?: "application/octet-stream"
    // Photos are shrunk here (a phone camera gives 4–12 MB), so the upload is fast, small and fits the server's limits.
    if (type.startsWith("image/")) return@withContext shrinkPhoto(context, uri, name)
    if (size > MAX_UPLOAD_BYTES) throw ApiException("File must be under 15 MB.")
    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: throw ApiException("Could not read that file.")
    if (bytes.size > MAX_UPLOAD_BYTES) throw ApiException("File must be under 15 MB.")
    Upload(name, type, bytes)
}

/** Decodes a photo at reduced size, turns it upright using the camera's orientation, and re-saves it as a JPEG. */
private fun shrinkPhoto(context: Context, uri: Uri, name: String): Upload {
    val cr = context.contentResolver
    val unreadable = ApiException("That image could not be read. Choose a JPG, PNG or PDF.")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // Measuring only: decodeStream returns no bitmap in this mode, so success is judged by the size it fills in.
    (cr.openInputStream(uri) ?: throw ApiException("Could not read that file.")).use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw unreadable
    // Power-of-two downsampling keeps memory small even for a 50-megapixel photo; the exact size is set below.
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_EDGE) sample *= 2
    var bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: throw unreadable
    val orientation = runCatching {
        cr.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest > MAX_PHOTO_EDGE) { val f = MAX_PHOTO_EDGE.toFloat() / longest; postScale(f, f) }
    }
    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    // JPEG has no transparency: put a PNG with a transparent background on white rather than black.
    if (bitmap.hasAlpha()) {
        val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
        bitmap = flat
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    bitmap.recycle()
    return Upload(name.substringBeforeLast('.', name) + ".jpg", "image/jpeg", out.toByteArray())
}

@Composable
private fun KycPanel(c: Profile, onSubmitted: () -> Unit) {
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val context = LocalContext.current
    val runner = rememberRunner()
    val k = c.kyc
    var pan by rememberSaveable { mutableStateOf("") }
    var addrType by rememberSaveable { mutableStateOf(ADDRESS_PROOFS.first()) }
    var last4 by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var panUri by rememberSaveable { mutableStateOf<String?>(null) }
    var addrUri by rememberSaveable { mutableStateOf<String?>(null) }
    val pickPan = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { panUri = it?.toString() }
    val pickAddr = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { addrUri = it?.toString() }
    val panName = remember(panUri) { panUri?.let { displayName(context, Uri.parse(it)) ?: "document chosen" } }
    val addrName = remember(addrUri) { addrUri?.let { displayName(context, Uri.parse(it)) ?: "document chosen" } }
    // Any photo (JPG, PNG, WebP, and phone formats such as HEIC) or a PDF; photos are converted to JPEG before upload.
    val types = arrayOf("image/*", "application/pdf")

    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Heading("KYC verification", Modifier.weight(1f))
            when (k.status) {
                "VERIFIED" -> Chip("Verified", Tone.Ok)
                "UNDER_REVIEW" -> Chip("Under review", Tone.Warn)
                "REJECTED" -> Chip("Rejected", Tone.Bad)
                else -> Chip("Not started")
            }
        }
        Text("Verify once to buy digital gold, withdraw to your bank, and place orders above ${inr(cfg?.rules?.panAbovePaise ?: 0)}.", color = Brand.Body, fontSize = 14.sp)
        if (k.status == "REJECTED") Text("Not approved: ${k.note ?: "Please re-upload clear copies."}", Modifier.fillMaxWidth().background(Brand.BadBg).padding(12.dp), color = Brand.Bad, fontSize = 13.sp)
        when (k.status) {
            "UNDER_REVIEW" -> Text("Documents received — PAN ${k.panMasked} · ${k.addrType} ••••${k.addrLast4}.\nOur team verifies within 24 hours.", Modifier.fillMaxWidth().background(Brand.GoldPale).padding(12.dp), fontSize = 14.sp)
            "VERIFIED" -> Text("You're verified.\nPAN ${k.panMasked} · ${k.addrType} ••••${k.addrLast4}\nDigital gold, bank withdrawals and high-value orders are unlocked.", Modifier.fillMaxWidth().background(Brand.OkBg).padding(12.dp), color = Brand.Ok, fontSize = 14.sp)
            else -> {
                OutlinedTextField(pan, { pan = it.uppercase().filter(Char::isLetterOrDigit).take(10) }, label = { Text("PAN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Muted("Address proof")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ADDRESS_PROOFS.take(2).forEach { t -> FilterChip(addrType == t, { addrType = t }, label = { Text(t) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { ADDRESS_PROOFS.drop(2).forEach { t -> FilterChip(addrType == t, { addrType = t }, label = { Text(t) }) } }
                OutlinedTextField(last4, { last4 = it.filter(Char::isDigit).take(4) }, label = { Text("Last 4 digits of $addrType") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                GhostButton(panName?.let { "PAN: $it" } ?: "Choose PAN card (photo or PDF)", onClick = { pickPan.launch(types) }, modifier = Modifier.fillMaxWidth())
                GhostButton(addrName?.let { "$addrType: $it" } ?: "Choose $addrType document", onClick = { pickAddr.launch(types) }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(consent, { consent = it })
                    Text("I consent to ${cfg?.company?.name ?: "the store"} verifying these documents for KYC. Only masked numbers are stored.", fontSize = 13.sp)
                }
                PrimaryButton("Submit for verification", busy = runner.busy, enabled = pan.length == 10 && last4.length == 4 && consent && panUri != null && addrUri != null,
                    modifier = Modifier.fillMaxWidth(), onClick = {
                        runner.run(onDone = onSubmitted) {
                            repo.submitKyc(pan, addrType, last4, readUpload(context, Uri.parse(panUri!!)), readUpload(context, Uri.parse(addrUri!!))).message
                        }
                    })
            }
        }
    }
}

// ---------------------------------------------------------------- payment methods
@Composable
fun PaymentMethodsScreen() {
    val repo = LocalRepo.current
    val runner = rememberRunner()
    val signedIn by repo.tokens.signedIn.collectAsState()
    if (!signedIn) return SignInPrompt("Sign in to manage payment methods.")
    val state = rememberLoad { call { paymentMethods() } }
    var upi by rememberSaveable { mutableStateOf("") }
    var max by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf("") }
    var acc by rememberSaveable { mutableStateOf("") }
    var ifsc by rememberSaveable { mutableStateOf("") }
    LoadList(state) { pm ->
        item {
            Text("Payments, saved cards and UPI Autopay mandates are handled by Razorpay. We never see or store your full card number or UPI PIN.",
                Modifier.fillMaxWidth().background(Brand.Sand).padding(14.dp), fontSize = 13.sp, color = Brand.Body)
        }
        item {
            Panel {
                Heading("UPI Autopay mandates")
                Muted("Used to collect your savings-plan instalments automatically each month.")
                if (pm.mandates.isEmpty()) Muted("No mandates yet.")
                pm.mandates.forEach { m ->
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(m.label, fontWeight = FontWeight.SemiBold); Muted("${m.upiMasked} · up to ${inr(m.maxPaise)}/month") }
                        Chip(if (m.status == "ACTIVE") "Active" else if (m.status == "PENDING") "Pending" else "Paused", if (m.status == "ACTIVE") Tone.Ok else Tone.Warn)
                        TextButton(enabled = !runner.busy, onClick = { runner.run(onDone = state::refresh) { repo.call { toggleMandate(m.id) }.message } }) { Text(if (m.status == "ACTIVE") "Pause" else "Resume") }
                    }
                }
                Divider()
                OutlinedTextField(upi, { upi = it.trim().take(100) }, label = { Text("UPI ID (name@bank)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(max, { max = it.filter(Char::isDigit).take(6) }, label = { Text("Maximum per month (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PrimaryButton("Authorise via Razorpay", busy = runner.busy, enabled = upi.contains('@') && rupeesToPaise(max) != null, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = { upi = ""; max = ""; state.refresh() }) { repo.call { linkMandate(com.suvrnika.app.data.MandateRequest(upi, rupeesToPaise(max)!!)) }.message }
                })
            }
        }
        item {
            Panel {
                Heading("Saved cards")
                if (pm.cards.isEmpty()) Muted("No saved cards. Cards you choose to save at Razorpay checkout appear here.")
                pm.cards.forEach { k ->
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("${k.brand} •••• ${k.last4}", fontWeight = FontWeight.SemiBold); Muted("Expires ${k.expiry} · tokenised") }
                        TextButton(enabled = !runner.busy, onClick = { runner.run(onDone = state::refresh) { repo.call { removeCard(k.id) }.message } }) { Text("Remove", color = Brand.Bad) }
                    }
                }
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Heading("Bank for withdrawals", Modifier.weight(1f))
                    Chip(pm.bank?.let { if (it.status == "VERIFIED") "Verified" else "Verifying" } ?: "Not added", if (pm.bank?.status == "VERIFIED") Tone.Ok else if (pm.bank != null) Tone.Warn else Tone.Neutral)
                }
                val bank = pm.bank
                if (bank != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(bank.holder, fontWeight = FontWeight.SemiBold); Muted("A/c ${bank.accountMasked} · ${bank.ifsc}") }
                    TextButton(enabled = !runner.busy, onClick = { runner.run(onDone = state::refresh) { repo.call { removeBank() }.message } }) { Text("Remove", color = Brand.Bad) }
                } else if (!pm.kycVerified) Muted("Complete KYC in Profile to add a bank account.")
                else {
                    OutlinedTextField(holder, { holder = it.take(80) }, label = { Text("Account holder") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(acc, { acc = it.filter(Char::isDigit).take(18) }, label = { Text("Account number") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(ifsc, { ifsc = it.uppercase().filter(Char::isLetterOrDigit).take(11) }, label = { Text("IFSC") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("Save bank account", busy = runner.busy, enabled = holder.isNotBlank() && acc.length >= 9 && ifsc.length == 11, modifier = Modifier.fillMaxWidth(), onClick = {
                        runner.run(onDone = state::refresh) { repo.call { saveBank(com.suvrnika.app.data.BankRequest(holder, acc, ifsc)) }.message }
                    })
                }
            }
        }
    }
}
