
package com.android.CodilityTestProjectGPS.library.util

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.android.CodilityTestProjectGPS.library.R


/**
 * Utilities for processing user interface elements
 */
object LibUIUtils {
    const val TAG = "UIUtils"
    const val ANIMATION_DURATION_SHORT_MS = 200




    /**
     * Shows a view using animation
     *
     * @param v                 View to show
     * @param animationDuration duration of animation
     */
    fun showViewWithAnimation(v: View, animationDuration: Int) {
        if (v.visibility == View.VISIBLE && v.alpha == 1f) {
            // View is already visible and not transparent, return without doing anything
            return
        }
        v.clearAnimation()
        v.animate().cancel()
        if (v.visibility != View.VISIBLE) {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            v.alpha = 0f
            v.visibility = View.VISIBLE
        }

        // Animate the content view to 100% opacity, and clear any animation listener set on the view.
        v.animate()
            .alpha(1f)
            .setDuration(animationDuration.toLong())
            .setListener(null)
    }

    /**
     * Hides a view using animation
     *
     * @param v                 View to hide
     * @param animationDuration duration of animation
     */
    fun hideViewWithAnimation(v: View, animationDuration: Int) {
        if (v.visibility == View.GONE) {
            // View is already gone, return without doing anything
            return
        }
        v.clearAnimation()
        v.animate().cancel()

        // Animate the view to 0% opacity. After the animation ends, set its visibility to GONE as
        // an optimization step (it won't participate in layout passes, etc.)
        v.animate()
            .alpha(0f)
            .setDuration(animationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                }
            })
    }

    /**
     * Shows the dialog to explain why location permissions are needed
     *
     * NOTE - this dialog can't be managed under the old dialog framework as the method
     * ActivityCompat.shouldShowRequestPermissionRationale() always returns false.
     */
    fun showLocationPermissionDialog(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.title_location_permission)
            .setMessage(R.string.text_location_permission)
            .setCancelable(false)
            .setPositiveButton(
                R.string.ok
            ) { dialog: DialogInterface?, which: Int ->
                // Request permissions from the user
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    PermissionUtils.LOCATION_PERMISSION_REQUEST
                )
            }
            .setNegativeButton(
                R.string.exit
            ) { dialog: DialogInterface?, which: Int ->
                // Exit app
                activity.finish()
            }
        builder.create().show()
    }

    /**
     * Ask the user if they want to enable GPS, and if so, show them system settings
     */
    fun promptEnableGps(context: Context,activity: Activity) {
        AlertDialog.Builder(activity)
            .setMessage(context.getString(R.string.enable_gps_message))
            .setPositiveButton(
                context.getString(R.string.enable_gps_positive_button)
            ) { dialog: DialogInterface?, which: Int ->
                val intent = Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
                activity.startActivity(intent)
            }
            .setNegativeButton(
                context.getString(R.string.enable_gps_negative_button)
            ) { dialog: DialogInterface?, which: Int -> }
            .show()
    }




//    /**
//     * Returns the `location` object as a human readable string for use in a notification summary
//     */
//    fun Location?.toNotificationSummary(context: Context, prefs: SharedPreferences): String {
//        return if (this != null) {
//            val lat = FormatUtils.formatLatOrLon(context, latitude, CoordinateType.LATITUDE, prefs)
//            val lon = FormatUtils.formatLatOrLon(context, longitude, CoordinateType.LONGITUDE, prefs)
//            val alt = FormatUtils.formatAltitude(context, this, prefs)
//            val speed = FormatUtils.formatSpeed(context,this, prefs)
//            val bearing = FormatUtils.formatBearing(context,this)
//            "$lat $lon $alt | $speed | $bearing"
//        } else {
//            "Unknown location"
//        }
//    }
}