package com.suvrnika.app.payment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.razorpay.Checkout
import com.suvrnika.app.data.ApiException
import com.suvrnika.app.data.PaymentStart
import com.suvrnika.app.data.PaymentStatus
import com.suvrnika.app.data.Repository
import com.suvrnika.app.data.VerifyPaymentRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Collects a payment the server started (checkout, digital gold, plan instalment).
 * The server stays the authority: the app only forwards the provider's result for verification,
 * and the Razorpay / Cashfree webhook completes the payment even if the app is killed half-way.
 */
class PaymentCoordinator(private val repo: Repository) {
    sealed interface SdkResult {
        data class Success(val paymentId: String, val orderId: String, val signature: String) : SdkResult
        data class Failure(val code: Int, val description: String?) : SdkResult
    }

    var activity: Activity? = null
    private var waiting: CompletableDeferred<SdkResult>? = null

    /** Non-null while the development-only mock payment sheet should be shown. */
    private val _mockRequest = MutableStateFlow<PaymentStart?>(null)
    val mockRequest: StateFlow<PaymentStart?> = _mockRequest
    private var mockAnswer: CompletableDeferred<Boolean>? = null

    fun answerMock(pay: Boolean) {
        _mockRequest.value = null
        mockAnswer?.complete(pay)
    }

    fun onSdkResult(result: SdkResult) {
        waiting?.complete(result)
    }

    // Cashfree: the payment happens in the browser; coming back to the app (paused, then resumed) means "check now".
    private var returned: CompletableDeferred<Unit>? = null
    private var leftApp = false
    fun onAppPaused() { if (returned != null) leftApp = true }
    fun onAppResumed() { if (leftApp) { leftApp = false; returned?.complete(Unit) } }

    /** Runs the payment to a final state and returns it (PAID, FAILED, or CREATED if still settling). */
    suspend fun collect(start: PaymentStart): PaymentStatus {
        repo.tokens.pendingPaymentId = start.paymentId
        val status = when (start.mode) {
            "done" -> status(start.paymentId)
            "mock" -> {
                val answer = CompletableDeferred<Boolean>().also { mockAnswer = it }
                _mockRequest.value = start
                if (answer.await()) repo.call { mockPay(start.paymentId) }.payment else repo.call { cancelPayment(start.paymentId) }.payment
            }
            "razorpay" -> razorpay(start)
            "cashfree" -> cashfree(start)
            else -> throw ApiException("Online payments are not available right now.")
        }
        if (status.status != "CREATED") repo.tokens.pendingPaymentId = null
        return status
    }

    private suspend fun razorpay(start: PaymentStart): PaymentStatus {
        val act = activity ?: throw ApiException("Please try again.")
        val result = CompletableDeferred<SdkResult>().also { waiting = it }
        withContext(Dispatchers.Main) {
            val options = JSONObject().apply {
                put("name", start.name)
                put("description", start.description)
                put("order_id", start.razorpayOrderId)
                put("amount", start.amountPaise)
                put("currency", "INR")
                start.prefill?.let { p ->
                    put("prefill", JSONObject().apply {
                        put("name", p.name)
                        p.email?.let { put("email", it) }
                        p.contact?.let { put("contact", it) }
                    })
                }
            }
            Checkout().apply { setKeyID(start.keyId) }.open(act, options)
        }
        return when (val r = result.await()) {
            is SdkResult.Success -> repo.call {
                verifyPayment(start.paymentId, VerifyPaymentRequest(r.orderId, r.paymentId, r.signature))
            }.payment
            is SdkResult.Failure ->
                // Only a deliberate cancel releases the reservation right away. Other errors (network, app
                // switched) may still have captured money, so ask the server instead of guessing.
                if (r.code == Checkout.PAYMENT_CANCELED) repo.call { cancelPayment(start.paymentId) }.payment else settle(start.paymentId)
        }
    }

    /**
     * Cashfree: opens the store's secure payment page (Cashfree checkout: UPI, cards, net banking, wallets…) in the
     * browser, waits for the customer to come back, then lets the server confirm with Cashfree. A UPI payment can
     * take a few seconds, so PENDING is checked again; if it is still settling, the payment stays pending and the
     * webhook finishes it (reconcilePending reports it later).
     */
    private suspend fun cashfree(start: PaymentStart): PaymentStatus {
        val act = activity ?: throw ApiException("Please try again.")
        val url = start.checkoutUrl ?: throw ApiException("Online payments are not available right now.")
        val back = CompletableDeferred<Unit>().also { returned = it; leftApp = false }
        try {
            withContext(Dispatchers.Main) { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            back.await()
        } catch (e: ActivityNotFoundException) {
            throw ApiException("Install or enable a web browser to pay.")
        } finally {
            returned = null
        }
        repeat(6) {
            val r = repo.call { confirmPayment(start.paymentId) }
            when (r.status) {
                "PAID" -> return r.payment
                // Nothing was paid (page closed): release the reservation now, like cancelling the Razorpay sheet.
                "FAILED" -> return repo.call { cancelPayment(start.paymentId) }.payment
            }
            delay(2500)
        }
        return status(start.paymentId)
    }

    private suspend fun status(id: String) = repo.call { payment(id) }.payment

    /** Polls briefly for the webhook to land. */
    private suspend fun settle(id: String): PaymentStatus {
        var s = status(id)
        repeat(5) {
            if (s.status != "CREATED") return s
            delay(2000)
            s = status(id)
        }
        return s
    }

    /** After a restart or returning to the app: report how a payment left in flight ended. */
    suspend fun reconcilePending(): PaymentStatus? {
        val id = repo.tokens.pendingPaymentId ?: return null
        if (!repo.tokens.signedIn.value) return null
        val s = runCatching { status(id) }.getOrElse { e ->
            if (e is ApiException && e.status == 404) repo.tokens.pendingPaymentId = null
            return null
        }
        if (s.status != "CREATED") repo.tokens.pendingPaymentId = null
        return s.takeIf { it.status != "CREATED" }
    }
}
