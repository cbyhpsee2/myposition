package com.example.myposition.util

import android.content.Context

fun saveLoginInfo(context: Context, email: String, nickname: String, userId: Int, profileImageUrl: String?) {
    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("email", email)
        .putString("nickname", nickname)
        .putInt("userId", userId)
        .putString("profileImageUrl", profileImageUrl ?: "")
        .apply()
}

fun getLoginInfo(context: Context): Quad<String?, String?, Int, String?> {
    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    return Quad(
        prefs.getString("email", null),
        prefs.getString("nickname", null),
        prefs.getInt("userId", -1),
        prefs.getString("profileImageUrl", null)
    )
}

fun clearLoginInfo(context: Context) {
    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}

fun saveLimit(context: Context, limit: Int) {
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("limit", limit).apply()
}

fun getLimit(context: Context): Int {
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("limit", 30)
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D) 