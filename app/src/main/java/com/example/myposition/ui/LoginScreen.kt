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
import com.kakao.sdk.user.UserApiClient
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.myposition.api.ApiService
import com.example.myposition.api.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(onLoginClick: (String, String) -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.apiService }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // 로고 영역
        Text(
            text = "SHOP",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        // 환영 메시지
        Text(
            text = "환영합니다!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "로그인하고 쇼핑을 시작하세요",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 이메일 입력
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // 비밀번호 찾기 링크
        TextButton(
            onClick = { /* TODO: 비밀번호 찾기 구현 */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("비밀번호를 잊으셨나요?", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // 로그인 버튼
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "이메일과 비밀번호를 입력해주세요"
                    return@Button
                }
                isLoading = true
                errorMessage = null

                // 서버에 사용자 등록
                apiService.registerUser(email, password, email.split("@")[0]).enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            if (responseBody?.get("success") == true) {
                                val userId = responseBody["user_id"] as? Int
                                if (userId != null) {
                                    Log.d("LOGIN", "사용자 등록 성공: $userId")
                                    onLoginClick(email, password)
                                } else {
                                    errorMessage = "사용자 ID를 받지 못했습니다"
                                }
                            } else {
                                errorMessage = responseBody?.get("error") as? String ?: "로그인 실패"
                            }
                        } else {
                            errorMessage = "서버 오류: ${response.code()}"
                        }
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        isLoading = false
                        errorMessage = "네트워크 오류: ${t.message}"
                        Log.e("LOGIN", "API 호출 실패", t)
                    }
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("로그인", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 소셜 로그인
        Text(
            text = "또는",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        // 먼저 현재 동의된 스코프 확인
                        UserApiClient.instance.me { user, error ->
                            if (error != null) {
                                // 사용자 정보가 없는 경우 (로그인이 필요한 경우)
                                // 카카오톡 앱 로그인 우선 시도
                                UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                                    Log.d("KAKAO_LOGIN", "loginWithKakaoTalk: token=$token, error=$error")
                                    if (error != null) {
                                        // 카카오톡 로그인 실패 시 카카오 계정(웹) 로그인 시도
                                        UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                                            Log.d("KAKAO_LOGIN", "loginWithKakaoAccount: token=$token, error=$error")
                                            if (error != null) {
                                                Toast.makeText(context, "카카오 로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                            } else if (token != null) {
                                                // 로그인 성공 시 추가 동의 요청
                                                UserApiClient.instance.loginWithNewScopes(activity, listOf("account_email", "profile")) { token, error ->
                                                    if (error != null) {
                                                        Toast.makeText(context, "추가 동의 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    } else if (token != null) {
                                                        // 사용자 정보 요청
                                                        UserApiClient.instance.me { user, error ->
                                                            if (error != null) {
                                                                Toast.makeText(context, "사용자 정보 요청 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                            } else if (user != null) {
                                                                val email = user.kakaoAccount?.email ?: ""
                                                                if (email.isNotEmpty()) {
                                                                    onLoginClick(email, "")
                                                                    Toast.makeText(context, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    Toast.makeText(context, "이메일 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (token != null) {
                                        // 카카오톡 로그인 성공 시 추가 동의 요청
                                        UserApiClient.instance.loginWithNewScopes(activity, listOf("account_email", "profile")) { token, error ->
                                            if (error != null) {
                                                Toast.makeText(context, "추가 동의 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                            } else if (token != null) {
                                                // 사용자 정보 요청
                                                UserApiClient.instance.me { user, error ->
                                                    if (error != null) {
                                                        Toast.makeText(context, "사용자 정보 요청 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                    } else if (user != null) {
                                                        val email = user.kakaoAccount?.email ?: ""
                                                        if (email.isNotEmpty()) {
                                                            onLoginClick(email, "")
                                                            Toast.makeText(context, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "이메일 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (user != null) {
                                // 이미 로그인된 상태인 경우
                                val email = user.kakaoAccount?.email ?: ""
                                if (email.isNotEmpty()) {
                                    onLoginClick(email, "")
                                    Toast.makeText(context, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 이메일 정보가 없는 경우 추가 동의 요청
                                    UserApiClient.instance.loginWithNewScopes(activity, listOf("account_email", "profile")) { token, error ->
                                        if (error != null) {
                                            Toast.makeText(context, "추가 동의 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                        } else if (token != null) {
                                            // 사용자 정보 재요청
                                            UserApiClient.instance.me { user, error ->
                                                if (error != null) {
                                                    Toast.makeText(context, "사용자 정보 요청 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                                                } else if (user != null) {
                                                    val newEmail = user.kakaoAccount?.email ?: ""
                                                    if (newEmail.isNotEmpty()) {
                                                        onLoginClick(newEmail, "")
                                                        Toast.makeText(context, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "이메일 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("카카오", color = Color(0xFFFEE500))
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(
                onClick = { /* TODO: 네이버 로그인 구현 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("네이버", color = Color(0xFF03C75A))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 회원가입 링크
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("계정이 없으신가요? ", color = Color.Gray)
            TextButton(
                onClick = { /* TODO: 회원가입 화면으로 이동 */ }
            ) {
                Text("회원가입", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
} 