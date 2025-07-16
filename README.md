# 🚴‍♂️ Cycle Track

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google%20Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-round)](http://makeapullrequest.com)

**A modern Kotlin-based Android cycling tracker app with live GPS tracking, real-time statistics, animated trails, and comprehensive ride history**

[Features](#-features) • [Screenshots](#-screenshots) • [Setup](#-getting-started) • [Tech Stack](#-tech-stack) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [✨ Features](#-features)
- [📱 Screenshots](#-screenshots)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)
  - [Google Maps API Configuration](#-google-maps-api-configuration)
- [🏗️ Project Structure](#-project-structure)
- [🛠️ Tech Stack](#-tech-stack)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [👨‍💻 Author](#-author)

---

## ✨ Features

<div align="center">

| Feature | Description |
|---------|-------------|
| 🗺️ **Live GPS Tracking** | Real-time route visualization on interactive maps |
| 📊 **Real-Time Statistics** | Live monitoring of duration, distance, and average speed |
| ☄️ **Animated Trails** | Beautiful comet trail effects following your location |
| 💾 **Ride History** | Persistent storage of all completed rides locally |
| 📈 **Detailed Analytics** | In-depth ride summaries with comprehensive statistics |
| ▶️ **Ride Replay** | Animated playback of past rides with path tracing |
| 🎨 **Modern UI** | Clean, intuitive interface built with Jetpack Compose |
| 🔒 **Offline Storage** | Local database ensures your data stays private |

</div>

### 🌟 Key Highlights

- **🚴‍♂️ Perfect for All Cyclists**: Whether you're a weekend warrior or daily commuter
- **📱 Native Android Experience**: Built specifically for Android with modern best practices
- **🔋 Battery Optimized**: Efficient GPS usage to preserve battery life
- **🎯 Performance Focused**: Smooth animations and responsive UI

---

## 📱 Screenshots

> 🖼️ *Screenshots and demo GIFs will be added here to showcase the app's interface and features*

<div align="center">

| Main Tracking Screen | Ride History | Statistics View |
|:-------------------:|:------------:|:---------------:|
| *Coming Soon* | *Coming Soon* | *Coming Soon* |

</div>

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio** (Arctic Fox or later) 🛠️
- **JDK 11** or higher ☕
- **Android SDK** with minimum API level 24 📱
- **Git** for version control 🔧

### Installation & Setup

1. **📥 Clone the repository**
   ```bash
   git clone https://github.com/koustav24/Cycle-Track.git
   cd Cycle-Track
   ```

2. **🔧 Open in Android Studio**
   - Launch Android Studio
   - Select `File` → `Open`
   - Navigate to the cloned `Cycle-Track` directory
   - Click `OK` to open the project

3. **🔄 Sync Project**
   - Wait for Gradle sync to complete
   - Resolve any dependency issues if prompted

### 🗺️ Google Maps API Configuration

> ⚠️ **Important**: This app requires a Google Maps API key to function properly.

#### Step 1: Obtain API Key
1. Visit the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Maps SDK for Android** API
4. Generate an API key for your project

#### Step 2: Configure Local Properties
Create a `local.properties` file in the project root and add your API key:

```properties
# Google Maps API Key
MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
```

#### Step 3: Verify Gradle Configuration
Ensure your `app/build.gradle.kts` includes:

```kotlin
android {
    defaultConfig {
        // ... other configurations
        
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY")}\"")
    }
}
```

#### Step 4: Verify Manifest Configuration
Check that `AndroidManifest.xml` contains:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

4. **▶️ Build and Run**
   - Click the **Sync Now** button when prompted
   - Select your target device or emulator
   - Click the **Run** button (▶️) to build and install

---

## 🏗️ Project Structure

```
📦 com.example.cyclingtracker
├── 📁 data/                    # Data layer components
│   ├── 📄 Ride.kt             # Ride entity model
│   ├── 📄 RideDao.kt          # Database access object
│   └── 📄 AppDatabase.kt      # Room database configuration
├── 📁 ui/                     # User interface components
│   ├── 📁 screen/             # Individual screen composables
│   │   ├── 📄 TrackingScreen.kt
│   │   ├── 📄 HistoryScreen.kt
│   │   └── 📄 RideDetailScreen.kt
│   └── 📁 theme/              # App theming
│       ├── 📄 Color.kt
│       ├── 📄 Theme.kt
│       └── 📄 Type.kt
└── 📄 MainActivity.kt         # Main application entry point
```

### 🎯 Architecture Principles

- **📐 MVVM Pattern**: Clear separation of concerns
- **🧩 Modular Design**: Easy to maintain and extend
- **🔄 Reactive Programming**: Kotlin Coroutines for async operations
- **💾 Local-First**: Room database for offline functionality

---

## 🛠️ Tech Stack

<div align="center">

### Core Technologies

| Technology | Purpose | Version |
|------------|---------|---------|
| ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white) | Primary Language | 1.9.22 |
| ![Jetpack Compose](https://img.shields.io/badge/Compose-4285F4?style=flat&logo=android&logoColor=white) | Modern UI Toolkit | Latest |
| ![Room](https://img.shields.io/badge/Room-4285F4?style=flat&logo=android&logoColor=white) | Local Database | 2.6.1 |
| ![Google Maps](https://img.shields.io/badge/Maps-4285F4?style=flat&logo=googlemaps&logoColor=white) | Mapping & Location | 18.2.0 |

</div>

### 📚 Dependencies

- **🎨 UI Framework**: Jetpack Compose with Material 3
- **🗺️ Maps & Location**: Google Play Services Location & Maps
- **💾 Database**: Room with KSP annotation processing
- **🔄 Async Operations**: Kotlin Coroutines & Flow
- **🧵 Serialization**: Gson for data persistence
- **🏗️ Architecture**: Android Architecture Components

### 🎯 Development Principles

- **Material Design 3** for consistent UI/UX
- **Single Activity Architecture** with Compose Navigation
- **Repository Pattern** for data management
- **State Management** with Compose State

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### 🌟 How to Contribute

1. **🍴 Fork the repository**
2. **🌿 Create a feature branch**
   ```bash
   git checkout -b feature/amazing-new-feature
   ```
3. **💻 Make your changes**
4. **✅ Add tests** if applicable
5. **📝 Commit your changes**
   ```bash
   git commit -m "Add amazing new feature"
   ```
6. **📤 Push to your branch**
   ```bash
   git push origin feature/amazing-new-feature
   ```
7. **🎯 Open a Pull Request**

### 🐛 Bug Reports

Found a bug? Please open an issue with:
- **📝 Clear description** of the problem
- **🔄 Steps to reproduce** the issue
- **📱 Device information** and Android version
- **📋 Relevant logs** or error messages

### 💡 Feature Requests

Have an idea? We'd love to hear it! Please include:
- **🎯 Clear description** of the proposed feature
- **💭 Use case** or problem it solves
- **🎨 Any mockups** or examples if applicable

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024 Koustav

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software")...
```

---

## 👨‍💻 Author

<div align="center">

**Koustav**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/koustav24)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/koustav24)

*Building the future of cycling technology, one commit at a time* 🚴‍♂️

</div>

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ and lots of ☕ by developers who love cycling

[Back to Top](#-cycle-track) ⬆️

</div>
