🚴‍♂️ Cycle Track: Android GPS Cycling Tracker
Welcome to Cycle Track, a modern Android application built with the latest technologies to track your cycling adventures. Whether you're a casual rider or a seasoned cyclist, Cycle Track provides the tools you need to monitor your performance, review your history, and visualize your progress in a clean, intuitive interface.
<br>
✨ Features
🗺️ Live GPS Tracking: See your route drawn on the map in real-time as you ride.
📊 Real-Time Statistics: Instantly monitor your Duration, Distance, and Average Speed.
☄️ Live Trail Effect: A "comet trail" follows your location, showing your most recent path for a cool visual effect.
💾 Persistent Ride History: All completed rides are automatically saved to a local database on your device.
📈 Detailed Ride Summary: Dive deep into any past ride with a full-screen map view and a complete breakdown of your stats.
▶️ Ride Replay Animation: Watch an animated replay of any past ride! A marker will trace the path from start to finish, bringing your journey back to life.
🚀 Getting Started
To get a local copy up and running, follow these simple steps.
Prerequisites
Android Studio (latest version recommended)
An Android device or emulator
Installation & Setup
Clone the repository:
git clone https://github.com/koustav24/Cycle-Track.git


Open in Android Studio:
Open Android Studio and select "Open" or "Open an Existing Project".
Navigate to the cloned Cycle-Track directory and open it.
Secure Your Google Maps API Key (Crucial Step):
This project uses the Google Maps SDK and requires an API key. For security, the key is not stored in the repository. You must add it locally by following these three steps.
Step A: Get an API Key
Follow the official Google guide to get a Google Maps API key.
Step B: Add Key to local.properties
In the root directory of the project, create a file named local.properties if it doesn't exist. Open the file and add the following line, replacing YOUR_API_KEY with the key you just generated:
MAPS_API_KEY=YOUR_API_KEY


Step C: Configure Gradle to Read the Key
Open the app-level build.gradle.kts file (app/build.gradle.kts). Add the following code inside the android { ... } block to read the key and make it available to the app.
defaultConfig {
    //... your existing config

    val localProperties = java.util.Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(java.io.FileInputStream(localPropertiesFile))
    }
    buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY")}\"")
}


Step D: Update the Manifest
Open your AndroidManifest.xml file. Ensure the <meta-data> tag for the API key references the variable, not a hard-coded key. It should look like this:
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />


Sync and Run:
Click "Sync Now" in the bar that appears in Android Studio.
Click the "Run 'app'" button (▶) to build and install the app on your selected device or emulator.
🏗️ Project Structure
The project is organized into several key packages to maintain a clean architecture:
com.example.cyclingtracker: The root package.
MainActivity.kt: The main entry point of the app, which also handles the navigation logic between screens.
/data: Contains all data-related classes.
Ride.kt: Defines the data structure (entity) for a ride.
RideDao.kt: The Data Access Object (DAO) that defines database interactions.
AppDatabase.kt: The Room Database setup, which ties the entity and DAO together.
/ui: Contains all UI-related components.
/screen: Holds the composable functions for each individual screen (TrackingScreen, HistoryScreen, RideDetailScreen).
/theme: Contains the app's color scheme and typography.
🛠️ Technology Stack
Language: Kotlin
UI Toolkit: Jetpack Compose
Architecture: MVVM (Model-View-ViewModel) principles
Asynchronous Operations: Kotlin Coroutines
Local Database: Room
Location Services: Google Play Services Location APIs
Maps: Google Maps SDK for Android
