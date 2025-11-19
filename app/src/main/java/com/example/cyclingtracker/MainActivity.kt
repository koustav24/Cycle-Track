package com.example.cyclingtracker

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cyclingtracker.data.AppDatabase
import com.example.cyclingtracker.data.Ride
import com.example.cyclingtracker.network.RetrofitInstance
import com.example.cyclingtracker.ui.screen.HistoryScreen
import com.example.cyclingtracker.ui.screen.RideDetailScreen
import com.example.cyclingtracker.ui.theme.CyclingTrackerTheme
import com.example.cyclingtracker.util.CalorieCalculator
import com.example.cyclingtracker.util.KalmanLatLong
import com.example.cyclingtracker.util.await
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import com.google.maps.model.TravelMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.maps.GeoApiContext
import com.google.maps.DirectionsApi
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        setContent {
            CyclingTrackerTheme {
                AppContent()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val rideDao = remember { db.rideDao() }

    var currentScreen by remember { mutableStateOf("tracking") }
    var selectedRideId by remember { mutableStateOf<Long?>(null) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    val routePath = remember { mutableStateListOf<LatLng>() }
    var totalDistance by remember { mutableFloatStateOf(0f) }
    var elapsedTimeInSeconds by remember { mutableLongStateOf(0L) }

    // States for new features
    var userWeight by remember { mutableFloatStateOf(70f) }
    var totalCalories by remember { mutableFloatStateOf(0f) }
    var aqi by remember { mutableStateOf<Int?>(null) }
    var aqiError by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var targetRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }

    // Challenge specific states
    var isChallengeMode by remember { mutableStateOf(false) }
    var challengeTimeLimitInSeconds by remember { mutableLongStateOf(0L) }
    var challengeTimeRemainingInSeconds by remember { mutableLongStateOf(0L) }
    var estimatedCalories by remember { mutableFloatStateOf(0f) }

    // Kalman filter for smoothing
    val kalmanFilter = remember { KalmanLatLong(3f) }

    // AQI Service
    val aqiService = remember { RetrofitInstance.api }
    val aqiApiKey = BuildConfig.AQI_API_KEY

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(28.7041, 77.1025), 15f) // Default to Delhi
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            hasLocationPermission = fineLocationGranted
            if (fineLocationGranted) {
                // Get initial location once permission is granted
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLocation!!, 15f)
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Timer coroutine for ride tracking
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (true) {
                delay(1000)
                elapsedTimeInSeconds++
            }
        }
    }

    // Countdown timer for challenge mode
    LaunchedEffect(isChallengeMode, isTracking) {
        if (isChallengeMode && isTracking) {
            challengeTimeRemainingInSeconds = challengeTimeLimitInSeconds
            while (challengeTimeRemainingInSeconds > 0) {
                delay(1000)
                challengeTimeRemainingInSeconds--
            }
            if (challengeTimeRemainingInSeconds == 0L) {
                Toast.makeText(context, "Time's up!", Toast.LENGTH_LONG).show()
                isTracking = false // Stop the ride
            }
        }
    }

    // Coroutine to fetch AQI when location is available
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && aqi == null && !aqiError) {
            scope.launch {
                try {
                    val response = aqiService.getAqi(currentLocation!!.latitude, currentLocation!!.longitude, aqiApiKey)
                    if (response.status == "ok" && response.data != null) {
                        aqi = response.data.aqi
                    } else {
                        Log.e("AQI_FETCH", "API response not 'ok' or data is null: ${response.status}")
                        aqiError = true // Prevent refetching on error
                    }
                } catch (e: Exception) {
                    Log.e("AQI_FETCH", "Failed to fetch AQI", e)
                    aqiError = true // Prevent refetching on error
                }
            }
        }
    }

    // Location request callback - now runs continuously if permission is granted
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                val smoothedLocation = kalmanFilter.process(location.latitude, location.longitude, location.accuracy, location.time)
                val newLatLng = LatLng(smoothedLocation.latitude, smoothedLocation.longitude)
                currentLocation = newLatLng // Always update the current location

                if (isTracking) {
                    // Only update route and stats if actively tracking
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(newLatLng, 17f)
                    if (routePath.isNotEmpty()) {
                        val lastLatLng = routePath.last()
                        val distanceResult = FloatArray(1)
                        Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, newLatLng.latitude, newLatLng.longitude, distanceResult)
                        totalDistance += distanceResult[0]
                    }
                    routePath.add(newLatLng)
                    val avgSpeedKmh = if (elapsedTimeInSeconds > 0) (totalDistance / elapsedTimeInSeconds) * 3.6f else 0f
                    val met = CalorieCalculator.getMetValue(avgSpeedKmh)
                    totalCalories = CalorieCalculator.calculateCalories(met, userWeight, elapsedTimeInSeconds)
                }
            }
        }
    }

    // Start/Stop location updates
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycling Tracker") },
                actions = {
                    IconButton(onClick = { showWeightDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { currentScreen = "history" }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            if (showWeightDialog) {
                WeightInputDialog(
                    currentWeight = userWeight,
                    onDismiss = { showWeightDialog = false },
                    onWeightSubmit = { newWeight ->
                        userWeight = newWeight
                        showWeightDialog = false
                    }
                )
            }
            if (showChallengeDialog) {
                ChallengeDialog(
                    onDismiss = { showChallengeDialog = false },
                    onStartChallenge = { dest, timeInMinutes ->
                        if (currentLocation == null) {
                            Toast.makeText(context, "Cannot get current location to start challenge.", Toast.LENGTH_LONG).show()
                            return@ChallengeDialog
                        }

                        scope.launch {
                            val placesClient = Places.createClient(context)
                            val request = FindAutocompletePredictionsRequest.builder().setQuery(dest).build()

                            try {
                                val predictionResponse = placesClient.findAutocompletePredictions(request).await()
                                if (predictionResponse.autocompletePredictions.isNotEmpty()) {
                                    val placeId = predictionResponse.autocompletePredictions[0].placeId
                                    val placeFields = listOf(Place.Field.LAT_LNG)
                                    val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
                                    val placeResponse = placesClient.fetchPlace(fetchPlaceRequest).await()
                                    val place = placeResponse.place
                                    destination = place.latLng

                                    val geoContext = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_API_KEY).build()
                                    val directionsResult = DirectionsApi.newRequest(geoContext)
                                        .origin(com.google.maps.model.LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                                        .destination(com.google.maps.model.LatLng(destination!!.latitude, destination!!.longitude))
                                        .mode(TravelMode.DRIVING) // Using driving route for India
                                        .await()

                                    val path = directionsResult.routes[0].overviewPolyline.decodePath()
                                    targetRoute = path.map { LatLng(it.lat, it.lng) }

                                    // Set up challenge state
                                    val estimatedDurationSeconds = directionsResult.routes[0].legs[0].duration.inSeconds
                                    val estimatedDistanceMeters = directionsResult.routes[0].legs[0].distance.inMeters
                                    val estimatedAvgSpeedKmh = (estimatedDistanceMeters / estimatedDurationSeconds) * 3.6f
                                    val met = CalorieCalculator.getMetValue(estimatedAvgSpeedKmh)
                                    estimatedCalories = CalorieCalculator.calculateCalories(met, userWeight, estimatedDurationSeconds)
                                    challengeTimeLimitInSeconds = (timeInMinutes.toLongOrNull() ?: 30) * 60
                                    isChallengeMode = true
                                    isTracking = true // Start tracking for the challenge
                                    showChallengeDialog = false

                                } else {
                                    Toast.makeText(context, "Destination not found.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("ChallengeError", "Failed to start challenge", e)
                                Toast.makeText(context, "Error starting challenge: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }

            if (currentScreen == "tracking") {
                TrackingScreen(
                    hasLocationPermission = hasLocationPermission,
                    isTracking = isTracking,
                    isChallengeMode = isChallengeMode,
                    routePath = routePath.toList(),
                    targetRoute = targetRoute,
                    destination = destination,
                    cameraPositionState = cameraPositionState,
                    totalDistance = totalDistance,
                    elapsedTimeInSeconds = elapsedTimeInSeconds,
                    challengeTimeRemainingInSeconds = challengeTimeRemainingInSeconds,
                    estimatedCalories = estimatedCalories,
                    totalCalories = totalCalories,
                    aqi = aqi,
                    onStartClick = {
                        if (isChallengeMode) return@TrackingScreen // Don't allow regular start in challenge mode
                        isTracking = !isTracking
                    },
                    onStopClick = {
                        if (routePath.size > 1 && !isChallengeMode) { // Only save if not in challenge mode
                            scope.launch {
                                val ride = Ride(
                                    durationInMillis = elapsedTimeInSeconds * 1000,
                                    distanceInMeters = totalDistance,
                                    path = routePath.toList(),
                                    caloriesBurned = totalCalories
                                )
                                rideDao.insertRide(ride)
                                Toast.makeText(context, "Ride Saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        // Reset all states
                        isTracking = false
                        isChallengeMode = false
                        routePath.clear()
                        totalDistance = 0f
                        elapsedTimeInSeconds = 0L
                        totalCalories = 0f
                        aqi = null
                        aqiError = false
                        targetRoute = emptyList()
                        destination = null
                        challengeTimeRemainingInSeconds = 0L
                        estimatedCalories = 0f
                    },
                    onChallengeClick = { showChallengeDialog = true }
                )
            } else if (currentScreen == "history") {
                HistoryScreen(onNavigateBack = { currentScreen = "tracking" }, onRideClick = {
                    selectedRideId = it
                    currentScreen = "ride_detail"
                })
            } else if (currentScreen == "ride_detail") {
                RideDetailScreen(rideId = selectedRideId!!, rideDao = rideDao, onBack = {
                    currentScreen = "history"
                })
            }
        }
    }
}

@Composable
fun ChallengeDialog(
    onDismiss: () -> Unit,
    onStartChallenge: (String, String) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("30") }
    var predictions by remember { mutableStateOf(emptyList<Pair<String, String>>()) } // Pair of (Place ID, Description)
    val context = LocalContext.current

    LaunchedEffect(destination) {
        if (destination.length > 2) { // Only search when query is long enough
            val placesClient = Places.createClient(context)
            val request = FindAutocompletePredictionsRequest.builder().setQuery(destination).build()
            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                predictions = response.autocompletePredictions.map { it.placeId to it.getFullText(null).toString() }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Start a Challenge", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    singleLine = true
                )
                if (predictions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp).fillMaxWidth()) {
                        items(predictions) { (placeId, description) ->
                            Text(
                                text = description,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        destination = description
                                        predictions = emptyList()
                                     }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onStartChallenge(destination, time) }, 
                        modifier = Modifier.weight(1f),
                        enabled = destination.isNotBlank()
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}

@Composable
fun WeightInputDialog(
    currentWeight: Float,
    onDismiss: () -> Unit,
    onWeightSubmit: (Float) -> Unit
) {
    var weightInput by remember { mutableStateOf(currentWeight.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enter Your Weight", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { 
                        val newWeight = weightInput.toFloatOrNull() ?: currentWeight
                        onWeightSubmit(newWeight)
                     }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingScreen(
    hasLocationPermission: Boolean,
    isTracking: Boolean,
    isChallengeMode: Boolean,
    routePath: List<LatLng>,
    targetRoute: List<LatLng>,
    destination: LatLng?,
    cameraPositionState: CameraPositionState,
    totalDistance: Float,
    elapsedTimeInSeconds: Long,
    challengeTimeRemainingInSeconds: Long,
    estimatedCalories: Float,
    totalCalories: Float,
    aqi: Int?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onChallengeClick: () -> Unit
) {
    if (!hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Location permission not granted. Please enable it in settings.")
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            if (routePath.size > 1) {
                Polyline(
                    points = routePath,
                    color = Color.Blue.copy(alpha = 0.7f),
                    width = 20f
                )
            }
            if (targetRoute.isNotEmpty()) {
                Polyline(
                    points = targetRoute,
                    color = Color.Gray,
                    width = 15f
                )
            }
            destination?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Destination"
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isChallengeMode) {
                ChallengeStatusCard(
                    timeRemaining = challengeTimeRemainingInSeconds,
                    estimatedCalories = estimatedCalories
                )
            } else {
                 StatsCard(
                    distance = totalDistance,
                    elapsedTime = elapsedTimeInSeconds,
                    avgSpeed = (if (elapsedTimeInSeconds > 0) (totalDistance / elapsedTimeInSeconds) * 3.6f else 0f),
                    calories = totalCalories,
                    aqi = aqi
                )
            }

            ControlButtons(
                isTracking = isTracking,
                isChallengeMode = isChallengeMode,
                elapsedTime = elapsedTimeInSeconds,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onChallengeClick = onChallengeClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeStatusCard(timeRemaining: Long, estimatedCalories: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "TIME LEFT", value = com.example.cyclingtracker.ui.screen.formatDuration(timeRemaining * 1000))
            StatItem(label = "EST. CALORIES", value = "%.0f".format(estimatedCalories))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsCard(
    distance: Float,
    elapsedTime: Long,
    avgSpeed: Float,
    calories: Float,
    aqi: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "DURATION", value = com.example.cyclingtracker.ui.screen.formatDuration(elapsedTime * 1000))
                StatItem(label = "DISTANCE", value = "%.2f KM".format(distance / 1000))
                StatItem(label = "AVG. SPEED", value = "%.1f KM/H".format(avgSpeed))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "CALORIES", value = "%.0f".format(calories))
                StatItem(label = "AQI", value = aqi?.toString() ?: "...")
            }
        }
    }
}

@Composable
fun ControlButtons(
    isTracking: Boolean,
    isChallengeMode: Boolean,
    elapsedTime: Long,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onChallengeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onStartClick,
            enabled = !isChallengeMode // Disable start/resume in challenge mode
        ) {
            Text(if (isTracking) "PAUSE" else if (elapsedTime > 0) "RESUME" else "START")
        }
        Button(
            onClick = onChallengeClick,
            enabled = !isTracking // Disable during a ride
        ) {
            Text("CHALLENGE")
        }
        Button(
            onClick = onStopClick,
            enabled = isTracking || elapsedTime > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("STOP & SAVE")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
