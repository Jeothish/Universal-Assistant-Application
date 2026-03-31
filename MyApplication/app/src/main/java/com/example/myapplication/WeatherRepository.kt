package com.example.myapplication

import android.util.Log
import com.chaquo.python.Python
import com.google.gson.Gson
import org.json.JSONObject

class WeatherRepository(private val weatherDao: WeatherDao) {

    suspend fun getWeatherData(prompt:String,default: String): String {
        val cacheExpiry = 30L * 60 * 1000
        weatherDao.deleteOldCache(cacheExpiry)

        val py = Python.getInstance().getModule("intent")
        val json = JSONObject(py.callAttr("getWeather", prompt, default).toString())
        val city = json.getString("city")
        val daysAhead = json.getInt("days_ahead")
        val hour = if (json.isNull("hour")) null else json.getInt("hour")

        val hourKey = hour?.toString() ?: "all"
        val weatherRequest  = "${city.lowercase()}_${daysAhead}_$hourKey"

        val cached = weatherDao.getWeather(weatherRequest)

        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheExpiry) {
            Log.d("WEATHER_CACHING", "Using database data for $weatherRequest")
            return cached.jsonResponse
        }

        Log.d("WEATHER_CACHING", "Fetching from API for $weatherRequest")
        val jsonString = json.toString()
        weatherDao.insertWeather(WeatherCache(weatherRequest, jsonString, System.currentTimeMillis()))
        return jsonString
    }
}
