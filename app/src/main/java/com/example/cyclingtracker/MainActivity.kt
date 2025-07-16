package com.example.cyclingtracker

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclingtracker.data.AppDatabase
import com.example.cyclingtracker.data.Ride
import com.example.cyclingtracker.ui.screen.HistoryScreen
import com.example.cyclingtracker.ui.screen.RideDetailScreen
import com.example.cyclingtracker.ui.theme.CyclingTrackerTheme
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String) {
    object Tracking : Screen("tracking")
    object History : Screen("history")
    object Detail : Screen("detail/{rideId}") {
        fun createRoute(rideId: Long) = "detail/$rideId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyclingTrackerTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    var currentRoute by remember { mutableStateOf(Screen.Tracking.route) }
    var selectedRideId by remember { mutableStateOf<Long?>(null) }

    when (currentRoute.substringBefore("/")) {
        "tracking" -> TrackingScreen(
            onNavigateToHistory = { currentRoute = Screen.History.route }
        )
        "history" -> HistoryScreen(
            onNavigateBack = { currentRoute = Screen.Tracking.route },
            onRideClick = { rideId ->
                selectedRideId = rideId
                currentRoute = Screen.Detail.createRoute(rideId)
            }
        )
        "detail" -> RideDetailScreen(
            rideId = selectedRideId!!,
            onNavigateBack = { currentRoute = Screen.History.route }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun TrackingScreen(onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val rideDao = remember { db.rideDao() }
    val scope = rememberCoroutineScope()

    var isTracking by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val routePath = remember { mutableStateListOf<LatLng>() }
    var totalDistance by remember { mutableFloatStateOf(0f) }
    var elapsedTimeInSeconds by remember { mutableLongStateOf(0L) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(28.7041, 77.1025), 15f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (true) {
                delay(1000)
                elapsedTimeInSeconds++
            }
        }
    }

    LaunchedEffect(isTracking, hasLocationPermission) {
        if (isTracking && hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val newLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(newLatLng, 17f)

                        if (routePath.isNotEmpty()) {
                            val lastLocation = routePath.last()
                            val distanceResult = FloatArray(1)
                            Location.distanceBetween(
                                lastLocation.latitude, lastLocation.longitude,
                                newLatLng.latitude, newLatLng.longitude,
                                distanceResult
                            )
                            totalDistance += distanceResult[0]
                        }
                        routePath.add(newLatLng)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CycleTrack") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Ride History", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                if (routePath.isNotEmpty()) {
                    Polyline(points = routePath, color = Color.Blue, width = 15f)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                StatsCard(distance = totalDistance, elapsedTime = elapsedTimeInSeconds)
                ControlButtons(
                    isTracking = isTracking,
                    elapsedTime = elapsedTimeInSeconds,
                    onStartClick = { isTracking = !isTracking },
                    onStopClick = {
                        if (routePath.size > 1) {
                            scope.launch {
                                val ride = Ride(
                                    durationInMillis = elapsedTimeInSeconds * 1000,
                                    distanceInMeters = totalDistance,
                                    path = routePath.toList()
                                )
                                rideDao.insertRide(ride)
                                Toast.makeText(context, "Ride Saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        isTracking = false
                        routePath.clear()
                        totalDistance = 0f
                        elapsedTimeInSeconds = 0L
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsCard(distance: Float, elapsedTime: Long) {
    val avgSpeed = if (elapsedTime > 0) (distance / elapsedTime) * 3.6f else 0f
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "DURATION", value = com.example.cyclingtracker.ui.screen.formatDuration(elapsedTime * 1000))
            StatItem(label = "DISTANCE", value = "%.2f KM".format(distance / 1000))
            StatItem(label = "AVG. SPEED", value = "%.1f KM/H".format(avgSpeed))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlButtons(isTracking: Boolean, elapsedTime: Long, onStartClick: () -> Unit, onStopClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onStartClick,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color(0xFFFFA726) else Color(0xFF4CAF50)
            )
        ) {
            Text(if (isTracking) "PAUSE" else "START", fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onStopClick,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            enabled = elapsedTime > 0
        ) {
            Text("STOP", fontSize = 16.sp, color = Color.White)
        }
    }
}
