package com.kanishk.golditemscanner.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalStorage private constructor(context: Context) {


    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getSharedPreferences(): SharedPreferences = sharedPreferences


    companion object {
        private const val PREF_NAME = "local_storage"

        private var INSTANCE: LocalStorage? = null

        fun getInstance(context: Context): LocalStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalStorage(context).apply { INSTANCE = this }
            }
        }

        inline fun <reified T> save(context: Context, key: StorageKey, value: T) {
            val editor = getInstance(context).getSharedPreferences().edit()
            when (T::class) {
                String::class -> editor.putString(key.name, value as String)
                Int::class -> editor.putInt(key.name, value as Int)
                Boolean::class -> editor.putBoolean(key.name, value as Boolean)
                Float::class -> editor.putFloat(key.name, value as Float)
                Long::class -> editor.putLong(key.name, value as Long)
                else -> throw IllegalArgumentException("Unsupported type")
            }
            editor.apply()
        }

        inline fun <reified T> get(context: Context, key: StorageKey): T? {
            val prefs = getInstance(context).getSharedPreferences()
            return when (T::class) {
                String::class -> prefs.getString(key.name, null) as T?
                Int::class -> prefs.getInt(key.name, 0) as T?
                Boolean::class -> prefs.getBoolean(key.name, false) as T?
                Float::class -> prefs.getFloat(key.name, 0f) as T?
                Long::class -> prefs.getLong(key.name, 0L) as T?
                else -> throw IllegalArgumentException("Unsupported type")
            }
        }

        inline fun <reified T> LocalStorage.Companion.saveObject(context: Context, key: StorageKey, value: T) {
            val gson = Gson()
            val json = gson.toJson(value) // Serialize the object to JSON
            val editor = getInstance(context).getSharedPreferences().edit()
            editor.putString(key.name, json)
            editor.apply()
        }

        inline fun <reified T> LocalStorage.Companion.getObject(context: Context, key: StorageKey): T? {
            val prefs = getInstance(context).getSharedPreferences()
            val json = prefs.getString(key.name, null) ?: return null // Retrieve the JSON string
            val gson = Gson()
            return gson.fromJson(json, object : TypeToken<T>() {}.type) // Deserialize the JSON to an object
        }
    }

    enum class StorageKey {
        RATE_PER_TOLA,
        LAST_UPDATED_TIMESTAMP,
        CURRENT_RATE,
        IS_LOGGED_IN
    }
}