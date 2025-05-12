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
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons
import com.example.myposition.model.User
import com.example.myposition.model.SearchUserResponse
import com.example.myposition.model.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

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
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf("") }
    var lastSentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLocationChanged by remember { mutableStateOf(false) }
    
    // 친구 검색 관련 상태
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // 친구 목록 관련 상태
    var friendsList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isFriendsLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: 검색, 1: 친구목록
    
    // userJson에서 user_id 파싱
    val parsedUserId = remember(userJson) {
        if (userJson.isNotEmpty()) {
            try {
                val jsonObject = org.json.JSONObject(userJson)
                jsonObject.getInt("user_id")
            } catch (e: Exception) {
                Log.e("UserParsing", "사용자 정보 파싱 실패", e)
                -1
            }
        } else {
            -1
        }
    }

    // 위치 변경 감지 및 전송을 위한 LaunchedEffect
    LaunchedEffect(parsedUserId, latitude, longitude) {
        val currentLat = latitude
        val currentLng = longitude
        
        if (currentLat != null && currentLng != null && parsedUserId > 0) {
            val lastLocation = lastSentLocation
            val isSignificantChange = lastLocation == null || 
                Math.abs(currentLat - lastLocation.first) > 0.0001 || 
                Math.abs(currentLng - lastLocation.second) > 0.0001

            if (isSignificantChange) {
                isLocationChanged = true
                lastSentLocation = Pair(currentLat, currentLng)
                Log.d("LocationSend", "위치 변경 감지: userId=$parsedUserId, lat=$currentLat, lng=$currentLng")
            }
        }
    }

    // 위치 변경 시 30초마다 전송하는 LaunchedEffect
    LaunchedEffect(isLocationChanged) {
        if (isLocationChanged) {
            while (true) {
                val lat = latitude
                val lng = longitude
                if (lat != null && lng != null && parsedUserId > 0) {
                    Log.d("LocationSend", "주기적 위치 전송: userId=$parsedUserId, lat=$lat, lng=$lng")
                    apiService.sendLocation(
                        userId = parsedUserId.toString(),
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

    // 친구 목록 가져오기
    LaunchedEffect(parsedUserId) {
        if (parsedUserId > 0) {
            isFriendsLoading = true
            apiService.getFriendsList(parsedUserId.toString()).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    isFriendsLoading = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body["success"] == true) {
                            @Suppress("UNCHECKED_CAST")
                            val friends = body["friends"] as? List<Map<String, Any>> ?: emptyList()
                            friendsList = friends
                        }
                    }
                }
                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                    isFriendsLoading = false
                    errorLog = "친구 목록을 가져오는데 실패했습니다: ${t.message}"
                }
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "이메일: $userEmail",
                fontSize = 16.sp,
            )
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 1.dp)
            ) {
                Text("로그아웃", fontSize = 12.sp)
            }
        }
        
        // 네이버 지도
        NaverMapScreen(
            onLocationChanged = { lat, lng ->
                latitude = lat
                longitude = lng
            },
            onLocationError = { error ->
                locationError = error
            }
        )

        // 탭 선택 UI
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("친구 검색") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("친구 목록") }
            )
        }

        when (selectedTab) {
            0 -> {
                // 기존 검색 UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("이메일 또는 닉네임으로 검색") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            isSearching = true
                            apiService.searchUsers(
                                keyword = searchQuery,
                                userId = parsedUserId.toString()
                            ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                                override fun onResponse(
                                    call: retrofit2.Call<Map<String, Any>>,
                                    response: retrofit2.Response<Map<String, Any>>
                                ) {
                                    isSearching = false
                                    Log.d("UserSearch", "응답 코드: ${response.code()}")
                                    Log.d("UserSearch", "응답 바디: ${response.body()}")
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        if (body != null && body["success"] == true) {
                                            @Suppress("UNCHECKED_CAST")
                                            val users = body["users"] as? List<Map<String, Any>> ?: emptyList()
                                            Log.d("UserSearch", "검색된 사용자 수: ${users.size}")
                                            users.forEachIndexed { index, user ->
                                                Log.d("UserSearch", "사용자 $index: $user")
                                            }
                                            searchResults = users
                                        } else {
                                            val errorMsg = body?.get("error") as? String ?: "검색 실패"
                                            Log.e("UserSearch", "검색 실패: $errorMsg")
                                            errorLog = errorMsg
                                        }
                                    } else {
                                        val errorMsg = "검색 실패: ${response.code()}"
                                        Log.e("UserSearch", errorMsg)
                                        errorLog = errorMsg
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                                    isSearching = false
                                    val errorMsg = "네트워크 오류: ${t.message}"
                                    Log.e("UserSearch", errorMsg, t)
                                    errorLog = errorMsg
                                }
                            })
                        },
                        enabled = searchQuery.isNotEmpty() && !isSearching
                    ) {
                        Text("검색")
                    }
                }

                // 검색 결과 리스트
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { user ->
                            Log.d("UserData", "사용자 데이터: $user")  // 사용자 데이터 로깅
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = user["nickname"] as? String ?: "",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = user["email"] as? String ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            // user_id 대신 id 필드 확인
                                            Log.d("FriendAdd", "id 타입: ${user["id"]?.javaClass?.name}")
                                            val friendId = when (val id = user["id"]) {
                                                is Int -> id
                                                is Long -> id.toInt()
                                                is String -> id.toIntOrNull()
                                                else -> null
                                            }
                                            Log.d("FriendAdd", "사용자 데이터: $user")
                                            Log.d("FriendAdd", "추출된 friendId: $friendId")

                                            if (friendId != null) {
                                                Log.d("FriendAdd", "친구 추가 시도: userId=$parsedUserId, friendId=$friendId")
                                                apiService.addFriend(
                                                    userId = parsedUserId.toString(),
                                                    friendId = friendId.toString() // Int를 String으로 변환
                                                ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                                                    override fun onResponse(
                                                        call: retrofit2.Call<Map<String, Any>>,
                                                        response: retrofit2.Response<Map<String, Any>>
                                                    ) {
                                                        Log.d("FriendAdd", "응답 코드: ${response.code()}")
                                                        Log.d("FriendAdd", "응답 바디: ${response.body()}")
                                                        if (response.isSuccessful) {
                                                            val body = response.body()
                                                            Log.d("FriendAdd", "응답 데이터: $body")
                                                            if (body != null && body["success"] == true) {
                                                                // 친구 추가 성공 처리
                                                                searchResults = searchResults.filter { it != user }
                                                                successMessage = "${user["nickname"]}님이 친구로 추가되었습니다."
                                                                // 3초 후 성공 메시지 제거
                                                                MainScope().launch {
                                                                    kotlinx.coroutines.delay(3000)
                                                                    successMessage = ""
                                                                }
                                                            } else {
                                                                val errorMsg = body?.get("error") as? String ?: "친구 추가 실패"
                                                                Log.e("FriendAdd", "친구 추가 실패: $errorMsg")
                                                                errorLog = errorMsg
                                                            }
                                                        } else {
                                                            val errorMsg = "친구 추가 실패: ${response.code()}"
                                                            Log.e("FriendAdd", errorMsg)
                                                            errorLog = errorMsg
                                                        }
                                                    }
                                                    override fun onFailure(
                                                        call: retrofit2.Call<Map<String, Any>>,
                                                        t: Throwable
                                                    ) {
                                                        val errorMsg = "네트워크 오류: ${t.message}"
                                                        Log.e("FriendAdd", errorMsg, t)
                                                        errorLog = errorMsg
                                                    }
                                                })
                                            } else {
                                                Log.e("FriendAdd", "friendId 파싱 실패: id=${user["id"]}")
                                                errorLog = "사용자 ID를 찾을 수 없습니다."
                                            }
                                        }
                                    ) {
                                        Text("친구 추가")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // 친구 목록 UI
                if (isFriendsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (friendsList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(friendsList) { friend ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = friend["nickname"] as? String ?: "",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = friend["email"] as? String ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        Button(
                                            onClick = {
                                                // 친구 위치 확인 로직
                                                val friendId = friend["id"] as? Int
                                                if (friendId != null) {
                                                    apiService.getFriendsLocations(parsedUserId.toString()).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                                                        override fun onResponse(
                                                            call: retrofit2.Call<Map<String, Any>>,
                                                            response: retrofit2.Response<Map<String, Any>>
                                                        ) {
                                                            if (response.isSuccessful) {
                                                                val body = response.body()
                                                                if (body != null && body["success"] == true) {
                                                                    @Suppress("UNCHECKED_CAST")
                                                                    val locations = body["locations"] as? List<Map<String, Any>> ?: emptyList()
                                                                    val friendLocation = locations.find { it["user_id"] == friendId }
                                                                    if (friendLocation != null) {
                                                                        val lat = friendLocation["latitude"] as? Double
                                                                        val lng = friendLocation["longitude"] as? Double
                                                                        if (lat != null && lng != null) {
                                                                            // 지도에 친구 위치 표시
                                                                            // TODO: 지도 UI 구현
                                                                            successMessage = "${friend["nickname"]}님의 위치를 확인했습니다."
                                                                            MainScope().launch {
                                                                                kotlinx.coroutines.delay(3000)
                                                                                successMessage = ""
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                                                            errorLog = "위치 정보를 가져오는데 실패했습니다: ${t.message}"
                                                        }
                                                    })
                                                }
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("위치")
                                        }
                                        Button(
                                            onClick = {
                                                // 친구 삭제 로직
                                                Log.v("FriendDelete", "friend 데이터: $friend")
                                                val friendId = when (val id = friend["id"]) {
                                                    is Int -> id
                                                    is Long -> id.toInt()
                                                    is String -> id.toIntOrNull()
                                                    else -> null
                                                }
                                                Log.v("FriendDelete", "버튼클릭 friendId :  ${friendId}")
                                                if (friendId != null) {
                                                    apiService.deleteFriend(
                                                        userId = parsedUserId.toString(),
                                                        friendId = friendId.toString()
                                                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                                                        override fun onResponse(
                                                            call: retrofit2.Call<Map<String, Any>>,
                                                            response: retrofit2.Response<Map<String, Any>>
                                                        ) {
                                                            Log.v("FriendDelete", "응답 코드: ${response.code()}")
                                                            Log.v("FriendDelete", "응답 바디: ${response.body()}")
                                                            if (response.isSuccessful) {
                                                                val body = response.body()
                                                                if (body != null && body["success"] == true) {
                                                                    // 친구 목록에서 제거
                                                                    friendsList = friendsList.filter { it != friend }
                                                                    successMessage = "${friend["nickname"]}님을 친구 목록에서 삭제했습니다."
                                                                    MainScope().launch {
                                                                        kotlinx.coroutines.delay(3000)
                                                                        successMessage = ""
                                                                    }
                                                                } else {
                                                                    val errorMsg = body?.get("error") as? String ?: "친구 삭제 실패"
                                                                    Log.e("FriendDelete", "친구 삭제 실패: $errorMsg")
                                                                    errorLog = errorMsg
                                                                }
                                                            } else {
                                                                val errorMsg = "친구 삭제 실패: ${response.code()}"
                                                                Log.e("FriendDelete", errorMsg)
                                                                errorLog = errorMsg
                                                            }
                                                        }
                                                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                                                            val errorMsg = "네트워크 오류: ${t.message}"
                                                            Log.e("FriendDelete", errorMsg, t)
                                                            errorLog = errorMsg
                                                        }
                                                    })
                                                }
                                            }
                                        ) {
                                            Text("삭제")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "친구가 없습니다",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // 기존 에러 메시지와 성공 메시지 표시
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
        if (successMessage.isNotEmpty()) {
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
} 