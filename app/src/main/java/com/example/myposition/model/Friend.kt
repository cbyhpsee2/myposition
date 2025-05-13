package com.example.myposition.model

// mp_users 테이블의 사용자 정보
// gid: PK, nickname, email

data class Friend(
    val gid: Int,
    val nickname: String,
    val email: String
) 