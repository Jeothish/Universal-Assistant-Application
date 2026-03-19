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


@Database(entities = [Reminder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}

object DatabaseProvider {

    private const val KEYSTORE_ALIAS = "SecureReminderKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFERENCES_NAME = "secure_preferences"
    private const val PREFERENCE_KEY = "db_passphrase"
    private var INSTANCE: AppDatabase? = null

    /**
     * Creates or retirves a Master Key stored in hardware-backed Android Keystore
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
     * Encrypts raw databse passphrase using the Keystore Master Key so it can be stored in SharedPreferences safely
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
     * Gets databse passphrase
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