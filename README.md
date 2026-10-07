# CHOR — চোর

**Developer:** JR Jubayer Hasan  
**Package / Namespace:** `com.example` (`applicationId`: `com.aistudio.chorgame.vkrpxz`)

A cartoon village stealth-arcade & multiplayer Android game built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Authentication (Google Sign-In via Credential Manager)**, and **Cloud Firestore**.

---

## Project Structure

```text
CHOR/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── model/GameModels.kt
│       │   │   │   └── repository/
│       │   │   │       ├── ChorRepository.kt
│       │   │   │       └── DataManager.kt
│       │   │   ├── game/VillageWorldGenerator.kt
│       │   │   ├── sound/SoundManager.kt
│       │   │   ├── ui/
│       │   │   │   ├── components/ShareInviteSheet.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── AdminPanelScreen.kt
│       │   │   │   │   ├── GameplayScreen.kt
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   ├── HowToPlayDialog.kt
│       │   │   │   │   ├── LoginScreen.kt
│       │   │   │   │   ├── MultiplayerHubScreen.kt
│       │   │   │   │   ├── ProfileScreen.kt
│       │   │   │   │   ├── RankingScreen.kt
│       │   │   │   │   └── ResultOverlay.kt
│       │   │   │   └── theme/
│       │   │   └── viewmodel/ChorViewModel.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── font/
│       │       ├── mipmap-anydpi-v26/
│       │       ├── mipmap-hdpi/
│       │       ├── mipmap-mdpi/
│       │       ├── mipmap-xhdpi/
│       │       ├── mipmap-xxhdpi/
│       │       ├── mipmap-xxxhdpi/
│       │       ├── values/
│       │       └── xml/
│       └── test/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── firestore.rules
└── .gitignore
```

---

## Building the APK Locally (Laptop / Android Studio)

1. **Generate `debug.keystore` (First time only after unzipping):**
   - **macOS / Linux:**
     ```bash
     base64 -d debug.keystore.base64 > debug.keystore
     ```
   - **Windows (PowerShell / CMD):**
     ```cmd
     certutil -decode debug.keystore.base64 debug.keystore
     ```

2. **Build the Debug APK:**
   - **macOS / Linux:**
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
   - **Windows:**
     ```cmd
     gradlew.bat assembleDebug
     ```

3. **Locate the generated APK:**
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
