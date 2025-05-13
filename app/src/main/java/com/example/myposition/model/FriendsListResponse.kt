package com.example.myposition.model

// 친구 목록 응답 (프론트-백엔드 통신용)
data class FriendsListResponse(
    val success: Boolean,
    val friends: List<Friend>?
) 