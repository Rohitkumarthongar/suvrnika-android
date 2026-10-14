# Suvrnika Android app

Native Android app (Kotlin, Jetpack Compose) for **customers**. It has every feature of the
website's customer portal and uses the same account, so a customer sees the same wallet,
plans, orders, bag and wishlist on the phone and on the web.

## How it stays in sync with the website

The app stores no business data of its own. Every screen reads from the server's customer API
(`/api/v1`, see [`docs/mobile-api.md`](../docs/mobile-api.md)), which calls the same server code
as the website's pages and Server Actions. Prices, GST, points, plan maturity, KYC rules and stock
are always computed on the server.

- Screens reload when the app returns to the foreground, and you can pull down to refresh.
- Bag and wishlist live on the server: a piece added on the web is in the app's bag, and the reverse.
- Guests can browse and fill a bag. Signing in merges that guest bag into the account, the same way the website does.

## Features (same as the customer portal)

| Area | Screens |
|---|---|
| Sign-in | OTP to mobile or email, new-customer sign-up, sign out, sign out of all devices |
| Shop | Home (hero, the owner's chosen rate strip, categories, bestsellers, For her / For him edits, new arrivals, plan promo), shop with search, metal, category and price-band filters and sorting (only the metals the store sells), product detail with price breakup, showroom stock and Ask on WhatsApp, wishlist |
| Men / Women views | Men \| Women switch on Home and Shop: shows that gender's pieces plus unisex ones and re-themes the whole app (Men: navy and steel, sans-serif headings, squarer shapes; Women: rose and wine, italic serif headlines, pill buttons). Tap the chosen view again (or double-tap) for every item and the normal look. Remembered across launches |
| Checkout | Bag with quantity changes, special offers (redeem a listed offer or enter an offer code, savings shown per piece), redeem points and plan balance, PAN/KYC and stock checks, payment method, Razorpay payment |
| Wallet | Total value, gold/silver/points/plan/cash balances, buy digital gold or silver, convert points to gold, sell gold, recent activity, complete history with filters |
| Trading | Buy digital gold/silver at the dealt rate during trading hours, estimate at today's rate, your requests with status, cancel a pending request for a refund (shown only when the store has turned Trading on) |
| Plans | Your plans with instalment tracker, pay the next instalment, join a plan |
| Orders | Order list with tracking stages, order detail with redeemed offer, courier/AWB tracking and events, BIS HUIDs, status history, invoice PDF download |
| Invoices | Invoices for paid orders (PDF) |
| Payment methods | UPI Autopay mandates, saved cards, withdrawal bank account |
| Profile & KYC | Personal details, email verification, KYC with PAN and address-proof upload |
| More | Showroom locator (directions, call, set as my store), metal calculator, offers, journal, help & FAQ |

## Build and run

Requirements: Android Studio (Ladybug or newer) or JDK 17 + the Android SDK (API 35).

1. Start the website locally from the repository root: `npm run db:setup && npm run dev`
   (development mode accepts OTP `123456` and uses mock payments; see `DEMO-CREDENTIALS.md`).
2. Open the `android/` folder in Android Studio and run the `app` configuration on an emulator.
   Debug builds talk to `http://10.0.2.2:3000/`, which is your computer as seen from the emulator.
3. To use a real phone with the local server, run `adb reverse tcp:3000 tcp:3000` and build with
   `-PsuvrnikaApiUrl=http://localhost:3000/`.

From the command line:

```bash
cd android
./gradlew assembleDebug                                                  # app/build/outputs/apk/debug/
./gradlew assembleRelease -PsuvrnikaApiUrl=https://shop.example.com/     # production server (https only)
```

Release builds allow HTTPS only. Cleartext HTTP is allowed only in debug builds, and only to
`10.0.2.2`, `localhost` and `127.0.0.1`. Sign the release APK/AAB with your own keystore. Keystores
are git-ignored. The GitHub Actions workflow `.github/workflows/android.yml` builds the debug APK on every change.

## Releases, app icon and updates

`.github/workflows/release.yml` builds a signed APK and publishes it as a GitHub release of this (public)
repository. It runs when app code is pushed to `main`, when it is started from the Actions tab, and when the
store's logo or app icon changes in Admin → Company setup (the server sends `app-icon-changed` when
`ANDROID_BUILD_TOKEN` is set). Before building it runs `scripts/brand_icon.py`, so the launcher icon is always the
store's current app icon (or logo).

The app checks the latest release on launch and when it returns to the foreground (at most every 6 hours) and
shows an "Update available" pop-up; Account → App settings & updates has a **Check for updates** button. The APK's
SHA-256 from the release is verified before Android's installer opens. Debug builds don't auto-check, because a
release signed with the release key can't be installed over them.

One-time setup: create a release keystore and keep it safe, since every update must be signed with it.

```bash
keytool -genkeypair -v -keystore suvrnika-release.jks -alias suvrnika -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 suvrnika-release.jks     # → secret SUVRNIKA_KEYSTORE_BASE64
```

Then add the secrets `SUVRNIKA_KEYSTORE_BASE64`, `SUVRNIKA_KEYSTORE_PASSWORD`, `SUVRNIKA_KEY_ALIAS` and
`SUVRNIKA_KEY_PASSWORD` (repository Settings → Secrets and variables → Actions). Optional variables:
`SUVRNIKA_API_URL` and `SUVRNIKA_ICON_BACKGROUND`.

## Payments

When Razorpay is configured on the server, checkout, digital-metal purchases and plan instalments
open the Razorpay Android SDK. The app sends the SDK's result to the server, which verifies the
signature. The Razorpay webhook stays the authority, so a payment still completes if the app is
killed half-way. The app remembers a payment that was in progress and shows how it ended when you
come back. In development (`PAYMENTS_MOCK=true`) a "Development payment" dialog replaces Razorpay.

## Security

- Sign-in issues per-device tokens: a 15-minute access token and a refresh token that rotates on
  every use. Replaying an old refresh token revokes that device.
- Tokens are stored with `EncryptedSharedPreferences` (Android Keystore) and excluded from backups.
- No server secrets (database, JWT, Razorpay secret, SMS/email keys) are in the app. It only
  receives the public Razorpay key id for each payment.
- KYC documents are uploaded to the server's private storage. Only masked numbers are kept on the account.

## Code layout

```
app/src/main/java/com/suvrnika/app/
  data/         API DTOs (Models.kt), Retrofit API, token storage, OkHttp auth + refresh, Repository
  payment/      PaymentCoordinator: Razorpay SDK, dev mock, reconciliation
  ui/           AppNav (shell, bottom bar, routes), theme (website palettes + Men/Women views), shared components
  ui/screens/   Store, Login, Account (wallet/history/plans/orders/invoices), Profile/KYC/payment methods, Info screens
  util/         Formatting identical to the website (₹ with Indian grouping, grams, IST dates)
```
