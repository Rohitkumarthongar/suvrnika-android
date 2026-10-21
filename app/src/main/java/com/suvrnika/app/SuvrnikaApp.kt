package com.suvrnika.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.razorpay.Checkout
import com.suvrnika.app.data.Repository
import com.suvrnika.app.data.StartupLoader
import com.suvrnika.app.data.TokenStore
import com.suvrnika.app.payment.PaymentCoordinator
import com.suvrnika.app.ui.theme.StoreView

class SuvrnikaApp : Application(), ImageLoaderFactory {
    lateinit var repo: Repository
        private set
    lateinit var payments: PaymentCoordinator
        private set
    lateinit var startup: StartupLoader
        private set

    override fun onCreate() {
        super.onCreate()
        StoreView.init(this)
        repo = Repository(this, TokenStore(this))
        payments = PaymentCoordinator(repo)
        startup = StartupLoader(this, repo).also { it.start() }
        Checkout.preload(applicationContext)
    }

    /** One image loader for the whole app: photos are kept on disk between launches and fade in when they arrive. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(100L * 1024 * 1024).build() }
        .build()
}
