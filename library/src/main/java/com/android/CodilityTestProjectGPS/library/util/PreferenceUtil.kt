package com.android.CodilityTestProjectGPS.library.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.android.CodilityTestProjectGPS.library.R


/**
 * Provides access to SharedPreferences to Activities and Services.
 */
object PreferenceUtil {
    const val SECONDS_TO_MILLISECONDS = 1000

    val METERS = "1"
    val METERS_PER_SECOND = "1"
    val KILOMETERS_PER_HOUR = "2"

    /**
     * Returns the minTime between location updates used for the LocationListener in milliseconds
     */
    fun minTimeMillis(context: Context, prefs: SharedPreferences): Long {
        val minTimeDouble: Double =
            prefs.getString(context.getString(R.string.pref_key_gps_min_time), "1")
                ?.toDouble() ?: 1.0
        return (minTimeDouble * SECONDS_TO_MILLISECONDS).toLong()
    }

    fun minDistance(context: Context, prefs: SharedPreferences): Float {
        return prefs.getString(context.getString(R.string.pref_key_gps_min_distance), "0") ?.toFloat() ?: 0.0f
    }



    fun newStopTrackingListener(cancelFlows: () -> Unit, prefs: SharedPreferences): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferenceUtils.KEY_SERVICE_TRACKING_ENABLED) {
                if (!PreferenceUtils.isTrackingStarted(prefs)) {
                    cancelFlows()
                }
            }
        }
    }

    fun runInBackground(context: Context, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(context.getString(R.string.pref_key_gnss_background), false)
    }

    fun hasInternetConnection(context: Context?): Boolean {
        if (context == null)
            return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

}