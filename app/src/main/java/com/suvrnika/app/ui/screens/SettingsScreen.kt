package com.suvrnika.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.suvrnika.app.ui.components.Eyebrow
import com.suvrnika.app.ui.components.GhostButton
import com.suvrnika.app.ui.components.KeyValue
import com.suvrnika.app.ui.components.LocalUpdates
import com.suvrnika.app.ui.components.Muted
import com.suvrnika.app.ui.components.Panel
import com.suvrnika.app.ui.components.PrimaryButton
import com.suvrnika.app.ui.theme.Brand
import com.suvrnika.app.ui.theme.StoreView

/** App settings: the version, "Check for updates", and the Men / Women shop view. */
@Composable
fun SettingsScreen() {
    val updates = LocalUpdates.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel {
            Eyebrow("App updates", Brand.Ink)
            KeyValue("Installed version", updates.versionName)
            val u = updates.update
            if (u != null) {
                Row(Modifier.fillMaxWidth().background(Brand.OkBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Version ${u.versionName} is available.", Modifier.weight(1f), color = Brand.Ok, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                PrimaryButton(if (updates.progress != null) "Downloading… ${((updates.progress ?: 0f) * 100).toInt()}%" else "Update now", gold = true,
                    busy = false, enabled = updates.progress == null, modifier = Modifier.fillMaxWidth(), onClick = updates::install)
            }
            GhostButton(if (updates.checking) "Checking…" else "Check for updates", enabled = !updates.checking, modifier = Modifier.fillMaxWidth(), onClick = updates::manualCheck)
            if (updates.isDevelopmentBuild) Muted("This is a development build. To receive updates, uninstall it once and install the latest release.")
            else Muted("The app also checks for updates on its own and lets you know when one is ready.")
        }
        Panel {
            Eyebrow("Shop view", Brand.Ink)
            Muted("Choose whose jewellery to show first. The app's colours follow your choice.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf<String?>(null) + StoreView.GENDERS).forEach { g ->
                    FilterChip(
                        selected = StoreView.gender == g, onClick = { StoreView.set(g) },
                        label = { Text(g?.let { StoreView.LABEL[it] } ?: "Everyone") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand.Ink, selectedLabelColor = Color.White),
                    )
                }
            }
        }
        Text("Shree Jewellers ${updates.versionName}", style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
