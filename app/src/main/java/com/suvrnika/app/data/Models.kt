package com.suvrnika.app.data

import kotlinx.serialization.Serializable

/*
 * DTOs for the Suvrnika customer API (/api/v1). Units match the server and the website:
 * money in PAISE, metal in MILLIGRAMS, purity in PER-MILLE, making charge in BASIS POINTS.
 * Every amount shown in the app comes from the server; the app never prices anything itself
 * (the metal calculator is the one estimate tool, exactly as on the website).
 */

// ---------------------------------------------------------------- errors
@Serializable data class ApiErrorBody(val error: ApiErrorDetail? = null)
@Serializable data class ApiErrorDetail(val code: String = "", val message: String = "")

// ---------------------------------------------------------------- auth
@Serializable data class DeviceInfo(val name: String? = null, val platform: String = "android")
@Serializable data class SendCodeRequest(val channel: String, val value: String)
@Serializable data class VerifyCodeRequest(val channel: String, val value: String, val code: String, val device: DeviceInfo)
@Serializable data class SignupRequest(val signupToken: String, val name: String, val extra: String = "", val device: DeviceInfo)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class DevHint(val devHint: String? = null)
@Serializable data class Tokens(val accessToken: String, val accessTokenExpiresIn: Int = 900, val refreshToken: String, val tokenType: String = "Bearer")
@Serializable data class TokensResponse(val tokens: Tokens)
@Serializable data class AuthResult(val status: String, val signupToken: String? = null, val tokens: Tokens? = null, val customer: Profile? = null)

// ---------------------------------------------------------------- config & content
@Serializable data class Rates(val goldPaisePerGram: Long = 0, val silverPaisePerGram: Long = 0, val gstBasisPoints: Int = 300)
@Serializable data class Company(
    val name: String = "SUVRNIKA", val tag: String = "", val legal: String = "", val logoUrl: String = "", val address: String = "",
    val phone: String = "", val whatsapp: String = "", val email: String = "", val hours: String = "", val gstin: String = "", val bis: String = "",
    val announce: String = "", val hero1: String = "", val hero2: String = "", val heroImg: String = "",
)
@Serializable data class Faq(val question: String, val answer: String)
@Serializable data class SocialUrls(val instagram: String = "", val facebook: String = "", val youtube: String = "", val whatsapp: String = "")
@Serializable data class Support(
    val deliveryInfo: String = "", val paymentInfo: String = "", val returnsInfo: String = "", val faqItems: List<Faq> = emptyList(),
    val socialUrls: SocialUrls = SocialUrls(), val cookiePolicy: String = "", val cookieSettings: String = "", val contactContent: String = "", val helpContent: String = "",
)
@Serializable data class PointsConfig(
    val earnPer100: Double = 1.0, val earnBase: String = "PAYABLE", val pointValuePaise: Long = 100, val maxRedeemPct: Int = 10, val minRedeem: Long = 100,
    val convertToGold: Boolean = true, val planEarnPer100: Double = 0.0, val signupBonus: Long = 0, val expiryMonths: Int = 12,
)
@Serializable data class Rules(val metalNeedsKyc: Boolean = true, val panAbovePaise: Long = 0)
@Serializable data class Popup(val enabled: Boolean = false, val delaySeconds: Int = 10, val headline: String = "", val sub: String = "", val schemeId: String? = null, val imageUrl: String = "")
@Serializable data class PayMethods(val upi: Boolean = true, val card: Boolean = true, val netbanking: Boolean = true, val emi: Boolean = true, val autopay: Boolean = true)
@Serializable data class PaymentsConfig(val provider: String = "none", val methods: PayMethods = PayMethods())
@Serializable data class Category(val id: String, val name: String, val imageUrl: String? = null, val iconPath: String? = null)
@Serializable data class AppConfig(
    val apiVersion: Int = 1, val company: Company = Company(), val support: Support = Support(), val points: PointsConfig = PointsConfig(),
    val rules: Rules = Rules(), val popup: Popup = Popup(), val payments: PaymentsConfig = PaymentsConfig(), val rates: Rates = Rates(),
    val categories: List<Category> = emptyList(),
)

