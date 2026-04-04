package com.example.myapplication

import android.util.Log
import com.chaquo.python.Python
import com.google.gson.Gson
import org.json.JSONObject

/**
 * Repository handling weather data retrieval and caching
 */
class WeatherRepository(private val weatherDao: WeatherDao) {


    /**
     * Retrieves weather data for a given prompt, using cache if available.
     *
     * @param prompt Query for weather
     * @param default Default city or parameters if parsing fails
     * @return JSON string with weather data
     */
    suspend fun getWeatherData(prompt:String,default: String): String {
        val cacheExpiry = 30L * 60 * 1000
        weatherDao.deleteOldCache(cacheExpiry)

        val py = Python.getInstance().getModule("intent")
        val params = JSONObject(py.callAttr("get_weather_params", prompt, default).toString())

        val city = params.getString("city")
        val daysAhead = params.getInt("days_ahead")
        val hour = if (params.isNull("hour")) null else params.getInt("hour")

        val hourKey = hour?.toString() ?: "all"
        val weatherRequest  = "${city.lowercase()}_${daysAhead}_$hourKey"

        val cached = weatherDao.getWeather(weatherRequest)

        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheExpiry) {
            Log.d("WEATHER_CACHING", "Using database data for $weatherRequest")
            return cached.jsonResponse
        }

        Log.d("WEATHER_CACHING", "Fetching from API for $weatherRequest")
        val json = JSONObject(py.callAttr("getWeather", prompt, default).toString())
        val jsonString = json.toString()
        weatherDao.insertWeather(WeatherCache(weatherRequest, jsonString, System.currentTimeMillis()))
        return jsonString
    }
}
