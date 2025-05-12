package com.example.myposition

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myposition.ui.LoginScreen
import com.example.myposition.ui.MainScreen
import com.example.myposition.ui.theme.MyPositionTheme
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 정밀 위치 권한이 허용됨
                Log.d("Location", "Fine location permission granted")
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 대략적인 위치 권한이 허용됨
                Log.d("Location", "Coarse location permission granted")
            }
            else -> {
                // 권한이 거부됨
                Log.d("Location", "Location permission denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 위치 권한 요청
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        
        // 패키지명 로그 출력
        val packageName = packageName
        Log.d("APP_INFO", "Package Name: $packageName")
        
        // 카카오 SDK 초기화
        KakaoSdk.init(this, "4a2137f151bca1f758c598c726fbf78b")
        
        // 키해시 로그 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KAKAO_KEY_HASH", "keyHash: $keyHash")
        
        enableEdgeToEdge()
        
        setContent {
            var isLoggedIn by remember { mutableStateOf(false) }
            var userEmail by remember { mutableStateOf("") }
            var userNickname by remember { mutableStateOf("") }
            var userPassword by remember { mutableStateOf("") }
            var autoLoginChecked by remember { mutableStateOf(false) }
            var userId by remember { mutableStateOf(-1) }
            var userInfo by remember { mutableStateOf("") }
            var userJson by remember { mutableStateOf("") }
            val context = this
            val apiService = com.example.myposition.api.RetrofitClient.apiService
            
            // 자동로그인 체크 (최초 1회)
            if (!autoLoginChecked) {
                autoLoginChecked = true
                UserApiClient.instance.me { user, error ->
                    if (user != null) {
                        userEmail = user.kakaoAccount?.email ?: ""
                        userNickname = user.kakaoAccount?.profile?.nickname ?: userEmail.substringBefore("@")
                        userPassword = "kakao"
                        val kakaoUid = user.id?.toString() ?: ""
                        // Send user information to the server
                        apiService.registerUser(userEmail, userPassword, userNickname, kakaoUid).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                            override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    Log.d("USER_INFO", "서버 응답 body: $body")
                                    userId = body?.get("user_id") as? Int ?: -1
                                    Log.d("USER_INFO", "userId from server: $userId")
                                    userInfo = "ID: $userId, Email: $userEmail, Nickname: $userNickname"
                                    userJson = body.toString()
                                    Log.d("USER_INFO", userInfo)
                                    isLoggedIn = true
                                } else {
                                    Log.e("USER_INFO", "Failed to register user: ${response.code()}")
                                }
                            }
                            override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                                Log.e("USER_INFO", "Network error: ${t.message}")
                            }
                        })
                    }
                }
            }
            
            MyPositionTheme {
                if (isLoggedIn) {
                    MainScreen(
                        userEmail = userEmail,
                        userNickname = userNickname,
                        userPassword = userPassword,
                        onLogout = {
                            // 카카오 로그아웃
                            UserApiClient.instance.logout { error ->
                                if (error != null) {
                                    Log.e("Logout", "로그아웃 실패: ${error.message}")
                                } else {
                                    isLoggedIn = false
                                    userEmail = ""
                                    userNickname = ""
                                    userPassword = ""
                                    userId = -1
                                    userInfo = ""
                                    userJson = ""
                                    Log.d("USER_INFO", "Logged out, userId reset to: $userId")
                                }
                            }
                        },
                        userInfo = userInfo,
                        userId = userId,
                        userJson = userJson
                    )
                } else {
                    LoginScreen(
                        onLoginClick = { email, password, id ->
                            if (email.isNotEmpty()) {
                                userEmail = email
                                userNickname = email.substringBefore("@")
                                userPassword = password
                                userId = id
                                userInfo = "Email: $email, Nickname: ${email.substringBefore("@")}" 
                                Log.d("USER_INFO", "Login click, userId set to: $userId")
                                isLoggedIn = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyPositionTheme {
        Greeting("Android")
    }
}