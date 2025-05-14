package com.example.myposition.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myposition.R
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.myposition.api.ApiService
import com.example.myposition.api.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.kakao.sdk.user.UserApiClient
import com.example.myposition.model.Friend
import com.example.myposition.model.FriendsListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background

@Composable
fun LoginScreen(onLoginSuccess: (String, String, Int, String?) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    // 상태바 배경색을 흰색으로 지정
    SideEffect {
        val activity = context as? Activity
        val window = activity?.window
        window?.statusBarColor = android.graphics.Color.WHITE
        window?.let {
            WindowCompat.getInsetsController(it, view)?.isAppearanceLightStatusBars = true
        }
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
        Image(
            painter = painterResource(id = R.drawable.bg_myposition),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Image(
            painter = painterResource(id = R.drawable.kakao_login_large_narrow),
            contentDescription = "카카오 로그인",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 166.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clickable(enabled = !isLoading) {
                    Log.d("LoginScreen", "onClick 호출")
                    val activity = context as? Activity
                    if (activity != null) {
                        isLoading = true
                        UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                            if (error != null) {
                                UserApiClient.instance.loginWithKakaoAccount(activity) { token2, error2 ->
                                    if (error2 == null && token2 != null) {
                                        UserApiClient.instance.me { user, error3 ->
                                            val email = user?.kakaoAccount?.email ?: ""
                                            val nickname = user?.kakaoAccount?.profile?.nickname ?: email.substringBefore("@")
                                            val uid = user?.id?.toString() ?: ""
                                            val profileImageUrl = user?.kakaoAccount?.profile?.profileImageUrl ?: ""
                                            // 서버에 email, nickname, uid, profileImageUrl 보내서 user_id 받아오기
                                            coroutineScope.launch {
                                                try {
                                                    val api = RetrofitClient.apiService
                                                    Log.e("RegisterUser", "registerUser 호출 시작: email=$email, nickname=$nickname, uid=$uid, profileImageUrl=$profileImageUrl")
                                                    println("registerUser 호출 시작: email=$email, nickname=$nickname, uid=$uid, profileImageUrl=$profileImageUrl")
                                                    val response = withContext(Dispatchers.IO) {
                                                        api.registerUser(email, "kakao", nickname, uid, profileImageUrl).execute()
                                                    }
                                                    val body = response.body()
                                                    Log.e("RegisterUser", "registerUser 응답: $body")
                                                    println("registerUser 응답: $body")
                                                    val userId = if (response.isSuccessful && body != null && body["success"] == true) {
                                                        (body["gid"] as? Number)?.toInt() ?: (body["gid"] as? String)?.toIntOrNull() ?: -1
                                                    } else {
                                                        -1
                                                    }
                                                    val profileImageUrlFromServer = body?.get("profile_image_url") as? String ?: profileImageUrl
                                                    onLoginSuccess(email, nickname, userId, profileImageUrlFromServer)
                                                } catch (e: Exception) {
                                                    Log.e("RegisterUser", "registerUser 예외: ${e.message}", e)
                                                    println("registerUser 예외: ${e.message}")
                                                    errorMsg = "user_id 받아오기 실패: ${e.message}"
                                                    onLoginSuccess(email, nickname, -1, null)
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (token != null) {
                                UserApiClient.instance.me { user, error3 ->
                                    val email = user?.kakaoAccount?.email ?: ""
                                    val nickname = user?.kakaoAccount?.profile?.nickname ?: email.substringBefore("@")
                                    val uid = user?.id?.toString() ?: ""
                                    val profileImageUrl = user?.kakaoAccount?.profile?.profileImageUrl ?: ""
                                    // 서버에 email, nickname, uid, profileImageUrl 보내서 user_id 받아오기
                                    coroutineScope.launch {
                                        try {
                                            val api = RetrofitClient.apiService
                                            Log.e("RegisterUser", "registerUser 호출 시작: email=$email, nickname=$nickname, uid=$uid, profileImageUrl=$profileImageUrl")
                                            println("registerUser 호출 시작: email=$email, nickname=$nickname, uid=$uid, profileImageUrl=$profileImageUrl")
                                            val response = withContext(Dispatchers.IO) {
                                                api.registerUser(email, "kakao", nickname, uid, profileImageUrl).execute()
                                            }
                                            val body = response.body()
                                            Log.e("RegisterUser", "registerUser 응답: $body")
                                            println("registerUser 응답: $body")
                                            val userId = if (response.isSuccessful && body != null && body["success"] == true) {
                                                (body["gid"] as? Number)?.toInt() ?: (body["gid"] as? String)?.toIntOrNull() ?: -1
                                            } else {
                                                -1
                                            }
                                            val profileImageUrlFromServer = body?.get("profile_image_url") as? String ?: profileImageUrl
                                            onLoginSuccess(email, nickname, userId, profileImageUrlFromServer)
                                        } catch (e: Exception) {
                                            Log.e("RegisterUser", "registerUser 예외: ${e.message}", e)
                                            println("registerUser 예외: ${e.message}")
                                            errorMsg = "user_id 받아오기 실패: ${e.message}"
                                            onLoginSuccess(email, nickname, -1, null)
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = Color.Red, modifier = Modifier.padding(8.dp))
            }
        }
    }
} 