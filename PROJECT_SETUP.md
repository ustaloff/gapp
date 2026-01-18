# Project Setup & Keys Guide

This document lists the critical configurations required for AdShield to function.
**Save this file.** If you move the project or wipe your settings, you will need to re-enable them.

## 1. Google Services Config (`google-services.json`)
**Status:** ✅ Configured
-   **Location:** `app/google-services.json`
-   **Purpose:** This file contains all the "secrets" and connection details for Firebase and Google Sign-In. It tells the app which database to use and what your API keys are.
-   **If Lost:** Go to [Firebase Console](https://console.firebase.google.com/) -> Project Settings -> Your Apps -> Download `google-services.json`.
-   **Important:** NEVER share this file publicly (e.g., on GitHub) if your database rules are not secure.

## 2. Firebase (Authentication & Database)
**Status:** ✅ Configured
-   **Service:** Firebase Authentication (Google Sign-In) & Cloud Firestore.
-   **Crucial Step:** You MUST check that **Cloud Firestore API** is **ENABLED** in the Google Cloud Console for the project. If the app logs "Firestore disabled", this is the fix.

## 3. Google Play Services (Sign-In)
**Status:** ✅ Configured
-   **Client ID:** The app uses the `default_web_client_id`. This ID is **automatically generated** from the `google-services.json` file during the build process.
-   **SHA-1 Fingerprint:** Your debug keystore SHA-1 is registered in Firebase.
    -   *If you change computers:* Run `gradlew signingReport` to get your new SHA-1, then add it to Firebase Console -> Project Settings.

## 4. Premium / Billing
**Status:** ✅ Local Simulation
-   **Implementation:** Premium статус симулируется локально в `BillingManager.kt`.
-   **No External SDK:** RevenueCat был удалён для упрощения разработки.
-   **Future:** Для продакшена можно интегрировать Google Play Billing Library или RevenueCat.

## 5. Keystore (Signing)
**Status:** ✅ Debug Mode
-   App works in debug mode.
-   For release, use `release-keystore.jks` and set environment variables `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
## 6. Publishing (Play Store)
**Status:** ℹ️ Information
> [!IMPORTANT]
> **VpnService Declaration Requirement**
> 
> When uploading this application to the Google Play Store, you will be required to declare the use of `VpnService` permission.
> 
> 1.  Navigate to **App Content** -> **VPN Service declaration form**.
> 2.  You must explicitly state that the app uses `VpnService` to implement its core functionality (DNS Filtering/Blocking).
> 3.  You must confirm that `LocalVpnService` is the class extending `VpnService`.
> 4.  Failure to declare this properly will result in app rejection. The code has been annotated with `@SuppressLint("VpnServicePolicy")` to suppress the IDE warning, but the Console form is mandatory.
