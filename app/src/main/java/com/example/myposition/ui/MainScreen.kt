package com.example.myposition.ui

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.naver.maps.map.util.MarkerIcons
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.example.myposition.model.Friend
import com.example.myposition.model.FriendsListResponse
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.clickable
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.awaitCancellation
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.util.FusedLocationSource
import android.app.Activity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.naver.maps.map.NaverMap
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import com.example.myposition.R

// --- Instagram 스타일 컬러/테마 정의 ---
object InstagramColors {
    val primaryBlue = Color(0xFF405DE6)
    val purple = Color(0xFF5851DB)
    val gradientPink = Color(0xFFE1306C)
    val orange = Color(0xFFFD1D1D)
    val yellow = Color(0xFFF77737)
    val lightBackground = Color(0xFFFAFAFA)
    val darkBackground = Color(0xFF121212)
    val cardBackground = Color.White
    val darkCardBackground = Color(0xFF262626)
}

private val InstagramLightColorScheme = lightColorScheme(
    primary = InstagramColors.primaryBlue,
    secondary = InstagramColors.gradientPink,
    tertiary = InstagramColors.yellow,
    background = InstagramColors.lightBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black
)

@Composable
fun InstagramTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InstagramLightColorScheme,
        typography = Typography(
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

@SuppressLint("MissingPermission")
@Composable
fun NaverMapScreen(
    userNickname: String,
    friendLocations: List<Map<String, Any>>,
    selectedLatLngTriple: Triple<Double, Double, Long>?,
    onLocationChanged: (Double, Double) -> Unit,
    onLocationError: (String) -> Unit,
    friendPath: List<LatLng>,
    myLatitude: Double?,
    myLongitude: Double?,
    onMapReady: (com.naver.maps.map.NaverMap) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var map by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }
    var friendMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var friendPolyline by remember { mutableStateOf<PolylineOverlay?>(null) }
    val locationSource = remember { FusedLocationSource(context as Activity, 1000) }
    var naverMapRef by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }

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
            map = naverMap
            naverMap.uiSettings.isLocationButtonEnabled = false
            naverMap.locationSource = locationSource
            naverMap.locationTrackingMode = LocationTrackingMode.Follow
            Log.d("NAVER_MAP", "getMapAsync 콜백 진입")
            val density = context.resources.displayMetrics.density
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocation(naverMap, location, currentMarker, userNickname)
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
                                updateLocation(naverMap, loc, currentMarker, userNickname)
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
            onMapReady(naverMap)
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        // 인스타 스타일 FAB
        FloatingActionButton(
            onClick = {
                val naverMap = map
                if (naverMap != null && myLatitude != null && myLongitude != null) {
                    naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(myLatitude, myLongitude)))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 300.dp)
                .size(56.dp),
            containerColor = InstagramColors.primaryBlue,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "내 위치로 이동",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // 친구 위치 마커 표시
    LaunchedEffect(map, friendLocations) {
        Log.d("DEBUG", "friendLocations: $friendLocations")
        if (map == null) {
            Log.d("DEBUG", "map이 아직 초기화되지 않음")
            return@LaunchedEffect
        }
        map?.let { naverMap ->
            Log.d("DEBUG", "map 객체: $naverMap, 타입: ${naverMap.javaClass.name}")
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                // 기존 친구 마커 제거
                friendMarkers.forEach { it.map = null }
                // 새 친구 마커 생성
                friendMarkers = friendLocations.mapNotNull { friend ->
                    val lat = (friend["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val lng = (friend["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val nick = friend["nickname"] as? String ?: ""
                    val marker = Marker().apply {
                        position = LatLng(lat, lng)
                        icon = MarkerIcons.BLACK
                        iconTintColor = android.graphics.Color.BLUE
                        captionText = nick
                        captionMinZoom = 1.0
                        try {
                            setMap(naverMap)
                        } catch (e: Exception) {
                            Log.e("DEBUG", "마커 setMap 할당 중 예외", e)
                        }
                    }
                    Log.d("DEBUG", "마커 생성 후 marker.map: ${marker.map}, naverMap: $naverMap")
                    marker
                }
            }
        }
    }

    // 선택된 위치로 카메라 이동
    LaunchedEffect(selectedLatLngTriple) {
        Log.d("NAV_DEBUG", "selectedLatLngTriple 변경: $selectedLatLngTriple")
        if (map == null) {
            Log.e("NAVER_MAP", "지도 객체(map)가 아직 초기화되지 않았음")
        }
        selectedLatLngTriple?.let { (lat, lng, _) ->
            map?.moveCamera(CameraUpdate.scrollTo(LatLng(lat, lng)))
            Log.d("NAV_DEBUG", "지도 moveCamera 실행: $lat, $lng")
        }
    }

    // PolylineOverlay로 점선 경로 표시
    LaunchedEffect(map, friendPath) {
        val naverMap = map
        Log.d("DEBUG", "LaunchedEffect: map=$naverMap, friendPath=$friendPath")
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            friendPolyline?.map = null
            if (friendPath.size > 1 && naverMap != null) {
                Log.d("DEBUG", "PolylineOverlay에 전달되는 friendPath: $friendPath")
                val polyline = PolylineOverlay().apply {
                    coords = friendPath
                    color = android.graphics.Color.RED
                    width = 5
                }
                polyline.map = naverMap
                friendPolyline = polyline
                val bounds = com.naver.maps.geometry.LatLngBounds.Builder().apply {
                    friendPath.forEach { include(it) }
                }.build()
                naverMap.moveCamera(com.naver.maps.map.CameraUpdate.fitBounds(bounds, 100))
                Log.d("DEBUG", "PolylineOverlay 생성 완료: $friendPolyline, map 할당: ${friendPolyline?.map}")
            } else {
                Log.d("DEBUG", "PolylineOverlay 생성 조건 불충족: 좌표 개수 ${friendPath.size}")
            }
        }
    }
}

