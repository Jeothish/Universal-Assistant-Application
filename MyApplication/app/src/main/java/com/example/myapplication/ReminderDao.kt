package com.example.myapplication


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM Reminders ORDER BY reminder_date ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminders WHERE reminder_id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}

@Dao
interface WikiDao {
    @Query("SELECT * FROM WikiCache WHERE topic = :topic LIMIT 1")
    suspend fun getWikiByTopic(topic: String): WikiCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWiki(wiki: WikiCache)

    @Query("DELETE FROM WikiCache WHERE (timestamp + :expiry) < :now")
    suspend fun deleteOldCache(expiry: Long, now: Long = System.currentTimeMillis())
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE weatherRequest = :key LIMIT 1")
    suspend fun getWeather(key: String): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherCache)

    @Query("DELETE FROM weather_cache WHERE (timestamp + :expiry) < :now")
    suspend fun deleteOldCache(expiry: Long, now: Long = System.currentTimeMillis())
}