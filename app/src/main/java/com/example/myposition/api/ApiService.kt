package com.example.myposition.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @FormUrlEncoded
    @POST("register_user")
    fun registerUser(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("nickname") nickname: String,
        @Field("uid") uid: String?
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("upload_location")
    fun sendLocation(
        @Field("user_id") gid: Int,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double
    ): Call<Map<String, Any>>

    @GET("friends_list/{user_id}")
    fun getFriendsList(
        @Path("user_id") gid: Int
    ): Call<com.example.myposition.model.FriendsListResponse>

    @GET("friends_locations/{user_id}")
    fun getFriendsLocations(
        @Path("user_id") gid: Int
    ): Call<com.example.myposition.model.FriendLocationResponse>

    @FormUrlEncoded
    @POST("search_users")
    fun searchUsers(
        @Field("keyword") keyword: String,
        @Field("user_id") gid: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("add_friend")
    fun addFriend(
        @Field("user_id") gid: Int,
        @Field("friend_id") friendGid: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("delete_friend")
    fun deleteFriend(
        @Field("user_id") gid: Int,
        @Field("friend_id") friendGid: Int
    ): Call<Map<String, Any>>
} 