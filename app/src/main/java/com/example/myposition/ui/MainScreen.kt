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
import kotlinx.coroutines.delay
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.example.myposition.model.Friend
import com.example.myposition.model.FriendsListResponse
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Navigation
import androidx.navigation.Navigation
import android.widget.Toast
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.foundation.clickable

@SuppressLint("MissingPermission")
@Composable
fun NaverMapScreen(
    userNickname: String,
    friendLocations: List<Map<String, Any>>,
    selectedLatLngTriple: Triple<Double, Double, Long>?,
    onLocationChanged: (Double, Double) -> Unit,
    onLocationError: (String) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var map by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }
    var friendMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }

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
            Log.d("NAVER_MAP", "getMapAsync 콜백 진입")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateLocation(naverMap, location, currentMarker, userNickname) { newMarker ->
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
                                updateLocation(naverMap, loc, currentMarker, userNickname) { newMarker ->
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
            .fillMaxSize()
    )

    // 친구 위치 마커 표시
    LaunchedEffect(map, friendLocations) {
        map?.let { naverMap ->
            // 기존 친구 마커 제거
            friendMarkers.forEach { it.map = null }
            // 새 친구 마커 생성
            friendMarkers = friendLocations.mapNotNull { friend ->
                val lat = (friend["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                val lng = (friend["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                val nick = friend["nickname"] as? String ?: ""
                Marker().apply {
                    position = LatLng(lat, lng)
                    icon = MarkerIcons.BLACK
                    iconTintColor = android.graphics.Color.BLUE
                    captionText = nick
                    captionMinZoom = 12.0
                    map = naverMap
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
}

private fun updateLocation(
    naverMap: com.naver.maps.map.NaverMap,
    location: Location,
    currentMarker: Marker?,
    userNickname: String,
    onMarkerCreated: (Marker) -> Unit
) {
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
        captionText = userNickname
        captionMinZoom = 12.0
        map = naverMap
    }
    
    // 카메라 이동
    naverMap.moveCamera(CameraUpdate.scrollTo(latLng))
    naverMap.uiSettings.isLocationButtonEnabled = true
    
    // 마커 생성 콜백
    onMarkerCreated(marker)
}

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // 친구 검색 관련 상태
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // 친구 관리 상태
    var friendList by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isFriendsLoading by remember { mutableStateOf(false) }
    
    // 친구 위치 상태
    var friendLocations by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isFriendLocationsLoading by remember { mutableStateOf(false) }
    
    // 상태를 Triple<Double, Double, Long>?로 관리
    var selectedLatLngTriple by remember { mutableStateOf<Triple<Double, Double, Long>?>(null) }

    // 탭 상태 (0: 검색, 1: 목록)
    var selectedTabIndex by remember { mutableStateOf(0) }
    // 이메일 보기 상태
    var selectedEmail by remember { mutableStateOf<String?>(null) }

    // userId 직접 사용
    val gid = userId

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
                                            "longitude" to loc.longitude
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

    // 위치 변경 감지 및 전송을 위한 LaunchedEffect
    LaunchedEffect(gid, latitude, longitude) {
        val currentLat = latitude
        val currentLng = longitude
        
        if (currentLat != null && currentLng != null && gid > 0) {
            val lastLocation = lastSentLocation
            val isSignificantChange = lastLocation == null || 
                Math.abs(currentLat - lastLocation.first) > 0.0001 || 
                Math.abs(currentLng - lastLocation.second) > 0.0001

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

    // 내 위치 LatLng 계산
    val myLatLng = if (latitude != null && longitude != null) LatLng(latitude!!, longitude!!) else null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("내 위치/친구 관리", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(text = userEmail, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "로그아웃", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
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
                Toast.makeText(context, "onAddFriend 호출됨", Toast.LENGTH_SHORT).show()
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
            modifier = Modifier.padding(innerPadding)
        )
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
    modifier: Modifier
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
            onLocationError = { _ -> }
        )
        // 하단 카드 (sheet처럼 겹치기)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 500.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 상단 버튼 Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { showSearch = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) { Text("친구 검색", color = if (showSearch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                    Button(
                        onClick = { showSearch = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) { Text("친구 목록", color = if (!showSearch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) }
                }
                Spacer(Modifier.height(8.dp))
                if (showSearch) {
                    // 검색 UI
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("이메일 또는 닉네임") },
                        singleLine = true
                    )
                    Button(
                        onClick = onSearch,
                        enabled = searchQuery.isNotEmpty() && !isSearching,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp, bottom = 8.dp)
                    ) { Text("검색") }
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
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
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
                                        }
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
                            val loc = friendLocations.find { it.gid == friend.gid }
                            val distanceKm = if (loc != null && myLocation != null) {
                                val d = haversine(myLocation.latitude, myLocation.longitude, loc.latitude, loc.longitude)
                                String.format("%.1fkm", d)
                            } else {
                                "-"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                Spacer(Modifier.width(12.dp))
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
                                Text(distanceKm, color = Color.Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                IconButton(onClick = { onDeleteFriend(friend) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "친구 삭제", tint = Color.Red)
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