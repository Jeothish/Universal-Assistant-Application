package com.example.myapplication


import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Reminder.
 * Provides methods for CRUD operations
 */
@Dao
interface ReminderDao {
    @Query("SELECT * FROM Reminders ORDER BY reminder_date ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminders WHERE reminder_id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert
    suspend fun insertReminder(reminder: Reminder) : Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}

/**
 * Data Access Object for caching WWikipedia articles locally
 * Provides methods for retrieving, inserting Wikipedia topics and deleting the cache
 */
@Dao
interface WikiDao {
    @Query("SELECT * FROM WikiCache WHERE topic = :topic LIMIT 1")
    suspend fun getWikiByTopic(topic: String): WikiCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWiki(wiki: WikiCache)

    @Query("DELETE FROM WikiCache WHERE (timestamp + :expiry) < :now")
    suspend fun deleteOldCache(expiry: Long, now: Long = System.currentTimeMillis())
}


/**
 * Data Access Object for caching weather locally
 * Provides methods for retrieving, inserting Weather and deleting the cache
 */
@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE weatherRequest = :key LIMIT 1")
    suspend fun getWeather(key: String): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherCache)

    @Query("DELETE FROM weather_cache WHERE (timestamp + :expiry) < :now")
    suspend fun deleteOldCache(expiry: Long, now: Long = System.currentTimeMillis())
}