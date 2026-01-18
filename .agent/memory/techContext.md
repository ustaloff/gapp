# Tech Context

## Stack
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Repository pattern
- **Async**: Kotlin Coroutines + Flow
- **DI**: Manual (no Hilt/Dagger yet)
- **Backend**: Firebase (Auth + Firestore)
- **Build**: Gradle 8.x + AGP

## Environment Setup
```powershell
# Сборка проекта
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; ./gradlew assembleDebug

# Полная сборка с проверками
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; ./gradlew build
```

## Key Dependencies
- `androidx.compose.*` — UI
- `androidx.lifecycle.*` — ViewModel, Flow
- `com.google.firebase:firebase-auth` — Authentication
- `com.google.firebase:firebase-firestore` — Database
- Android VpnService API — Core functionality

## Minimum Requirements
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **JDK**: 17 (bundled with Android Studio)

## Testing
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Manual testing: Emulator or physical device with VPN permission
