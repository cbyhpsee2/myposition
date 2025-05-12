package com.example.myposition

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.OnMapReadyCallback

@SuppressLint("MissingPermission")
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(mapView) {
        mapView?.getMapAsync(OnMapReadyCallback { naverMap ->
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    Log.d("NAVER_MAP", "lastLocation: ${latLng.latitude}, ${latLng.longitude}")
                    naverMap.moveCamera(CameraUpdate.scrollTo(latLng))
                    naverMap.uiSettings.isLocationButtonEnabled = true
                } else {
                    // lastLocation이 null이면 requestLocationUpdates로 위치 요청
                    val locationRequest = LocationRequest.create().apply {
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        interval = 1000
                        numUpdates = 1
                    }
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null) {
                                val latLng = LatLng(loc.latitude, loc.longitude)
                                Log.d("NAVER_MAP", "requestLocationUpdates: ${latLng.latitude}, ${latLng.longitude}")
                                naverMap.moveCamera(CameraUpdate.scrollTo(latLng))
                                naverMap.uiSettings.isLocationButtonEnabled = true
                            } else {
                                Log.d("NAVER_MAP", "requestLocationUpdates: 위치 정보 없음")
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "네이버 지도 예제",
            fontSize = 24.sp,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mapView = it }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
} 