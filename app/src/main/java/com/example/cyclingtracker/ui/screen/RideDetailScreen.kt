package com.example.cyclingtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclingtracker.data.Ride
import com.example.cyclingtracker.data.RideDao
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(rideId: Long, rideDao: RideDao, onBack: () -> Unit) {
    val ride by rideDao.getRideById(rideId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (ride == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val path = ride!!.path
            val cameraPositionState = rememberCameraPositionState {
                position = if (path.isNotEmpty()) {
                    CameraPosition.fromLatLngZoom(path.first(), 15f)
                } else {
                    CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(0.0, 0.0), 10f)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                GoogleMap(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    if (path.isNotEmpty()) {
                        Polyline(points = path, color = Color.Blue, width = 15f)
                    }
                }

                RideStats(ride = ride!!)
            }
        }
    }
}

@Composable
fun RideStats(ride: Ride) {
    val avgSpeed = if (ride.durationInMillis > 0) (ride.distanceInMeters / (ride.durationInMillis / 1000f)) * 3.6f else 0f
    Column(modifier = Modifier.padding(16.dp)) {
        StatRow("Date:", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ride.timestamp)))
        StatRow("Distance:", "%.2f km".format(ride.distanceInMeters / 1000f))
        StatRow("Duration:", formatDuration(ride.durationInMillis))
        StatRow("Average Speed:", "%.1f km/h".format(avgSpeed))
        StatRow("Calories Burned:", "%.0f kcal".format(ride.caloriesBurned)) // Added calories
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
        Text(text = value)
    }
}
