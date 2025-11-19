# Guide: Implementing Feature 4 (Timed Point-to-Point Rides)

Implementing a timed point-to-point challenge is a significant feature that builds on your existing app but requires several new components, including new Google APIs and a more complex UI state.

Here is a high-level guide on how to approach this.

## 1. Enable Required APIs & Add Dependencies

You will need two new Google APIs. You must enable them in the same Google Cloud Console project where you got your Maps API key.

1.  **Google Directions API:** Gets a route polyline, distance, and ETA between two points.
2.  **Google Places API:** Lets you add a destination search bar (autocomplete).

### Add Dependencies to `app/build.gradle.kts`

```kotlin
// For Google Directions API (a robust Java/Kotlin client)
implementation("com.google.maps:google-maps-services:2.2.0")

// For Google Places API (native Android SDK for autocomplete)
implementation("com.google.android.libraries.places:places:3.5.0")
```


2. Secure Your API Key for Directions API

The google-maps-services library is not part of the Android SDK and runs server-side (or in this case, client-side). It's highly recommended to run this from a backend server to protect your API key.

However, for a simple implementation, you can pass your API key directly.

3. Modify the UI (TrackingScreen.kt)

You'll need a new "challenge mode" UI.

Add a "Challenge" Button: Add a new button next to "Start" and "Stop" to initiate the challenge setup.

Create a Challenge Setup Modal/Dialog:

This dialog should pop up when "Challenge" is clicked.

Add a TextField for the time limit (e.g., "30" minutes).

Add a TextField for the destination. This is where you would integrate the Places Autocomplete widget.

An "Start Challenge" button.

Update Map UI State:

Create new state variables in TrackingScreen:

```
var isChallengeMode by remember { mutableStateOf(false) }
var targetRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
var destination by remember { mutableStateOf<LatLng?>(null) }
var timeLimitInSeconds by remember { mutableLongStateOf(0L) }
var timeRemaining by remember { mutableLongStateOf(0L) }
```


When "Start Challenge" is clicked, set these states.

In the GoogleMap composable, add a second Polyline for the targetRoute (e.g., in grey) and a Marker for the destination.

4. Implement Core Logic

a. Fetching the Route (Directions API)

When the user clicks "Start Challenge":

Get the startLocation (current location) and endLocation (from Places Autocomplete).

Create a GeoApiContext for the Directions API.

```
val geoContext = GeoApiContext.Builder()
    .apiKey(BuildConfig.MAPS_API_KEY) // Use your Maps key (must be enabled for Directions)
    .build()
```


Call the API (this is a network call, so run it in a scope.launch):

```
val result = DirectionsApi.newRequest(geoContext)
    .origin(com.google.maps.model.LatLng(startLocation.latitude, startLocation.longitude))
    .destination(com.google.maps.model.LatLng(endLocation.latitude, endLocation.longitude))
    .mode(TravelMode.BICYCLING)
    .await() // This is a suspending function

// Get the polyline
val path = result.routes[0].overviewPolyline.decodePath() // This is a List<com.google.maps.model.LatLng>

// Convert to Google Maps LatLng
targetRoute = path.map { LatLng(it.lat, it.lng) }
destination = endLocation

// Get ETA and set alerts
val etaInSeconds = result.routes[0].legs[0].duration.inSeconds

// Set timers
timeLimitInSeconds = /* user input in seconds */
timeRemaining = timeLimitInSeconds
isChallengeMode = true
isTracking = true // Start tracking
```


b. Monitoring Progress

Countdown Timer: Create a new LaunchedEffect(isChallengeMode, isTracking) that delays 1000ms and decrements timeRemaining as long as isTracking is true.

If timeRemaining hits 0, stop the ride and show a "Time's Up!" message.

Pace Alert: In your StatsCard, show "Time Remaining". You can also add a Text that compares your progress:

```
val distanceToDestination = ... (calculate in LocationCallback)

val timeTaken = timeLimitInSeconds - timeRemaining
```

Compare timeTaken vs etaInSeconds for the remaining distance to show "On Pace" or "Falling Behind".

Ride Completion: In your LocationCallback:

Calculate the distance from your newLatLng to the destination.

If distance < 30 (meters), you've arrived!

Stop the ride (isTracking = false, isChallengeMode = false).

Show a "Challenge Complete!" message.

Save the ride (you could add a new field to your Ride entity like isChallenge: Boolean).
