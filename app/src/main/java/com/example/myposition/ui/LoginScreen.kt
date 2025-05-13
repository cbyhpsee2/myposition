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

@Composable
fun LoginScreen(onLoginSuccess: (String, String, Int) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
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
                                        // 서버에 email, nickname 보내서 user_id 받아오기
                                        coroutineScope.launch {
                                            try {
                                                val api = RetrofitClient.apiService
                                                Log.e("RegisterUser", "registerUser 호출 시작: email=$email, nickname=$nickname")
                                                println("registerUser 호출 시작: email=$email, nickname=$nickname")
                                                val response = withContext(Dispatchers.IO) {
                                                    api.registerUser(email, "", nickname, null).execute()
                                                }
                                                val body = response.body()
                                                Log.e("RegisterUser", "registerUser 응답: $body")
                                                println("registerUser 응답: $body")
                                                val userId = if (response.isSuccessful && body != null && body["success"] == true) {
                                                    (body["gid"] as? Number)?.toInt() ?: (body["gid"] as? String)?.toIntOrNull() ?: -1
                                                } else {
                                                    -1
                                                }
                                                onLoginSuccess(email, nickname, userId)
                                            } catch (e: Exception) {
                                                Log.e("RegisterUser", "registerUser 예외: ${e.message}", e)
                                                println("registerUser 예외: ${e.message}")
                                                errorMsg = "user_id 받아오기 실패: ${e.message}"
                                                onLoginSuccess(email, nickname, -1)
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
                                // 서버에 email, nickname 보내서 user_id 받아오기
                                coroutineScope.launch {
                                    try {
                                        val api = RetrofitClient.apiService
                                        Log.e("RegisterUser", "registerUser 호출 시작: email=$email, nickname=$nickname")
                                        println("registerUser 호출 시작: email=$email, nickname=$nickname")
                                        val response = withContext(Dispatchers.IO) {
                                            api.registerUser(email, "", nickname, null).execute()
                                        }
                                        val body = response.body()
                                        Log.e("RegisterUser", "registerUser 응답: $body")
                                        println("registerUser 응답: $body")
                                        val userId = if (response.isSuccessful && body != null && body["success"] == true) {
                                            (body["gid"] as? Number)?.toInt() ?: (body["gid"] as? String)?.toIntOrNull() ?: -1
                                        } else {
                                            -1
                                        }
                                        onLoginSuccess(email, nickname, userId)
                                    } catch (e: Exception) {
                                        Log.e("RegisterUser", "registerUser 예외: ${e.message}", e)
                                        println("registerUser 예외: ${e.message}")
                                        errorMsg = "user_id 받아오기 실패: ${e.message}"
                                        onLoginSuccess(email, nickname, -1)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading
        ) {
            Text("카카오로 로그인", color = Color(0xFFFEE500), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = Color.Red, modifier = Modifier.padding(8.dp))
        }
    }
} 