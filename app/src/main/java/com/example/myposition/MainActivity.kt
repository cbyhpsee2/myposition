package com.example.myposition

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myposition.ui.LoginScreen
import com.example.myposition.ui.MainScreen
import com.example.myposition.ui.theme.MyApplicationTheme
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
            var autoLoginChecked by remember { mutableStateOf(false) }
            
            // 자동로그인 체크 (최초 1회)
            if (!autoLoginChecked) {
                autoLoginChecked = true
                UserApiClient.instance.me { user, error ->
                    if (user != null) {
                        userEmail = user.kakaoAccount?.email ?: ""
                        isLoggedIn = true
                    }
                }
            }
            
            MyApplicationTheme {
                if (isLoggedIn) {
                    MainScreen(
                        userEmail = userEmail,
                        onLogout = {
                            // 카카오 로그아웃
                            UserApiClient.instance.logout { error ->
                                if (error != null) {
                                    Log.e("Logout", "로그아웃 실패: ${error.message}")
                                } else {
                                    isLoggedIn = false
                                    userEmail = ""
                                }
                            }
                        }
                    )
                } else {
                    LoginScreen(
                        onLoginClick = { email, _ ->
                            if (email.isNotEmpty()) {
                                userEmail = email
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
    MyApplicationTheme {
        Greeting("Android")
    }
}