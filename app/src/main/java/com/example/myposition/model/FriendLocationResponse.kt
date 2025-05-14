package com.example.myposition.model

import com.google.gson.annotations.SerializedName

// 친구 위치 목록 응답 (프론트-백엔드 통신용)
data class FriendLocationResponse(
    val success: Boolean,
    @SerializedName("locations") val locations: List<FriendLocation>
)

data class FriendLocationHistoryResponse(
    val success: Boolean,
    @SerializedName("locations") val locations: List<FriendLocation>
) 