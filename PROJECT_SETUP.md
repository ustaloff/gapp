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

## 4. RevenueCat (Subscriptions)
**Status:** ⚠️ Partially Configured (App-side only)
-   **API Key:** Found in `MainActivity.kt`.
-   **Current State:** Subscriptions fail to load.
    -   Error 1: "API Key not recognized" (Check `MainActivity.kt` vs Dashboard).
    -   Error 2: Products (Monthly/Yearly) do not exist in Play Console.
-   **Fix:** 
    1. Update API Key in `MainActivity.kt`.
    2. Create products in Play Console.

## 5. Keystore (Signing)
**Status:** ✅ Debug Mode
-   App works in debug mode.
-   For release, use `release-keystore.jks` and set environment variables `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
