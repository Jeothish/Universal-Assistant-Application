package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.MDC.put
import java.net.HttpURLConnection
import java.net.URL
import kotlin.String
import kotlin.apply

suspend fun getReminders(): List<ReminderGet> = withContext(Dispatchers.IO){
    val url = URL("http://192.168.1.135:8000/reminders/get")
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"

    val response = connection.inputStream.bufferedReader().readText()
    val jsonArray = JSONArray(response)
    val reminders = mutableListOf<ReminderGet>()

    for (i in 0 until jsonArray.length()){
        val obj = jsonArray.getJSONObject(i)
        reminders.add(
            ReminderGet(
                reminder_id = obj.getInt("reminder_id"),
                reminder_title = obj.optString("reminder_title",null),
                reminder_date = obj.optString("reminder_date",null),
                reminder_description = obj.optString("reminder_description",null),
                is_complete = obj.optBoolean("is_complete",false),
                recurrence_type = obj.optString("recurrence_type",null),
                reminder_time = obj.optString("reminder_time",null),
            ))
    }
    return@withContext reminders
}

suspend fun createReminder(reminder: ReminderCreate): Boolean = withContext(Dispatchers.IO){
    val url = URL("http://192.168.1.135:8000/reminders/add")
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Content-Type","application/json")

    val json = JSONObject().apply{
        put("reminder_title", reminder.reminder_title)
        put("reminder_date", reminder.reminder_date)
        put("reminder_description", reminder.reminder_description)
        put("is_complete", reminder.is_complete)
        put("recurrence_type", reminder.recurrence_type)
        put("reminder_time", reminder.reminder_time)
    }

    connection.outputStream.use { outputStream -> outputStream.write(json.toString().toByteArray())}

    val success = connection.responseCode in 200..299
    connection.disconnect()
    return@withContext success
}

suspend fun deleteReminder(reminderId: Int): Boolean = withContext(Dispatchers.IO){
    val url = URL("http://192.168.1.135:8000/reminders/delete/$reminderId")
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "DELETE"
    connection.responseCode in 200..299
}

suspend fun updateReminders(reminderId: Int, reminder: ReminderEdit): Boolean = withContext(Dispatchers.IO) {
    val url = URL("http://192.168.1.135:8000/reminders/edit/$reminderId")
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "PATCH"
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json")

    val json = JSONObject().apply {
        reminder.reminder_title?.let { title -> put("reminder_title", title) }
        reminder.reminder_date?.let { date -> put("reminder_date", date) }
        reminder.reminder_description?.let { description-> put("reminder_description", description) }
        reminder.is_complete?.let { isComplete -> put("is_complete", isComplete) }
        reminder.recurrence_type?.let { type -> put("recurrence_type", type) }
        reminder.reminder_time?.let {time ->  put("reminder_time", time)}
    }

    connection.outputStream.use { outputStream -> outputStream.write(json.toString().toByteArray())}
    connection.responseCode in 200..299


}
