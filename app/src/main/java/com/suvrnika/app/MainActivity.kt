package com.suvrnika.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.suvrnika.app.payment.PaymentCoordinator
import com.suvrnika.app.ui.SuvrnikaRoot

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val app get() = application as SuvrnikaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app.payments.activity = this
        setContent { SuvrnikaRoot(app.repo, app.payments) }
    }

    override fun onDestroy() {
        if (app.payments.activity === this) app.payments.activity = null
        super.onDestroy()
    }

    // Razorpay Checkout reports back to the Activity that opened it.
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        app.payments.onSdkResult(
            PaymentCoordinator.SdkResult.Success(
                paymentId = data?.paymentId ?: razorpayPaymentId.orEmpty(),
                orderId = data?.orderId.orEmpty(),
                signature = data?.signature.orEmpty(),
            )
        )
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        app.payments.onSdkResult(PaymentCoordinator.SdkResult.Failure(code, response))
    }
}
