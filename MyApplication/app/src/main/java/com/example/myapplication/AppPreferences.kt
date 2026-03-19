package com.example.myapplication


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first


val Context.dataStore by preferencesDataStore(name = "settings")

object AppPreferences {
    private val SERVER_IP_KEY = stringPreferencesKey("server_ip")
    private val ASL_TIMER = intPreferencesKey("asl_timer")

    suspend fun saveIp(context: Context, ip: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_IP_KEY] = ip
        }
    }
    suspend fun saveTimer(context: Context, value: Int) {
        context.dataStore.edit { prefs ->
            prefs[ASL_TIMER] = value
        }
    }

    suspend fun loadIp(context: Context): String {
        val prefs = context.dataStore.data.first()
        return prefs[SERVER_IP_KEY] ?: "192.168.1.11" // fallback default
    }

    suspend fun loadTimer(context: Context): Int {
        val prefs = context.dataStore.data.first()
        return prefs[ASL_TIMER] ?: 2 // fallback default
    }
}