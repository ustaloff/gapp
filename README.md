# 🛡️ AdShield: Cyber-Secure DNS AdBlocker

AdShield is a high-performance, cyberpunk-inspired Android application designed to reclaim your digital privacy. By leveraging a local VPN service, AdShield intercepts DNS requests to block advertisements, trackers, and malicious domains at the system level without requiring root access.

---

## 🚀 Key Features (Capabilities)

- **⚙️ Advanced DNS Filtering**: Blocks ads across all apps (browsers, games, social media) using a high-precision Trie-based filtering engine.
- **📊 Real-Time Analytics**: Holographic graphs and live event logs showing exactly what domains are being intercepted.
- **🛡️ App Whitelisting**: Exclude specific applications from VPN protection to ensure compatibility with banking or local network apps.
- **🔄 Dynamic Blocklists**: Automatic daily updates of filter lists via a custom GitHub-hosted aggregator.
- **✨ Cyberpunk Aesthetics**: A premium, "neon-dark" UI with glitch animations and glowing components built with Jetpack Compose.
- **💎 Premium Subscription**: Integration with RevenueCat and Firebase for managing user profiles and pro-features.
- **☁️ Cloud Sync**: Firebase-backed user accounts with Google Sign-In support.

---

## 🛠️ Technical Details & Architecture

### Technology Stack
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3)
- **Networking**: Local VpnService (VpnService API)
- **Backend/Auth**: Firebase (Auth, Firestore)
- **Billing**: RevenueCat Purchases SDK
- **Data Persistence**: SharedPreferences & Kotlin StateFlow
- **Scripts**: Python (for Blocklist Aggregation)

### Core Components
- **`LocalVpnService`**: The engine that creates the TUN interface, parses IP/UDP/DNS packets, and decides whether to allow or block traffic.
- **`FilterEngine`**: A thread-safe, memory-optimized Trie structure that handles tens of thousands of domain rules with sub-millisecond lookup times.
- **`FilterRepository`**: Handles fetching and parsing of raw blocklists from remote URLs.
- **`VpnStats`**: A global state provider that collects metrics, counts blocked requests, and maintains the holographic graph data.

### Project Structure
```text
app/src/main/java/com/example/adshield/
├── data/           # Repositories, Preferences, and State (VpnStats)
├── service/        # VpnService implementation and packet parsing
├── filter/         # Core blocking logic (Trie Engine)
├── ui/             # Jetpack Compose Screens and components
│   ├── theme/      # Cyberpunk color palettes and typography
│   └── components/ # Reusable Neon and Glitch UI elements
scripts/            # Python-based blocklist aggregator & CI/CD
```

---

## 🏁 Getting Started (Launch Instructions)

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer.
- JDK 17.
- A physical Android device or Emulator (API 26+).

### Initial Setup
1. **Clone the project** to your local machine.
2. **Firebase Configuration**:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with package name `com.example.adshield`.
   - Add your SHA-1 fingerprint to the Firebase project.
   - Download `google-services.json` and place it in the `app/` directory.
3. **Google Sign-In**:
   - Enable "Google" as a Sign-in provider in Firebase Authentication.
   - Ensure the "Web Client ID" matches the one generated in your `google-services.json`.

### Running the App
1. Sync project with Gradle files.
2. Select the `app` configuration.
3. Run on your device/emulator (Debug mode recommended for initial testing).

### Blocklist Aggregator (Optional)
The blocklists are managed in a separate workflow. If you wish to update the list manually:
1. Navigate to `scripts/`.
2. Install dependencies: `pip install -r requirements.txt`.
3. Run the script: `python aggregator.py`.
4. The generated `blocklist.txt` is used as the primary source for the app.

---

## 📜 License
This project is private and intended for personal use and development.

---
*Built with ❤️ for Privacy and Style.*