@Serializable data class Offer(
    val id: String, val title: String, val slug: String, val summary: String = "", val body: String = "", val terms: String = "",
    val startsAt: String = "", val endsAt: String = "", val audience: String = "ALL", val ctaLabel: String = "", val ctaUrl: String = "",
)
@Serializable data class OffersResponse(val offers: List<Offer> = emptyList())
@Serializable data class OfferResponse(val offer: Offer)
@Serializable data class Post(val id: String, val title: String, val slug: String, val summary: String = "", val body: String = "", val author: String = "", val publishedAt: String? = null)
@Serializable data class PostsResponse(val posts: List<Post> = emptyList())
@Serializable data class PostResponse(val post: Post)

// ---------------------------------------------------------------- catalogue
@Serializable data class Price(val metalPaise: Long = 0, val makingPaise: Long = 0, val stonePaise: Long = 0, val basePaise: Long = 0, val gstPaise: Long = 0, val totalPaise: Long = 0)
@Serializable data class Item(
    val id: String, val sku: String, val name: String, val categoryId: String = "", val categoryName: String = "", val metal: String = "GOLD",
    val purityLabel: String = "", val purityPermille: Int = 0, val grossMg: Long = 0, val netMg: Long = 0, val stoneLabel: String? = null,
    val makingBp: Int = 0, val tags: List<String> = emptyList(), val imageUrl: String? = null, val description: String? = null,
    val price: Price = Price(), val stock: Int = 0, val wished: Boolean = false, val createdAt: String? = null,
)
@Serializable data class Band(val key: String, val label: String, val kicker: String = "")
@Serializable data class CatalogueResponse(val items: List<Item> = emptyList(), val bands: List<Band> = emptyList())
@Serializable data class Availability(val branchId: String, val city: String, val qty: Int)
@Serializable data class ProductResponse(
    val item: Item, val gstBasisPoints: Int = 300, val pointsEarned: Long = 0, val pointsEarnedValuePaise: Long = 0,
    val availability: List<Availability> = emptyList(), val related: List<Item> = emptyList(),
)
@Serializable data class Branch(
    val id: String, val code: String = "", val name: String, val city: String = "", val address: String? = null, val phone: String? = null,
    val hours: String? = null, val lat: Double? = null, val lng: Double? = null,
)
@Serializable data class BranchesResponse(val branches: List<Branch> = emptyList(), val homeBranchId: String? = null)
@Serializable data class Scheme(
    val id: String, val name: String, val tagline: String? = null, val type: String = "MONTHLY", val tenureMonths: Int = 0, val bonusMonths: Double = 0.0,
    val accrual: String = "AMOUNT", val minPaise: Long = 0, val maxPaise: Long? = null, val stepPaise: Long? = null, val graceDays: Int = 0,
    val missRule: String = "EXTEND", val redeemOn: String = "ANY", val makingOffPct: Int = 0, val autopay: Boolean = false, val imageUrl: String? = null,
)

