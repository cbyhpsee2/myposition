package com.example.myposition.model

import com.google.gson.annotations.SerializedName

// 친구 위치 정보 (mp_users.gid, mp_locations)
data class FriendLocation(
    val gid: Int,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
) 