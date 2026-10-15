package com.suvrnika.app.data

import com.suvrnika.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}

private val JSON_TYPE = "application/json".toMediaType()

/** Adds the access token (signed in) or the guest id (browsing as a guest) to every API call. */
private class AuthHeaders(private val store: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val b = chain.request().newBuilder().header("Accept", "application/json")
        val access = store.accessToken
        if (access != null) b.header("Authorization", "Bearer $access") else b.header("X-Guest-Token", store.guestToken)
        return chain.proceed(b.build())
    }
}

/**
 * On 401, exchanges the refresh token for a new pair once and retries. Refresh tokens rotate, so
 * concurrent 401s are serialised: a request that lost the race just retries with the token the winner saved.
 * If the server refuses the refresh, the device is signed out.
 */
private class TokenRefresher(private val store: TokenStore, private val baseUrl: String) : Authenticator {
    private val plain = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (response.priorResponse != null) return null // already retried once
        synchronized(this) {
            val current = store.accessToken ?: return null
            if (current != sent) return response.request.newBuilder().header("Authorization", "Bearer $current").build()
            val refresh = store.refreshToken ?: return null
            val req = Request.Builder().url(baseUrl + "api/v1/auth/refresh")
                .post(AppJson.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh)).toRequestBody(JSON_TYPE)).build()
            return try {
                plain.newCall(req).execute().use { r ->
                    val text = r.body?.string().orEmpty()
                    when {
                        r.isSuccessful -> {
                            val tokens = AppJson.decodeFromString(TokensResponse.serializer(), text).tokens
                            store.save(tokens)
                            response.request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build()
                        }
                        r.code == 401 -> { store.clear(); null }
                        else -> null // server trouble: keep the session, let the call fail and be retried later
                    }
                }
            } catch (e: Exception) {
                null // offline: keep the session
            }
        }
    }
}

fun createApi(store: TokenStore, baseUrl: String = BuildConfig.API_BASE_URL): Pair<SuvrnikaApi, OkHttpClient> {
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        // OkHttp's default is 10 s per write, which a slow mobile uplink can exceed while sending a photo.
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(AuthHeaders(store))
        .authenticator(TokenRefresher(store, baseUrl))
        .build()
    val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory(JSON_TYPE))
        .build()
        .create(SuvrnikaApi::class.java)
    return api to client
}
