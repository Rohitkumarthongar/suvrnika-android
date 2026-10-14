package com.suvrnika.app

import android.app.Application
import com.razorpay.Checkout
import com.suvrnika.app.data.Repository
import com.suvrnika.app.data.TokenStore
import com.suvrnika.app.payment.PaymentCoordinator
import com.suvrnika.app.ui.theme.StoreView

class SuvrnikaApp : Application() {
    lateinit var repo: Repository
        private set
    lateinit var payments: PaymentCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        StoreView.init(this)
        repo = Repository(this, TokenStore(this))
        payments = PaymentCoordinator(repo)
        Checkout.preload(applicationContext)
    }
}
