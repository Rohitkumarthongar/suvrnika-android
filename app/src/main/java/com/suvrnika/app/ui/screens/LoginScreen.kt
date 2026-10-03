package com.suvrnika.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.LocalBagChanged
import com.suvrnika.app.ui.components.LocalConfig
import com.suvrnika.app.ui.components.LocalNav
import com.suvrnika.app.ui.components.LocalRepo
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.components.rememberRunner
import com.suvrnika.app.ui.theme.Brand

/** Same sign-in as the website: a one-time code to mobile or email; new customers then add their name. */
@Composable
fun LoginScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val cfg = LocalConfig.current
    val bagChanged = LocalBagChanged.current
    val runner = rememberRunner()
    var channel by rememberSaveable { mutableStateOf("mobile") }
    var value by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("contact") } // contact → code → signup
    var signupToken by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf("") }
    var hint by rememberSaveable { mutableStateOf<String?>(null) }
    val done = { bagChanged(); nav.popBackStack() }

    val cleanValue = if (channel == "mobile") value.filter(Char::isDigit) else value.trim().lowercase()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Eyebrow(cfg?.company?.name ?: "Suvrnika")
        Text(if (step == "signup") "Welcome! Tell us your name" else "Sign in or create an account", style = MaterialTheme.typography.headlineMedium)
        when (step) {
            "contact" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = channel == "mobile", onClick = { channel = "mobile"; value = "" }, label = { Text("Mobile") })
                    FilterChip(selected = channel == "email", onClick = { channel = "email"; value = "" }, label = { Text("Email") })
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = if (channel == "mobile") it.filter(Char::isDigit).take(10) else it.trim() },
                    label = { Text(if (channel == "mobile") "10-digit mobile number" else "Email address") },
                    prefix = { if (channel == "mobile") Text("+91 ") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (channel == "mobile") KeyboardType.Phone else KeyboardType.Email),
                )
                PrimaryButton("Send code", busy = runner.busy, enabled = cleanValue.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run(onDone = { step = "code" }) { hint = repo.sendCode(channel, cleanValue).devHint; "Code sent." }
                })
                Muted("We'll send a one-time code. Never share it with anyone.")
            }
            "code" -> {
                Muted("Enter the code sent to ${if (channel == "mobile") "+91 $cleanValue" else cleanValue}.")
                hint?.let { Text(it, color = Brand.Gold) }
                OutlinedTextField(
                    value = code, onValueChange = { code = it.filter(Char::isDigit).take(8) }, label = { Text("One-time code") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                PrimaryButton("Verify", busy = runner.busy, enabled = code.trim().length >= 4, modifier = Modifier.fillMaxWidth(), onClick = {
                    runner.run {
                        val r = repo.verifyCode(channel, cleanValue, code.trim())
                        if (r.status == "signup") { signupToken = r.signupToken.orEmpty(); step = "signup"; null }
                        else { done(); "Welcome back, ${r.customer?.name?.substringBefore(' ') ?: ""}." }
                    }
                })
                Row {
                    TextButton(onClick = { step = "contact"; code = "" }) { Text("Change ${if (channel == "mobile") "number" else "email"}", color = Brand.Muted) }
                    TextButton(enabled = !runner.busy, onClick = { runner.run { hint = repo.sendCode(channel, cleanValue).devHint; "A new code is on its way." } }) { Text("Resend code", color = Brand.Gold) }
                }
            }
            else -> {
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = if (channel == "email") it.filter(Char::isDigit).take(10) else it.trim().lowercase().take(120) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (channel == "mobile") "Email (optional)" else "Mobile number (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (channel == "email") KeyboardType.Phone else KeyboardType.Email),
                )
                PrimaryButton("Create account", busy = runner.busy, enabled = name.trim().length >= 2, modifier = Modifier.fillMaxWidth(), onClick = {
                    val cleanExtra = if (channel == "email") extra.filter(Char::isDigit) else extra.trim().lowercase()
                    runner.run { repo.signup(signupToken, name, cleanExtra); done(); "Welcome to ${cfg?.company?.name ?: "Suvrnika"}!" }
                })
            }
        }
    }
}