// ---------------------------------------------------------------- bag, wishlist, checkout
@Serializable data class AddToBagRequest(val itemId: String, val qty: Int = 1)
@Serializable data class QtyRequest(val qty: Int)
@Serializable data class BagLine(val lineId: String, val qty: Int, val stock: Int, val lineBasePaise: Long, val item: Item)
@Serializable data class BagTotals(
    val subtotalPaise: Long = 0, val gstPaise: Long = 0, val gstBasisPoints: Int = 300, val pointsUsed: Long = 0, val pointsValuePaise: Long = 0,
    val schemeUsedPaise: Long = 0, val payablePaise: Long = 0, val pointsEarned: Long = 0,
)
@Serializable data class Redeem(val points: Long = 0, val canRedeem: Boolean = false, val minRedeem: Long = 0, val maxRedeemPct: Int = 0, val schemeCreditPaise: Long = 0)
@Serializable data class OutOfStock(val lineId: String, val name: String, val available: Int)
@Serializable data class PayMethod(val key: String, val label: String, val sub: String = "")
@Serializable data class Bag(
    val signedIn: Boolean = false, val lines: List<BagLine> = emptyList(), val count: Int = 0, val totals: BagTotals = BagTotals(),
    val redeem: Redeem = Redeem(), val panBlocked: Boolean = false, val panAbovePaise: Long = 0,
    val outOfStock: List<OutOfStock> = emptyList(), val methods: List<PayMethod> = emptyList(),
)
@Serializable data class WishlistResponse(val signedIn: Boolean = false, val items: List<Item> = emptyList())
@Serializable data class CheckoutRequest(val usePoints: Boolean, val useScheme: Boolean, val method: String)

@Serializable data class Prefill(val name: String = "", val email: String? = null, val contact: String? = null)
/** How to collect a payment: "razorpay" (open the SDK), "mock" (development only) or "done" (nothing to pay). */
@Serializable data class PaymentStart(
    val mode: String, val paymentId: String, val razorpayOrderId: String? = null, val keyId: String? = null, val amountPaise: Long = 0,
    val name: String = "", val description: String = "", val prefill: Prefill? = null,
)
@Serializable data class CheckoutResponse(val orderId: String, val orderNumber: String, val payablePaise: Long = 0, val payment: PaymentStart)
@Serializable data class PaymentStartResponse(val payment: PaymentStart)
@Serializable data class OrderRef(val id: String, val number: String, val status: String)
@Serializable data class PaymentStatus(
    val id: String, val purpose: String, val status: String, val amountPaise: Long = 0, val method: String? = null,
    val createdAt: String? = null, val paidAt: String? = null, val order: OrderRef? = null,
)
@Serializable data class PaymentStatusResponse(val payment: PaymentStatus)
@Serializable data class VerifyPaymentRequest(val razorpayOrderId: String, val razorpayPaymentId: String, val razorpaySignature: String)

// ---------------------------------------------------------------- account
@Serializable data class Kyc(
    val status: String = "NOT_STARTED", val panMasked: String? = null, val addrType: String? = null, val addrLast4: String? = null,
    val note: String? = null, val submittedAt: String? = null,
)
@Serializable data class Profile(
    val id: String, val name: String, val mobile: String? = null, val email: String? = null, val emailVerified: Boolean = false,
    val dob: String? = null, val nominee: String? = null, val address: String? = null, val homeBranch: Branch? = null,
    val memberSince: String? = null, val kyc: Kyc = Kyc(), val message: String? = null,
)
@Serializable data class ProfileUpdate(val name: String, val dob: String, val nominee: String, val address: String)
@Serializable data class EmailRequest(val email: String)
@Serializable data class EmailVerifyRequest(val email: String, val code: String)
@Serializable data class HomeBranchRequest(val branchId: String)

