package com.suvrnika.app.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** The Suvrnika customer API. Paths are relative to BuildConfig.API_BASE_URL. */
interface SuvrnikaApi {
    // ---- sign-in
    @POST("api/v1/auth/send-code") suspend fun sendCode(@Body body: SendCodeRequest): DevHint
    @POST("api/v1/auth/verify-code") suspend fun verifyCode(@Body body: VerifyCodeRequest): AuthResult
    @POST("api/v1/auth/signup") suspend fun signup(@Body body: SignupRequest): AuthResult
    @POST("api/v1/auth/logout") suspend fun logout(@Body body: RefreshRequest): ActionResult
    @POST("api/v1/auth/logout-all") suspend fun logoutAll(): ActionResult

    // ---- public storefront
    @GET("api/v1/config") suspend fun config(): AppConfig
    @GET("api/v1/rates") suspend fun rates(): Rates
    @GET("api/v1/catalogue") suspend fun catalogue(
        @Query("metal") metal: String? = null, @Query("cat") category: String? = null, @Query("tag") tag: String? = null,
        @Query("band") band: String? = null, @Query("q") query: String? = null, @Query("sort") sort: String? = null,
    ): CatalogueResponse
    @GET("api/v1/products/{sku}") suspend fun product(@Path("sku") sku: String): ProductResponse
    @GET("api/v1/branches") suspend fun branches(): BranchesResponse
    @GET("api/v1/offers") suspend fun offers(): OffersResponse
    @GET("api/v1/offers/{slug}") suspend fun offer(@Path("slug") slug: String): OfferResponse
    @GET("api/v1/journal") suspend fun journal(): PostsResponse
    @GET("api/v1/journal/{slug}") suspend fun post(@Path("slug") slug: String): PostResponse

    // ---- bag & wishlist (signed in, or guest via X-Guest-Token)
    @GET("api/v1/bag") suspend fun bag(@Query("usePoints") usePoints: Boolean = false, @Query("useScheme") useScheme: Boolean = false): Bag
    @POST("api/v1/bag/items") suspend fun addToBag(@Body body: AddToBagRequest): ActionResult
    @PATCH("api/v1/bag/items/{lineId}") suspend fun setQty(@Path("lineId") lineId: String, @Body body: QtyRequest): ActionResult
    @DELETE("api/v1/bag/items/{lineId}") suspend fun removeLine(@Path("lineId") lineId: String): ActionResult
    @GET("api/v1/wishlist") suspend fun wishlist(): WishlistResponse
    @POST("api/v1/wishlist/{itemId}") suspend fun toggleWish(@Path("itemId") itemId: String): ActionResult
    @POST("api/v1/wishlist/{itemId}/move-to-bag") suspend fun moveToBag(@Path("itemId") itemId: String): ActionResult

    // ---- checkout & payments (money-moving calls carry an Idempotency-Key)
    @POST("api/v1/checkout") suspend fun checkout(@Header("Idempotency-Key") key: String, @Body body: CheckoutRequest): CheckoutResponse
    @GET("api/v1/payments/{id}") suspend fun payment(@Path("id") id: String): PaymentStatusResponse
    @POST("api/v1/payments/{id}/verify") suspend fun verifyPayment(@Path("id") id: String, @Body body: VerifyPaymentRequest): PaymentStatusResponse
    @POST("api/v1/payments/{id}/cancel") suspend fun cancelPayment(@Path("id") id: String): PaymentStatusResponse
    @POST("api/v1/payments/{id}/mock") suspend fun mockPay(@Path("id") id: String): PaymentStatusResponse

    // ---- account
    @GET("api/v1/me") suspend fun me(): Profile
    @PATCH("api/v1/me") suspend fun updateMe(@Body body: ProfileUpdate): Profile
    @POST("api/v1/me/email/send-code") suspend fun sendEmailCode(@Body body: EmailRequest): DevHint
    @POST("api/v1/me/email/verify") suspend fun verifyEmail(@Body body: EmailVerifyRequest): ActionResult
    @Multipart @POST("api/v1/me/kyc") suspend fun submitKyc(
        @Part("pan") pan: RequestBody, @Part("addrType") addrType: RequestBody, @Part("addrLast4") addrLast4: RequestBody,
        @Part("consent") consent: RequestBody, @Part panFile: MultipartBody.Part, @Part addrFile: MultipartBody.Part,
    ): ActionResult
    @PUT("api/v1/me/home-branch") suspend fun setHomeBranch(@Body body: HomeBranchRequest): ActionResult

    @GET("api/v1/wallet") suspend fun wallet(): Wallet
    @GET("api/v1/wallet/history") suspend fun history(@Query("filter") filter: String, @Query("before") before: String? = null, @Query("limit") limit: Int = 50): History
    @POST("api/v1/wallet/buy-metal") suspend fun buyMetal(@Header("Idempotency-Key") key: String, @Body body: BuyMetalRequest): PaymentStartResponse
    @POST("api/v1/wallet/convert-points") suspend fun convertPoints(@Header("Idempotency-Key") key: String, @Body body: PointsRequest): ActionResult
    @POST("api/v1/wallet/sell-gold") suspend fun sellGold(@Header("Idempotency-Key") key: String, @Body body: SellGoldRequest): ActionResult

    @GET("api/v1/plans") suspend fun plans(): Plans
    @POST("api/v1/plans/join") suspend fun joinPlan(@Header("Idempotency-Key") key: String, @Body body: JoinPlanRequest): PaymentStartResponse
    @POST("api/v1/plans/{id}/pay") suspend fun payInstalment(@Header("Idempotency-Key") key: String, @Path("id") enrollmentId: String): PaymentStartResponse

    @GET("api/v1/orders") suspend fun orders(): OrdersResponse
    @GET("api/v1/orders/{id}") suspend fun order(@Path("id") id: String): OrderResponse
    @GET("api/v1/invoices") suspend fun invoices(): InvoicesResponse
    @Streaming @GET("api/v1/invoices/{orderId}") suspend fun invoicePdf(@Path("orderId") orderId: String): ResponseBody

    @GET("api/v1/payment-methods") suspend fun paymentMethods(): PaymentMethods
    @POST("api/v1/payment-methods/mandates") suspend fun linkMandate(@Body body: MandateRequest): ActionResult
    @POST("api/v1/payment-methods/mandates/{id}/toggle") suspend fun toggleMandate(@Path("id") id: String): ActionResult
    @DELETE("api/v1/payment-methods/cards/{id}") suspend fun removeCard(@Path("id") id: String): ActionResult
    @PUT("api/v1/payment-methods/bank") suspend fun saveBank(@Body body: BankRequest): ActionResult
    @DELETE("api/v1/payment-methods/bank") suspend fun removeBank(): ActionResult
}
