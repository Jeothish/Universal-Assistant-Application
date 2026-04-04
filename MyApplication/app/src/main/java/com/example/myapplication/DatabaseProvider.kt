package com.example.myapplication

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.crypto.SecretKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

//Room database for the application storing reminders, wiki cache, and weather cache
@Database(entities = [Reminder::class, WikiCache::class, WeatherCache::class], version = 4, exportSchema = false)

//AppDatabase inherits RoomDatabase and gives access to DAO methods
abstract class AppDatabase : RoomDatabase() {


    abstract fun reminderDao(): ReminderDao
    abstract fun wikiDao(): WikiDao
    abstract fun weatherDao(): WeatherDao
}

/**
 * Creates a secure instance of AppDatabase
 *
 * Handles:
 * - Master key generation in Android Keystore
 * - Secure database passphrase storage in SharedPreferences
 * - Encryption/Decryption of database keys using AES-GCM
 */
object DatabaseProvider {

    private const val KEYSTORE_ALIAS = "SecureReminderKey" //Unique key name for Android Keystore
    private const val ANDROID_KEYSTORE = "AndroidKeyStore" //The keystore provider
    private const val PREFERENCES_NAME = "secure_preferences" //Shared preferences file to store encrypted DB passphrase
    private const val PREFERENCE_KEY = "db_passphrase" //Key name for stored preferences
    private var INSTANCE: AppDatabase? = null //Cached instance of Room database to prevent multiple openings

    /**
     * Creates or retrieves a Master Key stored in hardware-backed Android Keystore
     *
     * @return SecretKey used for encrypting database passphrase
     */
    private fun getOrCreateSecretKey(): SecretKey{
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        //If the key already exists return it
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        //Otherwise generate a new AES-256 key
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    /**
     * Encrypts raw database passphrase using the Keystore Master Key so it can be stored in SharedPreferences safely
     *
     * @param key Raw database passphrase
     * @param keystoreKey Master key from Keystore
     * @return Base64-encoded encrypted key with IV prepended
     */
    private fun wrapKey(key: ByteArray,keystoreKey: SecretKey): String{
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val encrypted = cipher.doFinal(key)
        val iv = cipher.iv
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypts saved database passphrase using the Keystore Master Key
     * @param encoded Base64-encoded encrypted key
     * @param keyStoreKey Master key from Keystore
     * @return Raw decrypted database key
     */
    private fun unwrapKey(encoded:String, keyStoreKey: SecretKey): ByteArray{
        val combined = Base64.decode(encoded, Base64.DEFAULT)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyStoreKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * Retrieves or generates the database passphrase for SQLCipher.
     *
     * @param context Application context
     * @return Byte array used as database passphrase
     */
    private fun getDatabaseKey(context: Context): ByteArray {
        val keystoreKey = getOrCreateSecretKey()
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val wrappedKey = prefs.getString(PREFERENCE_KEY, null)

        return if (wrappedKey != null) {
            unwrapKey(wrappedKey, keystoreKey)
        } else {
            val key = ByteArray(32)
            SecureRandom().nextBytes(key)
            val wrapped = wrapKey(key, keystoreKey)
            prefs.edit().putString(PREFERENCE_KEY, wrapped).apply()
            key
        }
    }

    /**
     * Returns a single instance of AppDatabse
     *
     * @param context Application context
     * @return Secure Room database instance
     */
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this){

            val secretKey = getOrCreateSecretKey()
            val passphrase: ByteArray = getDatabaseKey(context)


            val factory = SupportFactory(passphrase)

            val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java,"secure_reminder_db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance
            instance
        }
    }



}