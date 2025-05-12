package com.example.myposition.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("register_user")
    fun registerUser(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("nickname") nickname: String
    ): Call<Map<String, Any>>
} 