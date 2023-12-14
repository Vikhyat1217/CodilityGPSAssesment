package com.android.CodilityTestProjectGPS.library.util;

import static java.util.Collections.emptySet;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import com.android.CodilityTestProjectGPS.library.R;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A class containing utility methods related to preferences
 */
public class PreferenceUtils {

    public static final int CAPABILITY_LOCATION_DISABLED = 2;

    public static final String KEY_SERVICE_TRACKING_ENABLED = "tracking_foreground_location";

    /**
     * Gets the string description of a CAPABILITY_* constant
     * @param capability CAPABILITY_* constant defined in this class
     * @return a string description of the CAPABILITY_* constant
     */
    public static String getCapabilityDescription(Context context, int capability) {
        switch (capability) {
            case CAPABILITY_LOCATION_DISABLED:
                return context.getString(R.string.capability_value_location_disabled);
            default:
                return context.getString(R.string.default_capability);
        }
    }


    @TargetApi(9)
    public static void saveString(String key, String value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveInt(String key, int value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(key, value);
        edit.apply();
    }

    public static int getInt(String key, int defaultValue, SharedPreferences prefs) {
        return prefs.getInt(key, defaultValue);
    }

    @TargetApi(9)
    public static void saveLong(String key, long value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveBoolean(String key, boolean value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveFloat(String key, float value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(key, value);
        edit.apply();
    }

    @TargetApi(9)
    public static void saveDouble(String key, double value, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, Double.doubleToRawLongBits(value));
        edit.apply();
    }

    /**
     * Gets a double for the provided key from preferences, or the default value if the preference
     * doesn't currently have a value
     *
     * @param key          key for the preference
     * @param defaultValue the default value to return if the key doesn't have a value
     * @return a double from preferences, or the default value if it doesn't exist
     */
    public static Double getDouble(String key, double defaultValue, SharedPreferences prefs) {
        if (!prefs.contains(key)) {
            return defaultValue;
        }
        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }

    public static String getString(String key, SharedPreferences prefs) {
        return prefs.getString(key, null);
    }

    public static long getLong(SharedPreferences prefs, String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }

    public static float getFloat(SharedPreferences prefs, String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }




    /**
     * Removes the specified preference by deleting it
     * @param key
     */
    public static void remove(String key, SharedPreferences prefs) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(key).apply();
    }

    /**
     * Returns true if service location tracking is active, and false if it is not
     * @return true if service location tracking is active, and false if it is not
     */
    public static boolean isTrackingStarted(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_SERVICE_TRACKING_ENABLED, false);
    }

    /**
     * Saves the provided value as the current service location tracking state
     * @param value true if service location tracking is active, and false if it is not
     */
    public static void saveTrackingStarted(boolean value, SharedPreferences prefs) {
        saveBoolean(KEY_SERVICE_TRACKING_ENABLED, value, prefs);
    }
}