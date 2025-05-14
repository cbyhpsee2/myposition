package com.example.myposition

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var job: Job? = null
    private var userId: Int = -1
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastSentTime: Long = 0L
    private var isMoving = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra("user_id", -1) ?: -1
        job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val delayMillis = if (isMoving) 10_000L else 30_000L // 10초 or 30초
                sendLocation()
                delay(delayMillis)
            }
        }
        return START_STICKY
    }

    private fun sendLocation() {
        if (userId <= 0) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val now = System.currentTimeMillis()
                val lat = location.latitude
                val lng = location.longitude
                val moved = lastLat == null || lastLng == null ||
                    Math.abs(lat - lastLat!!) > 0.0001 || Math.abs(lng - lastLng!!) > 0.0001

                if (moved) {
                    isMoving = true
                    lastLat = lat
                    lastLng = lng
                    lastSentTime = now
                    sendToServer(lat, lng)
                } else if (now - lastSentTime > 300_000) {
                    isMoving = false
                    lastSentTime = now
                    sendToServer(lat, lng)
                }
            }
        }
    }

    private fun sendToServer(lat: Double, lng: Double) {
        com.example.myposition.api.RetrofitClient.apiService.sendLocation(
            gid = userId,
            latitude = lat,
            longitude = lng
        ).enqueue(object : retrofit2.Callback<Map<String, Any>> {
            override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                if (!response.isSuccessful) {
                    Log.e("LocationService", "Server error: ${response.code()}")
                } else {
                    Log.d("LocationService", "Location sent successfully")
                }
            }
            override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                Log.e("LocationService", "Network error: ${t.message}", t)
            }
        })
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "위치 추적",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, "location_channel")
            .setContentTitle("위치 추적 중")
            .setContentText("친구 위치 공유 서비스가 실행 중입니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }
} 