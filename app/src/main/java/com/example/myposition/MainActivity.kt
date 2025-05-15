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
import com.example.myposition.util.*

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
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 위치 권한 요청
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        
        // 패키지명 로그 출력
        val packageName = packageName
        Log.d("APP_INFO", "Package Name: $packageName")
        
        // 카카오 SDK 초기화
         KakaoSdk.init(applicationContext, "4a2137f151bca1f758c598c726fbf78b")
        
        // 키해시 로그 출력
        // val keyHash = Utility.getKeyHash(this)
        // Log.d("KAKAO_KEY_HASH", "keyHash: $keyHash")
        
        enableEdgeToEdge()
        
        // 자동로그인 정보 불러오기
        val (savedEmail, savedNickname, savedUserId, savedProfileImageUrl) = getLoginInfo(this)

        setContent {
            var isLoggedIn by remember { mutableStateOf(savedUserId > 0) }
            var userEmail by remember { mutableStateOf(savedEmail ?: "") }
            var userNickname by remember { mutableStateOf(savedNickname ?: "") }
            var userId by remember { mutableStateOf(savedUserId) }
            var userProfileImageUrl by remember { mutableStateOf(savedProfileImageUrl ?: "") }
            MyPositionTheme {
                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = { email, nickname, id, profileImageUrl ->
                        userEmail = email
                        userNickname = nickname
                        userId = id
                        userProfileImageUrl = profileImageUrl ?: ""
                        isLoggedIn = true
                    })
                } else {
                    MainScreen(
                        userEmail = userEmail,
                        userNickname = userNickname,
                        userProfileImageUrl = userProfileImageUrl,
                        userPassword = "",
                        onLogout = {
                            isLoggedIn = false
                            userEmail = ""
                            userNickname = ""
                            userId = -1
                            userProfileImageUrl = ""
                            clearLoginInfo(this@MainActivity)
                        },
                        userInfo = "",
                        userId = userId,
                        userJson = ""
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