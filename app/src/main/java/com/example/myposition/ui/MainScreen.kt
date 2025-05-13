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
            .fillMaxWidth()
            .height(300.dp)
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
        Log.d("NAVER_MAP", "LaunchedEffect(selectedLatLngTriple): $selectedLatLngTriple, map=$map")
        if (map == null) {
            Log.e("NAVER_MAP", "지도 객체(map)가 아직 초기화되지 않았음")
        }
        selectedLatLngTriple?.let { (lat, lng, _) ->
            map?.moveCamera(CameraUpdate.scrollTo(LatLng(lat, lng)))
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

    // 친구 목록 로드 함수
    val loadFriendsList = {
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
            userNickname = userNickname,
            friendLocations = friendLocations,
            selectedLatLngTriple = selectedLatLngTriple,
            onLocationChanged = { lat, lng ->
                latitude = lat
                longitude = lng
            },
            onLocationError = { error ->
                locationError = error
            }
        )

        // 탭: 친구검색 / 친구목록
        Spacer(modifier = Modifier.height(16.dp))
        val tabs = listOf("친구검색", "친구목록")
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                ) { Text(text = title) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 친구 검색 UI (탭 0)
        if (selectedTabIndex == 0) {
            // 친구 검색 UI
            Spacer(modifier = Modifier.height(16.dp))
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
                            gid = gid
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
                                        Log.d("FriendAdd", "gid 타입: ${user["gid"]?.javaClass?.name}")
                                        val friendGid = when (val id = user["gid"]) {
                                            is Int -> id
                                            is Long -> id.toInt()
                                            is String -> id.toIntOrNull()
                                            else -> null
                                        }
                                        Log.d("FriendAdd", "사용자 데이터: $user")
                                        Log.d("FriendAdd", "추출된 friendGid: $friendGid")

                                        if (friendGid != null) {
                                            Log.d("FriendAdd", "친구 추가 시도: gid=$gid, friendGid=$friendGid")
                                            apiService.addFriend(
                                                gid = gid,
                                                friendGid = friendGid
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
                                                            // 친구 추가 성공 처리 및 목록 갱신
                                                            searchResults = searchResults.filter { it != user }
                                                            loadFriendsList()
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
                                            Log.e("FriendAdd", "friendGid 파싱 실패: gid=${user["gid"]}")
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

        // 친구 목록 초기 로드
        LaunchedEffect(gid) {
            if (gid > 0) {
                loadFriendsList()
            }
        }

        // 친구 위치 로드
        LaunchedEffect(gid) {
            if (gid > 0) {
                isFriendLocationsLoading = true
                apiService.getFriendsLocations(gid).enqueue(object : retrofit2.Callback<com.example.myposition.model.FriendLocationResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myposition.model.FriendLocationResponse>,
                        response: retrofit2.Response<com.example.myposition.model.FriendLocationResponse>
                    ) {
                        isFriendLocationsLoading = false
                        val locBody = response.body()
                        Log.d("FriendLoc", "getFriendsLocations 응답 body: $locBody")
                        if (response.isSuccessful && locBody != null && locBody.success) {
                            // 데이터 클래스 기반 파싱
                            friendLocations = locBody.locations.map { loc ->
                                mapOf(
                                    "gid" to loc.gid,
                                    "nickname" to loc.nickname,
                                    "latitude" to loc.latitude,
                                    "longitude" to loc.longitude
                                )
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
            }
        }

        // 친구 목록 UI (탭 1)
        if (selectedTabIndex == 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "친구 목록", fontWeight = FontWeight.Bold)
            if (isFriendsLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (friendList.isEmpty()) {
                Text(text = "등록된 친구가 없습니다.", modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(friendList) { friend ->
                        val fid = friend.gid
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp),
                             elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    // 친구 이름 및 위치 버튼
                                    Log.d("FriendLoc", "Loaded friendLocations: $friendLocations")
                                    val currentLoc = friendLocations.firstOrNull { loc ->
                                        val idVal = when {
                                            loc["gid"] is Number -> (loc["gid"] as Number).toInt()
                                            loc["user_gid"] is Number -> (loc["user_gid"] as Number).toInt()
                                            else -> -1
                                        }
                                        idVal == fid
                                    }
                                    if (currentLoc == null) {
                                        Log.e("FriendLoc", "친구 위치를 찾을 수 없습니다. friendGid=$fid")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = friend.nickname,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (currentLoc != null) {
                                            val flat = (currentLoc["latitude"] as? Number)?.toDouble() ?: 0.0
                                            val flng = (currentLoc["longitude"] as? Number)?.toDouble() ?: 0.0
                                            TextButton(onClick = {
                                                Log.v("FriendLoc", "친구위치버튼 클릭. friendGid=$fid")
                                                Log.d("FriendLoc", "위치 이동 flat=$flat, flng=$flng")
                                                selectedLatLngTriple = Triple(flat, flng, System.currentTimeMillis())
                                            }) {
                                                Text("위치", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                Button(onClick = {
                                    apiService.deleteFriend(
                                        gid = gid,
                                        friendGid = fid
                                    ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                                        override fun onResponse(
                                            call: retrofit2.Call<Map<String, Any>>, 
                                            response: retrofit2.Response<Map<String, Any>>
                                        ) {
                                            if (response.isSuccessful && response.body()?.get("success")==true) {
                                                loadFriendsList()
                                                friendList = friendList.filter { it.gid != fid }
                                                successMessage = "친구가 삭제되었습니다."
                                                MainScope().launch {
                                                    delay(3000)
                                                    successMessage = ""
                                                }
                                            } else {
                                                Log.e("FriendDelete","삭제 실패: ${response.body()?.get("error")}")
                                            }
                                        }
                                        override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                                            Log.e("FriendDelete","네트워크 오류: ${t.message}", t)
                                        }
                                    })
                                }) {
                                    Text("삭제")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { selectedEmail = friend.email }) {
                                    Text("@")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 이메일 AlertDialog
        if (selectedEmail != null) {
            AlertDialog(
                onDismissRequest = { selectedEmail = null },
                text = { Text(text = selectedEmail ?: "") },
                confirmButton = {
                    TextButton(onClick = { selectedEmail = null }) {
                        Text("확인")
                    }
                }
            )
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
        if (successMessage.isNotEmpty()) {
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
} 