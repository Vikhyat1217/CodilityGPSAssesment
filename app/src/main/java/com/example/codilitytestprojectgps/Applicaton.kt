package com.android.CodilityTestProjectGPS

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.preference.PreferenceManager
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

/**
 * Holds application-wide state
 *
 * @author Vikhyat Chandra
 */
@HiltAndroidApp
class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        lateinit var app: Application
            private set

        lateinit var prefs: SharedPreferences
            private set
    }
}