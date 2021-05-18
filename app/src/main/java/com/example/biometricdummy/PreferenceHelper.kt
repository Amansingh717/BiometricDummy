package com.example.biometricdummy

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {

    private const val PREFS_NAME = "AppPreferences"

    fun getPreference(): SharedPreferences? = appPrefs

    private val appPrefs: SharedPreferences? = ContextProvider.contextProvider?.provideContext()
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = this.edit()
        operation(editor)
        editor.apply()
    }

    /**
     * puts a key value pair in shared appPrefs if doesn't exists, otherwise updates value on given [key]
     */
    operator fun SharedPreferences?.set(key: String, value: Any?) {
        if (this != null)
            when (value) {
                is String? -> edit { it.putString(key, value) }
                is Int -> edit { it.putInt(key, value) }
                is Boolean -> edit { it.putBoolean(key, value) }
                is Float -> edit { it.putFloat(key, value) }
                is Long -> edit { it.putLong(key, value) }
                else -> throw UnsupportedOperationException("Not yet implemented")
            }
    }

    /**
     * finds value on given key.
     * [T] is the type of value
     * @param defaultValue optional default value - will take null for strings, false for bool and -1 for numeric values if [defaultValue] is not specified
     */
    inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        defaultValue: T? = null
    ): T? {
        return when (T::class) {
            String::class -> getString(key, defaultValue as? String ?: "") as T?
            Int::class -> getInt(key, defaultValue as? Int ?: 0) as T?
            Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
            Float::class -> getFloat(key, defaultValue as? Float ?: 0f) as T?
            Long::class -> getLong(key, defaultValue as? Long ?: 0) as T?
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }

    //encryption IV
    fun setBase64EncryptionIv(base64EncryptionIv: String) =
        appPrefs?.set(PREF_BASE64_ENCRYPTION_IV, base64EncryptionIv)

    fun getBase64EncryptionIv(): String = appPrefs?.get(PREF_BASE64_ENCRYPTION_IV, "") ?: ""

    //secret Text
    fun setBase64SecretText(base64UserName: String) =
        appPrefs?.set(PREF_BASE64_SECRET_TEXT_CIPHER, base64UserName)

    fun getBase64SecretText(): String = appPrefs?.get(PREF_BASE64_SECRET_TEXT_CIPHER, "") ?: ""

}