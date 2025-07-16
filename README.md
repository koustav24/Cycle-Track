
---

# 🚴‍♂️ Cycle Track

[![Android](https://img.shields.io/badge/platform-Android-green?style=flat-square&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-blue?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/github/license/koustav24/Cycle-Track?style=flat-square)](LICENSE)

> **Cycle Track** is a modern, feature-rich Android app for tracking your cycling adventures, built with the latest Android technologies and Jetpack Compose UI.

---

## ✨ Features

- 🗺️ **Live GPS Tracking**: See your route drawn on the map in real-time as you ride.
- 📊 **Real-Time Statistics**: Instantly monitor your ride’s duration, distance, and average speed.
- ☄️ **Live Trail Effect**: A cool “comet trail” follows your location, showing your recent path.
- 💾 **Persistent Ride History**: All completed rides are automatically saved locally.
- 📈 **Detailed Ride Summaries**: Review any past ride with a full map view and stat breakdown.
- ▶️ **Ride Replay Animation**: Watch an animated replay of any ride—relive your journey!

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest recommended)
- Android device or emulator

### Installation

1. **Clone the repository:**
    ```bash
    git clone https://github.com/koustav24/Cycle-Track.git
    ```

2. **Open in Android Studio:**
   - Open Android Studio and select **Open an Existing Project**.
   - Navigate to the cloned `Cycle-Track` directory.

3. **Google Maps API Key (Required):**
   - Get an API key by following the [official Google instructions](https://developers.google.com/maps/documentation/android-sdk/get-api-key).
   - In your project’s root, create or edit `local.properties`:
     ```
     MAPS_API_KEY=YOUR_API_KEY
     ```
   - In `app/build.gradle.kts`, add inside `defaultConfig`:
     ```kotlin
     val localProperties = java.util.Properties()
     val localPropertiesFile = rootProject.file("local.properties")
     if (localPropertiesFile.exists()) {
         localProperties.load(java.io.FileInputStream(localPropertiesFile))
     }
     buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY")}\"")
     ```
   - In `AndroidManifest.xml`, ensure:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="${MAPS_API_KEY}" />
     ```

4. **Sync & Run:**
   - Click *Sync Now* in Android Studio.
   - Click the ▶️ *Run* button to build and install the app.

---

## 🏗️ Project Structure

```
com.example.cyclingtracker/
├── MainActivity.kt         # Main entry and navigation logic
├── data/
│   ├── Ride.kt            # Ride entity
│   ├── RideDao.kt         # Database access
│   └── AppDatabase.kt     # Room DB setup
├── ui/
│   ├── screen/            # Composable screen UIs
│   └── theme/             # Color scheme & typography
```

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM
- **Async:** Kotlin Coroutines
- **Database:** Room
- **Location:** Google Play Services Location API
- **Maps:** Google Maps SDK

---

## 🙌 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you’d like to change.

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---