@Serializable data class Balances(val cashPaise: Long = 0, val points: Long = 0, val goldMg: Long = 0, val silverMg: Long = 0, val schemeCreditPaise: Long = 0)
@Serializable data class Values(val goldPaise: Long = 0, val silverPaise: Long = 0, val pointsPaise: Long = 0)
@Serializable data class WalletPoints(val pointValuePaise: Long = 100, val expiryMonths: Int = 0, val convertToGold: Boolean = false)
@Serializable data class LedgerEntry(
    val id: String, val account: String, val delta: Long, val balanceAfter: Long, val kind: String, val description: String,
    val reference: String? = null, val createdAt: String? = null,
)
@Serializable data class Wallet(
    val totalValuePaise: Long = 0, val balances: Balances = Balances(), val values: Values = Values(), val rates: Rates = Rates(),
    val points: WalletPoints = WalletPoints(), val kycLocked: Boolean = true, val recent: List<LedgerEntry> = emptyList(),
)
@Serializable data class HistoryStats(
    val ordersPlaced: Long = 0, val ordersTotalPaise: Long = 0, val paidIntoPlansPaise: Long = 0, val plansCount: Long = 0,
    val goldMg: Long = 0, val goldValuePaise: Long = 0, val points: Long = 0,
)
@Serializable data class History(val stats: HistoryStats = HistoryStats(), val entries: List<LedgerEntry> = emptyList(), val nextCursor: String? = null)
@Serializable data class BuyMetalRequest(val metal: String, val amountPaise: Long)
@Serializable data class PointsRequest(val points: Long)
@Serializable data class SellGoldRequest(val mg: Long)
@Serializable data class ActionResult(
    val ok: Boolean = true, val message: String? = null, val count: Int? = null, val added: Boolean? = null,
    val goldMg: Long? = null, val amountPaise: Long? = null,
)

@Serializable data class Enrollment(
    val id: String, val scheme: Scheme, val status: String, val instalmentPaise: Long, val tenure: Int, val paidCount: Int,
    val accrual: String = "AMOUNT", val accruedMg: Long = 0, val bonusPaise: Long = 0, val paidSoFarPaise: Long = 0, val redeemValuePaise: Long = 0,
    val startedAt: String? = null, val maturedAt: String? = null,
)
@Serializable data class Plans(val enrollments: List<Enrollment> = emptyList(), val available: List<Scheme> = emptyList())
@Serializable data class JoinPlanRequest(val schemeId: String, val instalmentPaise: Long)

@Serializable data class Stage(val key: String, val label: String)
@Serializable data class OrderBranch(val id: String, val name: String, val city: String)
@Serializable data class OrderLine(val itemId: String, val name: String, val qty: Int, val unitPricePaise: Long, val netMg: Long = 0)
@Serializable data class OrderEvent(val status: String, val label: String, val timestamp: String)
@Serializable data class Order(
    val id: String, val number: String, val status: String, val statusLabel: String = "", val stageIndex: Int = -1, val stages: List<Stage> = emptyList(),
    val subtotalPaise: Long = 0, val gstPaise: Long = 0, val pointsUsed: Long = 0, val pointsValuePaise: Long = 0, val schemeUsedPaise: Long = 0,
    val payablePaise: Long = 0, val pointsEarned: Long = 0, val paymentMethod: String? = null, val branch: OrderBranch? = null,
    val createdAt: String? = null, val paidAt: String? = null, val invoiceAvailable: Boolean = false,
    val lines: List<OrderLine> = emptyList(), val history: List<OrderEvent> = emptyList(),
)
@Serializable data class OrdersResponse(val orders: List<Order> = emptyList())
@Serializable data class OrderResponse(val order: Order)
@Serializable data class Invoice(val orderId: String, val orderNumber: String, val invoiceNumber: String, val issuedAt: String? = null, val paidAt: String? = null, val payablePaise: Long = 0)
@Serializable data class InvoicesResponse(val invoices: List<Invoice> = emptyList())

@Serializable data class Mandate(val id: String, val label: String, val upiMasked: String, val maxPaise: Long, val status: String)
@Serializable data class SavedCard(val id: String, val brand: String, val last4: String, val expiry: String)
@Serializable data class BankAccount(val holder: String, val accountMasked: String, val ifsc: String, val status: String)
@Serializable data class PaymentMethods(
    val mandates: List<Mandate> = emptyList(), val cards: List<SavedCard> = emptyList(), val bank: BankAccount? = null, val kycVerified: Boolean = false,
)
@Serializable data class MandateRequest(val upi: String, val maxPaise: Long)
@Serializable data class BankRequest(val holder: String, val accountNumber: String, val ifsc: String)
