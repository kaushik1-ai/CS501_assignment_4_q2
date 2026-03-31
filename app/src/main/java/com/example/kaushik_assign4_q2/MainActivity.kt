package com.example.kaushik_assign4_q2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kaushik_assign4_q2.ui.theme.Kaushik_assign4_q2Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionGranted = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            Kaushik_assign4_q2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationScreen(
                        modifier = Modifier.padding(innerPadding),
                        permissionGranted = permissionGranted,
                        onRequestPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun LocationScreen(
    modifier: Modifier,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Boston City Center coordinates
    val boston = LatLng(42.3601, -71.0589)
    
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(boston, 12f)
    }
    
    var address by remember { mutableStateOf("Fetching address for Boston...") }
    val markers = remember { mutableStateListOf<LatLng>().apply { add(boston) } }

    // Update initial address for Boston
    LaunchedEffect(Unit) {
        getAddress(context, boston.latitude, boston.longitude) {
            address = it
        }
    }

    // Still handle user location if permission is granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userLatLng = LatLng(loc.latitude, loc.longitude)
                    markers.add(userLatLng)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionGranted),
            onMapClick = {
                markers.add(it)
                getAddress(context, it.latitude, it.longitude) { newAddr ->
                    address = newAddr
                }
            }
        ) {
            markers.forEachIndexed { index, pos ->
                Marker(
                    state = MarkerState(position = pos),
                    title = if (index == 0) "Boston" else "Marker ${index + 1}",
                    snippet = if (index == 0) "City of Boston" else ""
                )
            }
        }

        // Info Card
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(androidx.compose.ui.Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (!permissionGranted) "Permission Required for My Location" else "Location Info",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (!permissionGranted) Color.Red else Color.Black
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!permissionGranted) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

fun getAddress(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    onResult: (String) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())

    try {
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(lat, lng, 1) {
                onResult(it.firstOrNull()?.getAddressLine(0) ?: "No address found")
            }
        } else {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lng, 1)
            onResult(list?.firstOrNull()?.getAddressLine(0) ?: "No address found")
        }
    } catch (e: Exception) {
        onResult("Error getting address")
    }
}