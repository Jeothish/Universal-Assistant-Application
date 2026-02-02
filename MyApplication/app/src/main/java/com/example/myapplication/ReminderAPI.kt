package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.String

suspend fun getReminders(): List<ReminderGet> = withContext(Dispatchers.IO){
    val url = URL("http://ip:8000/reminders/get")
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"

    val response = connection.inputStream.bufferedReader().readText()
    val jsonArray = JSONArray(response)
    val reminders = mutableListOf<ReminderGet>()

    for (i in 0 until jsonArray.length()){
        val arr = jsonArray.getJSONArray(i)
        reminders.add(
            ReminderGet(
                reminder_title = arr.optString(1,"No title"),
                reminder_date = arr.optString(2,"No date"),
                reminder_description = arr.optString(3,"No description"),
                is_complete = arr.optBoolean(4,false) ,
                recurrence_type = arr.optString(5,"none"),
                recurrence_day_of_week = if(arr.isNull(6)) null else arr.getInt(6),
                recurrence_time = arr.optString(7,null)
                ))
    }
    return@withContext reminders
}
