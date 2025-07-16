package com.example.cyclingtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(rideId: Long, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val rideDao = db.rideDao()
    val ride by rideDao.getRideById(rideId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (ride == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                RideDetailMap(ride = ride!!)
                RideDetailStats(ride = ride!!)
            }
        }
    }
}

@Composable
fun RideDetailMap(ride: Ride) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(ride.path) {
        if (ride.path.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            for (point in ride.path) {
                boundsBuilder.include(point)
            }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
        cameraPositionState = cameraPositionState
    ) {
        Polyline(points = ride.path, color = Color.Blue, width = 15f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailStats(ride: Ride) {
    val avgSpeed = if (ride.durationInMillis > 0) (ride.distanceInMeters / (ride.durationInMillis / 1000f)) * 3.6f else 0f

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ride Summary",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            StatRow("Date:", formatDate(ride.timestamp))
            StatRow("Distance:", "%.2f km".format(ride.distanceInMeters / 1000f))
            StatRow("Duration:", formatDuration(ride.durationInMillis))
            StatRow("Average Speed:", "%.1f km/h".format(avgSpeed))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(text = value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
