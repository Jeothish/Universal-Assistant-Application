package com.example.myapplication
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "WikiCache")
data class WikiCache(
    @PrimaryKey val topic: String,
    val title: String,
    val summary: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val weatherRequest: String,
    val jsonResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)