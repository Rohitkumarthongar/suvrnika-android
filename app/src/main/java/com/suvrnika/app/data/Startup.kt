package com.suvrnika.app.data

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import com.suvrnika.app.ui.theme.StoreView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opening screen loader (same idea as the website's opening screen): while the app icon shows, fetch the store
 * settings and download the photos the first screen shows (logo, hero, For her / For him cards, the first products)
 * into Coil's cache, so Home opens complete instead of filling in piece by piece. Never waits more than 4 s — on a
 * slow or offline connection the app opens anyway and photos fade in as they arrive.
 */
class StartupLoader(private val context: Context, private val repo: Repository) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** Store settings fetched during start-up, so Home's first frame already has the brand and photos. */
    @Volatile var firstConfig: AppConfig? = null
        private set

    fun start() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            withTimeoutOrNull(4_000) {
                val cfg = async { runCatching { repo.call { config() } }.getOrNull() }
                val items = async { runCatching { repo.call { catalogue(gender = StoreView.gender) }.items }.getOrDefault(emptyList()) }
                val c = cfg.await()?.also { firstConfig = it }
                val photos = buildList<String?> {
                    c?.company?.let { add(it.logoUrl); add(it.heroImg) }
                    c?.homeImages?.let { addAll(it.her.take(1)); addAll(it.him.take(1)) }
                    items.await().take(6).forEach { add(it.imageUrl) }
                }.mapNotNull { repo.imageUrl(it?.ifBlank { null }) }.distinct()
                photos.map { url ->
                    async { runCatching { context.imageLoader.execute(ImageRequest.Builder(context).data(url).build()) } }
                }.awaitAll()
            }
            _ready.value = true
        }
    }
}