private fun updateLocation(
    naverMap: NaverMap,
    location: Location,
    currentMarker: Marker?,
    userNickname: String
) {
    val latLng = LatLng(location.latitude, location.longitude)
    Log.d("NAVER_MAP", "위치 업데이트: ${latLng.latitude}, ${latLng.longitude}")
    // 기존 마커 제거
    currentMarker?.map = null
    // 카메라 이동만 남김
    naverMap.moveCamera(CameraUpdate.scrollTo(latLng))
    naverMap.uiSettings.isLocationButtonEnabled = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userEmail: String,
    userNickname: String,
    userProfileImageUrl: String,
    userPassword: String = "",
    onLogout: () -> Unit,
    userInfo: String,
    userId: Int,
    userJson: String
) {
    val context = LocalContext.current
    val apiService = remember { com.example.myposition.api.RetrofitClient.apiService }
    val snackbarHostState = remember { SnackbarHostState() }
    var errorLog by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf("") }
    var lastSentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLocationChanged by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var friendList by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isFriendsLoading by remember { mutableStateOf(false) }
    var friendLocations by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isFriendLocationsLoading by remember { mutableStateOf(false) }
    var selectedLatLngTriple by remember { mutableStateOf<Triple<Double, Double, Long>?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedEmail by remember { mutableStateOf<String?>(null) }
    val gid = userId
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasLocationPermission) {
        latitude = null
        longitude = null
    }
    var selectedFriendPath by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var selectedFriendId by remember { mutableStateOf<Int?>(null) }
    fun refreshFriendPath(friendId: Int) {
        apiService.getFriendLocationHistory(friendId).enqueue(object : retrofit2.Callback<com.example.myposition.model.FriendLocationHistoryResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myposition.model.FriendLocationHistoryResponse>,
                response: retrofit2.Response<com.example.myposition.model.FriendLocationHistoryResponse>
            ) {
                val body = response.body()
                Log.d("DEBUG", "API 응답 raw: ${response.raw()}")
                Log.d("DEBUG", "API 응답 body: $body")
                if (response.isSuccessful && body != null && body.success) {
                    Log.d("DEBUG", "API 반환 locations: ${body.locations}")
                    selectedFriendPath = body.locations.map { LatLng(it.latitude, it.longitude) }
                } else {
                    Log.d("DEBUG", "API 실패 또는 데이터 없음: ${response.errorBody()?.string()}")
                    selectedFriendPath = emptyList()
                }
            }
            override fun onFailure(
                call: retrofit2.Call<com.example.myposition.model.FriendLocationHistoryResponse>,
                t: Throwable
            ) {
                Log.d("DEBUG", "API 호출 실패: ${t.message}")
                selectedFriendPath = emptyList()
            }
        })
    }

    // 친구 목록 로드 함수 (최상단으로 이동)
    fun loadFriendsList() {
        isFriendsLoading = true
        Log.d("FriendList", "내gid $gid")
        apiService.getFriendsList(gid).enqueue(object : retrofit2.Callback<FriendsListResponse> {
            override fun onResponse(call: retrofit2.Call<FriendsListResponse>, response: retrofit2.Response<FriendsListResponse>) {
                isFriendsLoading = false
                val body = response.body()
                Log.d("FriendList", "친구 목록 raw body: $body")
                if (response.isSuccessful && body != null && body.success) {
                    friendList = body.friends ?: emptyList()
                    // 친구 위치도 함께 로드
                    apiService.getFriendsLocations(gid).enqueue(object : retrofit2.Callback<com.example.myposition.model.FriendLocationResponse> {
                        override fun onResponse(
                            call: retrofit2.Call<com.example.myposition.model.FriendLocationResponse>,
                            response: retrofit2.Response<com.example.myposition.model.FriendLocationResponse>
                        ) {
                            isFriendLocationsLoading = false
                            val locBody = response.body()
                            Log.d("FriendLoc", "getFriendsLocations 응답 body: $locBody")
                            if (response.isSuccessful && locBody != null && locBody.success) {
                                try {
                                    friendLocations = locBody.locations.map { loc ->
                                        mapOf(
                                            "gid" to loc.gid,
                                            "nickname" to loc.nickname,
                                            "latitude" to loc.latitude,
                                            "longitude" to loc.longitude,
                                            "profileImageUrl" to (loc.profileImageUrl ?: "")
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("FriendLoc", "친구 위치 파싱 실패", e)
                                    friendLocations = emptyList()
                                }
                            } else {
                                Log.e("FriendLoc", "친구 위치 로드 실패")
                            }
                        }
                        override fun onFailure(
                            call: retrofit2.Call<com.example.myposition.model.FriendLocationResponse>,
                            t: Throwable
                        ) {
                            isFriendLocationsLoading = false
                            Log.e("FriendLoc", "네트워크 오류: ${t.message}", t)
                        }
                    })
                } else {
                    Log.e("FriendList", "친구 목록 갱신 실패: $body")
                }
            }
            override fun onFailure(call: retrofit2.Call<FriendsListResponse>, t: Throwable) {
                isFriendsLoading = false
                Log.e("FriendList", "친구 목록 네트워크 오류: ${t.message}", t)
            }
        })
    }

    // MainScreen 진입 시 친구 목록 자동 로드
    LaunchedEffect(gid) {
        loadFriendsList()
    }

    // 친구 위치 5초/30초마다 자동 갱신 (포그라운드/백그라운드)
    var lastFriendLocationUpdateTime by remember { mutableStateOf<Long?>(null) }
    var isForeground by remember { mutableStateOf(true) } // 실제로는 LifecycleObserver로 감지 필요
    LaunchedEffect(gid, isForeground, lastFriendLocationUpdateTime) {
        val pollingInterval = if (isForeground) 5000L else 30000L
        while (gid > 0) {
            apiService.getFriendsLocations(gid, lastFriendLocationUpdateTime).enqueue(object : retrofit2.Callback<com.example.myposition.model.FriendLocationResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.myposition.model.FriendLocationResponse>,
                    response: retrofit2.Response<com.example.myposition.model.FriendLocationResponse>
                ) {
                    isFriendLocationsLoading = false
                    val locBody = response.body()
                    if (response.isSuccessful && locBody != null && locBody.success) {
                        if (locBody.locations.isNotEmpty()) {
                            friendLocations = locBody.locations.map { loc ->
                                mapOf(
                                    "gid" to loc.gid,
                                    "nickname" to loc.nickname,
                                    "latitude" to loc.latitude,
                                    "longitude" to loc.longitude,
                                    "profileImageUrl" to (loc.profileImageUrl ?: "")
                                )
                            }
                            lastFriendLocationUpdateTime = System.currentTimeMillis()
                        }
                    } else {
                        Log.e("FriendLoc", "친구 위치 로드 실패")
                    }
                }
                override fun onFailure(
                    call: retrofit2.Call<com.example.myposition.model.FriendLocationResponse>,
                    t: Throwable
                ) {
                    isFriendLocationsLoading = false
                    Log.e("FriendLoc", "네트워크 오류: ${t.message}", t)
                }
            })
            kotlinx.coroutines.delay(pollingInterval)
        }
    }

    // 위치 변경 감지 및 전송을 위한 LaunchedEffect
    LaunchedEffect(gid, latitude, longitude) {
        val currentLat = latitude
        val currentLng = longitude
        val DISTANCE_THRESHOLD_METERS = 15.0 // 15미터 이상 이동 시만 업데이트
        if (currentLat != null && currentLng != null && gid > 0) {
            val lastLocation = lastSentLocation
            val isSignificantChange = lastLocation == null ||
                haversine(currentLat, currentLng, lastLocation.first, lastLocation.second) * 1000 > DISTANCE_THRESHOLD_METERS

            if (isSignificantChange) {
                isLocationChanged = true
                lastSentLocation = Pair(currentLat, currentLng)
                Log.d("LocationSend", "위치 변경 감지: gid=$gid, lat=$currentLat, lng=$currentLng")
            }
        }
    }

    // 위치 변경 시 30초마다 전송하는 LaunchedEffect
    LaunchedEffect(isLocationChanged) {
        if (isLocationChanged) {
            while (true) {
                val lat = latitude
                val lng = longitude
                if (lat != null && lng != null && gid > 0) {
                    Log.d("LocationSend", "주기적 위치 전송: gid=$gid, lat=$lat, lng=$lng")
                    apiService.sendLocation(
                        gid = gid,
                        latitude = lat,
                        longitude = lng
                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                        override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                            if (!response.isSuccessful) {
                                Log.e("LOCATION_SEND", "Server error: ${response.code()}")
                            } else {
                                Log.d("LOCATION_SEND", "Location sent successfully")
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                            Log.e("LOCATION_SEND", "Network error: ${t.message}", t)
                        }
                    })
                }
                kotlinx.coroutines.delay(30000) // 30초 대기
            }
        }
    }
    
    var successMessage by remember { mutableStateOf("") }

    // 메시지 발생 시 스낵바 표시
    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(successMessage)
            successMessage = ""
        }
    }
    LaunchedEffect(errorLog) {
        if (errorLog.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorLog)
            errorLog = ""
        }
    }
    LaunchedEffect(locationError) {
        if (locationError.isNotEmpty()) {
            snackbarHostState.showSnackbar(locationError)
            locationError = ""
        }
    }

    // 내 위치 LatLng 계산 (권한 없으면 null)
    val myLatLng = if (hasLocationPermission && latitude != null && longitude != null) LatLng(latitude!!, longitude!!) else null

    // 포그라운드 위치 서비스 시작
    LaunchedEffect(userId) {
        if (userId > 0) {
            val intent = Intent(context, com.example.myposition.LocationForegroundService::class.java)
            intent.putExtra("user_id", userId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // 내 위치 실시간 갱신: 앱 실행 중 항상 최신 위치를 받아와 상태에 반영
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 3000 // 3초마다 위치 요청
            fastestInterval = 2000
        }
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                } else {
                    locationError = "내 위치를 받아올 수 없습니다. 위치 권한/서비스를 확인하세요."
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            awaitCancellation()
        } finally {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // MainScreen에서만 naverMapRef와 onMoveToMyLocation 핸들러 선언
    var naverMapRef by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }
    val handleMoveToMyLocation = {
        val myLat = myLatLng?.latitude
        val myLng = myLatLng?.longitude
        if (naverMapRef != null && myLat != null && myLng != null) {
            naverMapRef!!.moveCamera(CameraUpdate.scrollTo(LatLng(myLat, myLng)))
        }
    }

    InstagramTheme {
        // 상태바 배경색 흰색으로 지정
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            window?.statusBarColor = Color.White.toArgb()
            window?.let {
                WindowCompat.getInsetsController(it, view)?.isAppearanceLightStatusBars = true
            }
        }
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { snackbarData ->
                    Snackbar(
                        modifier = Modifier
                            .padding(16.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = InstagramColors.darkBackground.copy(alpha = 0.9f),
                        contentColor = Color.White,
                        snackbarData = snackbarData
                    )
                }
            },
        ) { innerPadding ->
            FriendLocationScreen(
                myGid = gid,
                myLocation = myLatLng,
                friends = friendList,
                friendLocations = friendLocations.mapNotNull { locMap ->
                    try {
                        com.example.myposition.model.FriendLocation(
                            gid = (locMap["gid"] as? Number)?.toInt() ?: return@mapNotNull null,
                            nickname = locMap["nickname"] as? String ?: "",
                            latitude = (locMap["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null,
                            longitude = (locMap["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null,
                            updatedAt = ""
                        )
                    } catch (e: Exception) { null }
                },
                selectedLatLngTriple = selectedLatLngTriple,
                onFriendNavigate = { loc ->
                    Log.d("NAV_DEBUG", "onFriendNavigate 호출: $loc")
                    selectedLatLngTriple = Triple(loc.latitude, loc.longitude, System.currentTimeMillis())
                },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isSearching = isSearching,
                searchResults = searchResults,
                onSearch = {
                    isSearching = true
                    apiService.searchUsers(
                        keyword = searchQuery,
                        gid = gid
                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                        override fun onResponse(
                            call: retrofit2.Call<Map<String, Any>>,
                            response: retrofit2.Response<Map<String, Any>>
                        ) {
                            isSearching = false
                            Log.d("SEARCH_API_DEBUG", "searchUsers response: ${response.body()}")
                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body != null && body["success"] == true) {
                                    @Suppress("UNCHECKED_CAST")
                                    val users = body["users"] as? List<Map<String, Any>> ?: emptyList()
                                    searchResults = users
                                } else {
                                    val errorMsg = body?.get("error") as? String ?: "검색 실패"
                                    errorLog = errorMsg
                                }
                            } else {
                                val errorMsg = "검색 실패: ${response.code()}"
                                errorLog = errorMsg
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                            isSearching = false
                            val errorMsg = "네트워크 오류: ${t.message}"
                            errorLog = errorMsg
                        }
                    })
                },
                onAddFriend = { user ->
                    println("[DEBUG] 친구 추가 버튼 클릭됨")
                    //Toast.makeText(context, "onAddFriend 호출됨", Toast.LENGTH_SHORT).show()
                    val friendGid = when (val id = user["gid"]) {
                        is Int -> id
                        is Long -> id.toInt()
                        is String -> id.toIntOrNull()
                        else -> null
                    }
                    Log.d("FriendAdd", "내 gid = $gid, 친구 gid = $friendGid, user['gid'] = ${user["gid"]}, userId type = ${user["gid"]?.javaClass?.name}")
                    if (friendGid != null) {
                        apiService.addFriend(
                            gid = gid,
                            friendGid = friendGid
                        ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                            override fun onResponse(
                                call: retrofit2.Call<Map<String, Any>>,
                                response: retrofit2.Response<Map<String, Any>>
                            ) {
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    if (body != null && body["success"] == true) {
                                        searchResults = searchResults.filter { it != user }
                                        loadFriendsList()
                                        successMessage = "${user["nickname"]}님이 친구로 추가되었습니다."
                                        MainScope().launch {
                                            kotlinx.coroutines.delay(3000)
                                            successMessage = ""
                                        }
                                    } else {
                                        val errorMsg = body?.get("error") as? String ?: "친구 추가 실패"
                                        errorLog = errorMsg
                                    }
                                } else {
                                    val errorMsg = "친구 추가 실패: ${response.code()}"
                                    errorLog = errorMsg
                                }
                            }
                            override fun onFailure(
                                call: retrofit2.Call<Map<String, Any>>,
                                t: Throwable
                            ) {
                                val errorMsg = "네트워크 오류: ${t.message}"
                                errorLog = errorMsg
                            }
                        })
                    } else {
                        errorLog = "사용자 ID 파싱 실패: user['gid'] = ${user["gid"]}, type = ${user["gid"]?.javaClass?.name}"
                    }
                },
                onDeleteFriend = { friend ->
                    apiService.deleteFriend(
                        gid = gid,
                        friendGid = friend.gid
                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                        override fun onResponse(
                            call: retrofit2.Call<Map<String, Any>>,
                            response: retrofit2.Response<Map<String, Any>>
                        ) {
                            if (response.isSuccessful && response.body()?.get("success") == true) {
                                loadFriendsList()
                                successMessage = "${friend.nickname}님이 친구 목록에서 삭제되었습니다."
                            } else {
                                val errorMsg = response.body()?.get("error") as? String ?: "친구 삭제 실패"
                                errorLog = errorMsg
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                            val errorMsg = "네트워크 오류: ${t.message}"
                            errorLog = errorMsg
                        }
                    })
                },
                onShowMessage = { successMessage = it },
                friendPath = selectedFriendPath,
                onShowFriendPath = { friendId ->
                    selectedFriendId = friendId
                    refreshFriendPath(friendId)
                },
                onMoveToMyLocation = handleMoveToMyLocation,
                modifier = Modifier.padding(innerPadding),
                userProfileImageUrl = userProfileImageUrl,
                userNickname = userNickname,
                userEmail = userEmail,
                onLogout = onLogout
            )
        }
    }

    // 5초마다 자동 경로 갱신 LaunchedEffect 추가
    LaunchedEffect(selectedFriendId) {
        while (selectedFriendId != null) {
            refreshFriendPath(selectedFriendId!!)
            kotlinx.coroutines.delay(5000)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendLocationScreen(
    myGid: Int,
    myLocation: LatLng?,
    friends: List<com.example.myposition.model.Friend>,
    friendLocations: List<com.example.myposition.model.FriendLocation>,
    selectedLatLngTriple: Triple<Double, Double, Long>?,
    onFriendNavigate: (com.example.myposition.model.FriendLocation) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean,
    searchResults: List<Map<String, Any>>,
    onSearch: () -> Unit,
    onAddFriend: (Map<String, Any>) -> Unit,
    onDeleteFriend: (com.example.myposition.model.Friend) -> Unit,
    onShowMessage: (String) -> Unit,
    friendPath: List<LatLng>,
    onShowFriendPath: (Int) -> Unit,
    onMoveToMyLocation: () -> Unit,
    modifier: Modifier,
    userProfileImageUrl: String,
    userNickname: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        // 지도 전체화면
        NaverMapScreen(
            userNickname = "나",
            friendLocations = friendLocations.map {
                mapOf(
                    "nickname" to it.nickname,
                    "latitude" to it.latitude,
                    "longitude" to it.longitude
                )
            },
            selectedLatLngTriple = selectedLatLngTriple,
            onLocationChanged = { _, _ -> },
            onLocationError = { _ -> },
            friendPath = friendPath,
            myLatitude = myLocation?.latitude,
            myLongitude = myLocation?.longitude,
            onMapReady = { }
        )
        // 하단 카드 (sheet처럼 겹치기)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp)
                .padding(start = 8.dp, end = 8.dp, bottom = 48.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 4.dp,
            color = Color(0xFFF5F7FA)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 내 프로필 Surface+Row 추가
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFFFFF), // 친구검색/목록 Surface와 동일
                    shadowElevation = 0.5.dp   // 그림자 더 연하게
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        if (userProfileImageUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(userProfileImageUrl),
                                contentDescription = "내 프로필",
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = userNickname,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "친구 ${friends.size}명",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InstagramColors.primaryBlue,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "로그아웃",
                                tint = InstagramColors.primaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 친구검색/친구목록 버튼 Row Surface로 감싸기
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    color = Color(0xFFEBEEF5), // 동일한 연한 회색
                    shadowElevation = 0.5.dp,  // 그림자 더 연하게
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showSearch = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSearch) Color(0xFF3B5BFE) else Color.White,
                                contentColor = if (showSearch) Color.White else Color.Black
                            )
                        ) { Text("친구 검색") }
                        Button(
                            onClick = { showSearch = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!showSearch) Color(0xFF3B5BFE) else Color.White,
                                contentColor = if (!showSearch) Color.White else Color.Black
                            )
                        ) { Text("친구 목록") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (showSearch) {
                    // 검색 UI
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("이메일 또는 닉네임") },
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onSearch,
                            enabled = searchQuery.isNotEmpty() && !isSearching,
                            modifier = Modifier.height(56.dp)
                        ) { Text("검색") }
                    }
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            items(searchResults) { user ->
                                val userGid = when (val id = user["gid"]) {
                                    is Int -> id
                                    is Long -> id.toInt()
                                    is String -> id.toIntOrNull()
                                    else -> null
                                }
                                val isMe = userGid == myGid
                                val isAlreadyFriend = friends.any { it.gid == userGid }
                                val profileImageUrl = user["profileImageUrl"] as? String
                                    ?: user["profile_image_url"] as? String
                                    ?: ""
                                val profileImageUrlWithTimestamp = if (profileImageUrl.isNotBlank()) {
                                    profileImageUrl + "?ts=" + System.currentTimeMillis()
                                } else ""
                                Log.d("PROFILE_IMAGE_DEBUG", "userGid=$userGid, profileImageUrl=$profileImageUrl, profileImageUrlWithTimestamp=$profileImageUrlWithTimestamp")
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (profileImageUrlWithTimestamp.isNotBlank()) {
                                            val painter = rememberAsyncImagePainter(
                                                ImageRequest.Builder(LocalContext.current)
                                                    .data(profileImageUrlWithTimestamp)
                                                    .diskCachePolicy(CachePolicy.DISABLED)
                                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                                    .build()
                                            )
                                            Image(
                                                painter = painter,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            user["nickname"] as? String ?: "",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                if (userGid != null) onFriendNavigate(com.example.myposition.model.FriendLocation(
                                                    gid = userGid,
                                                    nickname = user["nickname"] as? String ?: "",
                                                    latitude = user["latitude"] as? Double ?: 0.0,
                                                    longitude = user["longitude"] as? Double ?: 0.0,
                                                    updatedAt = ""
                                                ))
                                            }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        when {
                                            isMe -> {
                                                Text("나", color = Color.Gray)
                                            }
                                            isAlreadyFriend -> {
                                                Text("이미 친구", color = Color.Gray)
                                            }
                                            else -> {
                                                Button(onClick = { onAddFriend(user) }) {
                                                    Text("친구 추가")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        Text("검색 결과 없음", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    // 친구 전체 목록 UI (닉네임, 이메일, 위치, 삭제)
                    LazyColumn {
                        items(friends) { friend ->
                            val loc = friendLocations.find { it.gid.toString() == friend.gid.toString() }
                            val profileImageUrl = loc?.profileImageUrl ?: friend.profileImageUrl ?: ""
                            Log.d("FRIEND_LIST_DEBUG", "friend=${friend}, loc=${loc}, profileImageUrl=$profileImageUrl")
                            val distanceKm = remember(myLocation, loc) {
                                if (loc != null && myLocation != null) {
                                    Log.d("DEBUG", "거리 계산: 내 위치 $myLocation, 친구 위치 $loc")
                                    val d = haversine(myLocation.latitude, myLocation.longitude, loc.latitude, loc.longitude)
                                    String.format("%.1fkm", d)
                                } else {
                                    Log.d("DEBUG", "거리 계산 불가: myLocation=$myLocation, loc=$loc")
                                    "-"
                                }
                            }
                            val isRecentlyUpdated = loc?.updatedAt?.let {
                                try {
                                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val updated = fmt.parse(it)?.time ?: 0L
                                    System.currentTimeMillis() - updated < 40_000 // 40초 이내면 갱신중
                                } catch (e: Exception) { false }
                            } ?: false
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                if (profileImageUrl.isNotBlank()) {
                                    val painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(profileImageUrl)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    )
                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                }
                                Spacer(Modifier.width(12.dp))
                                // 갱신 상태 표시: isRecentlyUpdated면 ProgressIndicator, 아니면 동그라미
                                if (isRecentlyUpdated) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.Green,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        friend.nickname,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            Log.d("NAV_DEBUG", "닉네임 클릭: ${friend.nickname}, loc=$loc")
                                            if (loc != null) onFriendNavigate(loc)
                                        }
                                    )
                                }
                                IconButton(onClick = {
                                    onShowMessage(friend.email ?: "")
                                }) {
                                    Icon(Icons.Default.Email, contentDescription = "이메일 보기")
                                }
                                Text(distanceKm, color = Color(0xFF555555), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                IconButton(onClick = { onDeleteFriend(friend) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "친구 삭제", tint = Color(0xFF555555))
                                }
                                IconButton(
                                    onClick = { if (friend.gid != null) onShowFriendPath(friend.gid) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.run2),
                                        contentDescription = "이동",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
} 