package com.example.ceparsivi

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LanguageManager {
    private const val PREF_NAME = "LanguagePref"
    private const val KEY_LANGUAGE = "language_code"

    fun setLocale(context: Context, languageCode: String?) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getSelectedLanguageCode(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // If no language is selected, return null to use the system default.
        return prefs.getString(KEY_LANGUAGE, null)
    }

    fun wrapContext(context: Context): ContextWrapper {
        val savedLanguageCode = getSelectedLanguageCode(context)
        val config = Configuration(context.resources.configuration)
        val locale = if (savedLanguageCode != null) {
            Locale(savedLanguageCode)
        } else {
            // If no language is selected, use the system's default locale
            Resources.getSystem().configuration.locales.get(0)
        }
        Locale.setDefault(locale)
        config.setLocale(locale)

        val newContext = context.createConfigurationContext(config)
        return ContextWrapper(newContext)
    }
}