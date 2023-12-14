package com.android.CodilityTestProjectGPS.library.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


object PermissionUtils {
    const val LOCATION_PERMISSION_REQUEST = 1
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Returns true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     * @param activity
     * @param requiredPermissions
     * @return true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     */
    fun hasGrantedPermissions(activity: Activity?, requiredPermissions: Array<String>): Boolean {
        for (p in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    activity!!,
                    p!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}