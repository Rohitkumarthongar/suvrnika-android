package com.suvrnika.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Session credentials, encrypted at rest with an Android Keystore-backed key.
 * Tokens are never logged and are excluded from backups (see data_extraction_rules.xml).
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "suvrnika_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _signedIn = MutableStateFlow(prefs.getString(REFRESH, null) != null)
    /** True while this device holds a session. Flips to false when the server ends it. */
    val signedIn: StateFlow<Boolean> = _signedIn

    val accessToken: String? get() = prefs.getString(ACCESS, null)
    val refreshToken: String? get() = prefs.getString(REFRESH, null)

    /** Anonymous id for a guest bag/wishlist; merged into the account at sign-in. */
    val guestToken: String
        get() = prefs.getString(GUEST, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(GUEST, it).apply() }

    /** A payment the app started but has not seen finish (survives process death; reconciled on resume). */
    var pendingPaymentId: String?
        get() = prefs.getString(PENDING_PAYMENT, null)
        set(value) { prefs.edit().apply { if (value == null) remove(PENDING_PAYMENT) else putString(PENDING_PAYMENT, value) }.apply() }

    @Synchronized fun save(tokens: Tokens) {
        prefs.edit().putString(ACCESS, tokens.accessToken).putString(REFRESH, tokens.refreshToken).commit()
        _signedIn.value = true
    }

    @Synchronized fun clear() {
        // A new guest id after sign-out, so the next shopper doesn't inherit this account's merged guest bag.
        prefs.edit().remove(ACCESS).remove(REFRESH).remove(PENDING_PAYMENT).putString(GUEST, UUID.randomUUID().toString()).commit()
        _signedIn.value = false
    }

    private companion object {
        const val ACCESS = "access"
        const val REFRESH = "refresh"
        const val GUEST = "guest"
        const val PENDING_PAYMENT = "pending_payment"
    }
}
