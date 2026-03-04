package com.example.myapplication


import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object WeatherApi {

    private val WEATHER_CODES = mapOf(
        0 to "Clear sky", 1 to "Mainly clear", 2 to "Partly cloudy",
        3 to "Overcast", 45 to "Fog", 48 to "Depositing rime fog",
        51 to "Light drizzle", 53 to "Moderate drizzle", 55 to "Dense drizzle",
        61 to "Slight rain", 63 to "Moderate rain", 65 to "Heavy rain",
        71 to "Slight snow fall", 73 to "Moderate snow fall", 75 to "Heavy snow fall",
        80 to "Slight rain showers", 81 to "Moderate rain showers", 82 to "Violent rain showers",
        95 to "Thunderstorm (slight/moderate)", 96 to "Thunderstorm with slight hail",
        99 to "Thunderstorm with heavy hail"
    )

    suspend fun getCoordinates(cityName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$cityName"
        val response = JSONObject(URL(url).readText())
        val results = response.optJSONArray("results") ?: return@withContext null
        if (results.length() == 0) return@withContext null
        val first = results.getJSONObject(0)
        Pair(first.getDouble("latitude"), first.getDouble("longitude"))
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): Map<String, Any>? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto"
        val data = JSONObject(URL(url).readText())
        val weather = data.optJSONObject("current_weather") ?: return@withContext null

        val rawTime = weather.getString("time")
        val localTime = LocalDateTime.parse(rawTime)
        val formatted = localTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))

        mapOf(
            "Temperature (°C)"   to weather.getDouble("temperature"),
            "Wind Speed (km/h)"  to weather.getDouble("windspeed"),
            "Wind Direction (°)" to weather.getDouble("winddirection"),
            "Weather Condition"  to (WEATHER_CODES[weather.getInt("weathercode")] ?: "Unknown"),
            "Local Time"         to formatted
        )
    }

    suspend fun getForecastSpecificTime(lat: Double, lon: Double, target: LocalDateTime): Map<String, Any> = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m,weathercode,windspeed_10m,winddirection_10m&timezone=auto"
        val data = JSONObject(URL(url).readText())
        val hourly = data.getJSONObject("hourly")
        val times  = hourly.getJSONArray("time")
        val targetStr = target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))

        for (i in 0 until times.length()) {
            if (times.getString(i) == targetStr) {
                return@withContext mapOf(
                    "Temperature (°C)"   to hourly.getJSONArray("temperature_2m").getDouble(i),
                    "Wind Speed (km/h)"  to hourly.getJSONArray("windspeed_10m").getDouble(i),
                    "Wind Direction (°)" to hourly.getJSONArray("winddirection_10m").getDouble(i),
                    "Weather Condition"  to (WEATHER_CODES[hourly.getJSONArray("weathercode").getInt(i)] ?: "Unknown"),
                    "Local Time"         to target.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))
                )
            }
        }
        mapOf("error" to "Forecast for this hour not available")
    }

    suspend fun getForecastDay(lat: Double, lon: Double, targetDay: LocalDateTime): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&hourly=temperature_2m,weathercode,windspeed_10m,winddirection_10m&timezone=auto"
        val data   = JSONObject(URL(url).readText())
        val hourly = data.getJSONObject("hourly")
        val times  = hourly.getJSONArray("time")
        val dayStr = targetDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val result = mutableListOf<Map<String, Any>>()
        for (i in 0 until times.length()) {
            if (times.getString(i).startsWith(dayStr)) {
                result.add(mapOf(
                    "Temperature (°C)"   to hourly.getJSONArray("temperature_2m").getDouble(i),
                    "Wind Speed (km/h)"  to hourly.getJSONArray("windspeed_10m").getDouble(i),
                    "Wind Direction (°)" to hourly.getJSONArray("winddirection_10m").getDouble(i),
                    "Weather Condition"  to (WEATHER_CODES[hourly.getJSONArray("weathercode").getInt(i)] ?: "Unknown"),
                    "Local Time"         to times.getString(i)
                ))
            }
        }
        result
    }
}


object NewsApi {

    private val API_KEY = "pub_a5b78b04509c4f26a27113fd9ae03147"





    private fun countryCode(name:String):String{
        val py = Python.getInstance()
        val mod = py.getModule("pycountry")

        return  mod.callAttr("country_code",name).toString()

    }


    suspend fun getNews(
        country: String? = null,
        source: String?  = null,
        topic: String?   = null,
        language: String = "en"
    ): List<Map<String, String?>> = withContext(Dispatchers.IO) {

        var resolvedSource  = source
        var resolvedCountry = country


        if (resolvedSource?.lowercase() == "rte.ie") {
            resolvedSource  = "independent.ie"
            resolvedCountry = "Ireland"
        }

        var countryCode: String? = null

        if (!resolvedCountry.isNullOrBlank() && resolvedCountry.lowercase() != "none"){
            countryCode = countryCode(resolvedCountry)
        }

        val params = buildString {
            append("https://newsdata.io/api/1/news?language=$language&apikey=$API_KEY")
            if (countryCode   != null) append("&country=$countryCode")
            if (resolvedSource != null) append("&domainurl=$resolvedSource")
            if (topic          != null) append("&q=$topic")
        }

        val data     = JSONObject(URL(params).readText())
        val articles = data.optJSONArray("results")

        if (articles == null || articles.length() == 0) return@withContext emptyList()

        val result = mutableListOf<Map<String, String?>>()
        val limit  = minOf(articles.length(), 5)

        for (i in 0 until limit) {
            val article = articles.getJSONObject(i)
            result.add(mapOf(
                "Title"       to article.optString("title"),
                "Link"        to article.optString("link"),
                "Description" to article.optString("description"),
                "Published"   to article.optString("pubDate")
            ))
        }
        result
    }
}