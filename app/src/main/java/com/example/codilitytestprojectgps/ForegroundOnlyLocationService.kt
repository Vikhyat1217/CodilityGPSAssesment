package com.android.CodilityTestProjectGPS

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.CodilityTestProjectGPS.Application.Companion.prefs
import com.android.CodilityTestProjectGPS.library.data.LocationRepository
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtils
import com.android.CodilityTestProjectGPS.ui.MainActivity
import com.example.codilitytestprojectgps.ui.NotificationUpdateListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Service tracks location, logs to files, and shows a notification to the user.
 *
 * Flows are kept active by this Service while it is bound and started.
 */
@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() , NotificationUpdateListener {
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false


    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()
    private var isBound = false
    private var isStarted = false
    private var isForeground = false
    private lateinit var notificationManager: NotificationManager

    // We save a local reference to last location and SatelliteStatus to create a Notification
    private var currentLocation: Location? = null

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository


    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null


    private var deletedFiles = false
    private var injectedAssistData = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
        } else {
            if (!isStarted) {
                isStarted = true
                try {
                    observeFlows()
                } catch (unlikely: Exception) {
                    PreferenceUtils.saveTrackingStarted(false, prefs)
                    Log.e(TAG, "Exception registering for updates: $unlikely")
                }

                // We may have been restarted by the system. Manage our lifetime accordingly.
                goForegroundOrStopSelf()
            }
        }

        // Tells the system to recreate the service after it's been killed.
        return super.onStartCommand(intent, flags, START_NOT_STICKY)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind()")
        configurationChange = false
        handleBind()
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        //stopForeground(true)
        //serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
        handleBind()
    }

    private fun handleBind() {
        if (!isBound) {
            isBound = true
            // Start ourself. This will begin collecting exercise state if we aren't already.
            //startService(Intent(this, this::class.java))
            // As long as a UI client is bound to us, we can hide the ongoing activity notification.
            //removeOngoingActivityNotification()
        }
    }


    override fun onUnbind(intent: Intent): Boolean {
        isBound = false
        lifecycleScope.launch {
            // Client can unbind because it went through a configuration change, in which case it
            // will be recreated and bind again shortly. Wait a few seconds, and if still not bound,
            // manage our lifetime accordingly.
            delay(UNBIND_DELAY_MILLIS)
            if (!isBound) {
                goForegroundOrStopSelf()
            }
        }
        // Allow clients to re-bind. We will be informed of this in onRebind().
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        PreferenceUtils.saveTrackingStarted(true, prefs)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            cancelFlows()
            stopSelf()
            isStarted = false
            PreferenceUtils.saveTrackingStarted(false, prefs)
            removeOngoingActivityNotification()
            currentLocation = null
        } catch (unlikely: SecurityException) {
            PreferenceUtils.saveTrackingStarted(true, prefs)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    @SuppressLint("NewApi")
    @ExperimentalCoroutinesApi
    private fun observeFlows() {
        observeLocationFlow()
    }

    private fun cancelFlows() {
        locationFlow?.cancel()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                //Log.d(TAG, "Service location: ${it.toNotificationTitle()}")
                currentLocation = it
            }
            .launchIn(lifecycleScope)
    }


    private fun goForegroundOrStopSelf() {
        lifecycleScope.launch {
            // We may have been restarted by the system - check if we're still monitoring data
            if (PreferenceUtils.isTrackingStarted(prefs)) {
                // Monitoring GNSS data
                postOngoingActivityNotification()
            } else {
                // We have nothing to do, so we can stop.
                stopSelf()
                isStarted = false
            }
        }
    }

    private fun postOngoingActivityNotification() {
        if (!isForeground) {
            isForeground = true
            Log.d(TAG, "Posting ongoing activity notification")

            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification(currentLocation, "currentSatellites"))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    private fun buildNotification(location: Location?, satellites: String): Notification {

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(satellites)
            .setBigContentTitle(satellites)

        // 3. Set up main Intent/Pending Intents for notification
        val launchActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            // NOTE: The above causes the activity/viewmodel to be recreated from scratch for Accuracy when it's already visible
            // and the notification is tapped (strangely if it's destroyed Accuracy viewmodel seems to keep it's state)
            // FLAG_ACTIVITY_REORDER_TO_FRONT seems like it should work, but if this is used then onResume() is called
            // again (and onPause() is never called). This seems to freeze up Status into a blank state because GNSS inits again.
        }
        val openActivityPendingIntent = PendingIntentCompat.getActivity(
            applicationContext,
            System.currentTimeMillis().toInt(),
            launchActivityIntent,
            0,
            false
        )

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java).apply {
            putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
        }
        val stopServicePendingIntent = PendingIntentCompat.getService(
            applicationContext,
            System.currentTimeMillis().toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(satellites)
            .setContentText(satellites)
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
            .setColor(ContextCompat.getColor(this, R.color.black))
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(PRIORITY_LOW) // For < API 26
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // For < API 26
            .setContentIntent(openActivityPendingIntent)
            .build()
    }

    private fun removeOngoingActivityNotification() {
        if (isForeground) {
            Log.d(TAG, "Removing ongoing activity notification")
            isForeground = false
            stopForeground(true)
        }
    }

    /**
     * Initialize and start logging if permissions have been granted.
     *
     * Note that this is called from each of the flows that log data, because when the user initially
     * enables logging in the settings the preference change callback happens before the user grants
     * file permissions. So we need to call this on each update in case the user just granted file
     * permissions but logging hasn't been started yet.
     */


    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val TAG = "LocationService"

        private const val PACKAGE_NAME = "com.android.Coditi"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL = "gsptest_channel_01"

        private const val UNBIND_DELAY_MILLIS = 3_000L
    }

    override fun onDataUpdated(data: String?) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(currentLocation , data!!)
        )
    }
}
