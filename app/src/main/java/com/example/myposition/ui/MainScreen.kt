package com.example.myposition.ui

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.naver.maps.map.MapView
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons

@SuppressLint("MissingPermission")
@Composable
fun NaverMapScreen(onLocationChanged: (Double, Double) -> Unit, onLocationError: (String) -> Unit) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentMarker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mapView) {
        Log.d("NAVER_MAP", "LaunchedEffect 진입, mapView=$mapView")
        mapView.getMapAsync(OnMapReadyCallback { naverMap ->
            Log.d("NAVER_MAP", "getMapAsync 콜백 진입")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocation(naverMap, location, currentMarker) { newMarker ->
                        currentMarker = newMarker
                    }
                    onLocationChanged(location.latitude, location.longitude)
                } else {
                    val locationRequest = LocationRequest.create().apply {
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        interval = 1000
                        numUpdates = 1
                    }
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null) {
                                updateLocation(naverMap, loc, currentMarker) { newMarker ->
                                    currentMarker = newMarker
                                }
                                onLocationChanged(loc.latitude, loc.longitude)
                            } else {
                                Log.d("NAVER_MAP", "requestLocationUpdates: 위치 정보 없음")
                                onLocationError("위치 정보를 가져올 수 없습니다.")
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }.addOnFailureListener { exception ->
                onLocationError("위치 정보 에러: ${exception.message}")
            }
        })
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

private fun updateLocation(naverMap: com.naver.maps.map.NaverMap, location: Location, currentMarker: Marker?, onMarkerCreated: (Marker) -> Unit) {
    val latLng = LatLng(location.latitude, location.longitude)
    Log.d("NAVER_MAP", "위치 업데이트: ${latLng.latitude}, ${latLng.longitude}")
    
    // 기존 마커 제거
    currentMarker?.map = null
    
    // 새 마커 생성
    val marker = Marker().apply {
        position = latLng
        icon = MarkerIcons.BLACK
        iconTintColor = android.graphics.Color.RED
        width = 50
        height = 50
        captionText = "현재 위치"
        captionMinZoom = 12.0
        map = naverMap
    }
    
    // 카메라 이동
    naverMap.moveCamera(CameraUpdate.scrollTo(latLng))
    naverMap.uiSettings.isLocationButtonEnabled = true
    
    // 마커 생성 콜백
    onMarkerCreated(marker)
}

@Composable
fun MainScreen(
    userEmail: String,
    userNickname: String,
    userPassword: String = "",
    onLogout: () -> Unit,
    userInfo: String,
    userId: Int,
    userJson: String
) {
    val apiService = remember { com.example.myposition.api.RetrofitClient.apiService }
    var errorLog by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sentToServer by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf("") }
    
    // userJson에서 user_id 파싱
    val parsedUserId = remember(userJson) {
        if (userJson.isNotEmpty()) {
            try {
                val jsonObject = org.json.JSONObject(userJson)
                jsonObject.getInt("user_id")
            } catch (e: Exception) {
                -1
            }
        } else {
            -1
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "환영합니다!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            text = "ID: $parsedUserId",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Column {
                Text(
                    text = "이메일: $userEmail",
                    fontSize = 16.sp,
                )
                Text(
                    text = "닉네임: $userNickname",
                    fontSize = 16.sp,
                )
                Text(
                    text = "사용자 ID: $parsedUserId",
                    fontSize = 16.sp,
                )
                Text(
                    text = "위도: ${latitude ?: "정보 없음"}",
                    fontSize = 16.sp,
                )
                Text(
                    text = "경도: ${longitude ?: "정보 없음"}",
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 1.dp)
            ) {
                Text("로그아웃", fontSize = 12.sp)
            }
        }
        
        // 네이버 지도 추가
        NaverMapScreen(
            onLocationChanged = { lat, lng ->
                latitude = lat
                longitude = lng
            },
            onLocationError = { error ->
                locationError = error
            }
        )
        // 지도 아래에 서버에서 받은 JSON 데이터 표시
        if (userJson.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "서버에서 받은 사용자 정보:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = userJson,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        // 위치 정보 전송 버튼 추가
        Button(
            onClick = {
                val lat = latitude
                val lng = longitude
                if (lat != null && lng != null && parsedUserId > 0) {
                    isLoading = true
                    errorLog = ""
                    Log.d("LocationSend", "위치 전송 시도: userId=$parsedUserId, lat=$lat, lng=$lng")
                    apiService.sendLocation(
                        userId = parsedUserId.toString(),
                        latitude = lat,
                        longitude = lng
                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                        override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                            isLoading = false
                            Log.d("LOCATION_SEND", "Response code: ${response.code()}")
                            if (!response.isSuccessful) {
                                errorLog = "서버 오류: ${response.code()}"
                                Log.e("LOCATION_SEND", "Server error: ${response.code()}")
                            } else {
                                val body = response.body()
                                Log.d("LOCATION_SEND", "Response body: $body")
                                if (body?.get("success") != true) {
                                    errorLog = body?.get("error") as? String ?: "위치 전송 실패"
                                    Log.e("LOCATION_SEND", "Location send failed: $errorLog")
                                } else {
                                    errorLog = "" // 성공 시 로그 없음
                                    sentToServer = true
                                    Log.d("LOCATION_SEND", "Location sent successfully")
                                }
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                            isLoading = false
                            errorLog = "네트워크 오류: ${t.message}"
                            Log.e("LOCATION_SEND", "Network error: ${t.message}", t)
                        }
                    })
                } else {
                    errorLog = "위치 정보가 준비되지 않았습니다. (userId: $parsedUserId, lat: $latitude, lng: $longitude)"
                    Log.e("LOCATION_SEND", "Location data not ready: userId=$parsedUserId, lat=$latitude, lng=$longitude")
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("위치 정보 전송")
        }
        if (locationError.isNotEmpty()) {
            Text(
                text = locationError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (errorLog.isNotEmpty()) {
            Text(
                text = errorLog,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
} 