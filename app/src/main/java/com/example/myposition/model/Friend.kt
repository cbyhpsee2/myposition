package com.example.myposition.model

import com.google.gson.annotations.SerializedName

// mp_users 테이블의 사용자 정보
// gid: PK, nickname, email

data class Friend(
    val gid: Int,
    val nickname: String,
    val email: String,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
) 