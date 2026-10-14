package com.suvrnika.app.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.UUID

/** A failure with a message that is safe to show the customer (it comes from the server's error envelope). */
class ApiException(message: String, val code: String = "", val status: Int = 0) : Exception(message) {
    val needsSignIn get() = status == 401
}

/** A local file chosen by the customer for KYC upload. */
class Upload(val name: String, val mimeType: String, val bytes: ByteArray)

/**
 * Single entry point to the server. The app keeps no business data of its own: every screen reads
 * from here, so the app always shows exactly what the website shows for the same account.
 */
class Repository(private val context: Context, val tokens: TokenStore) {
    val api: SuvrnikaApi
    /** App updates from the store's GitHub releases. */
    val updater = AppUpdater(context)
    private val http: okhttp3.OkHttpClient

    init {
        val (a, c) = createApi(tokens)
        api = a
        http = c
    }

    private val device get() = DeviceInfo(name = "${Build.MANUFACTURER} ${Build.MODEL}".take(80))

    /** Runs an API call, converting HTTP and network failures into [ApiException]. */
    suspend fun <T> call(block: suspend SuvrnikaApi.() -> T): T = withContext(Dispatchers.IO) {
        try {
            api.block()
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val detail = runCatching { body?.let { AppJson.decodeFromString(ApiErrorBody.serializer(), it).error } }.getOrNull()
            val message = detail?.message?.takeIf { it.isNotBlank() } ?: when (e.code()) {
                401 -> "Please sign in to continue."
                404 -> "Not found."
                else -> "Something went wrong. Please try again."
            }
            throw ApiException(message, detail?.code.orEmpty(), e.code())
        } catch (e: IOException) {
            throw ApiException("You appear to be offline. Check your connection and try again.", "offline")
        }
    }

    /** A fresh key per customer action; network retries of that same action reuse it. */
    fun newIdempotencyKey(): String = UUID.randomUUID().toString()

    // ---- sign-in
    suspend fun sendCode(channel: String, value: String) = call { sendCode(SendCodeRequest(channel, value)) }

    suspend fun verifyCode(channel: String, value: String, code: String): AuthResult =
        call { verifyCode(VerifyCodeRequest(channel, value, code, device)) }.also { it.tokens?.let(tokens::save) }

    suspend fun signup(signupToken: String, name: String, extra: String): AuthResult =
        call { signup(SignupRequest(signupToken, name, extra, device)) }.also { it.tokens?.let(tokens::save) }

    suspend fun signOut() {
        val refresh = tokens.refreshToken
        runCatching { if (refresh != null) call { logout(RefreshRequest(refresh)) } }
        tokens.clear()
    }

    suspend fun signOutEverywhere(): ActionResult = call { logoutAll() }.also { tokens.clear() }

    // ---- KYC
    suspend fun submitKyc(pan: String, addrType: String, addrLast4: String, panFile: Upload, addrFile: Upload): ActionResult {
        val text = "text/plain".toMediaType()
        fun part(field: String, u: Upload) = MultipartBody.Part.createFormData(field, u.name, u.bytes.toRequestBody(u.mimeType.toMediaType()))
        return call {
            submitKyc(pan.toRequestBody(text), addrType.toRequestBody(text), addrLast4.toRequestBody(text), "true".toRequestBody(text),
                part("panFile", panFile), part("addrFile", addrFile))
        }
    }

    /** Downloads an invoice PDF into the app cache (shared with a viewer through FileProvider). */
    suspend fun downloadInvoice(orderId: String, invoiceNumber: String): File {
        val body = call { invoicePdf(orderId) }
        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val file = File(dir, invoiceNumber.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".pdf")
            body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            file
        }
    }

    /** Resolves server-relative image paths (/media/…) against the API host. */
    fun imageUrl(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http://") || path.startsWith("https://") -> path
        else -> com.suvrnika.app.BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }
}
