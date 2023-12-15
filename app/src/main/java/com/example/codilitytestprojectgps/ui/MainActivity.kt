package com.android.CodilityTestProjectGPS.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.CodilityTestProjectGPS.Application.Companion.app
import com.android.CodilityTestProjectGPS.Application.Companion.prefs
import com.android.CodilityTestProjectGPS.ForegroundOnlyLocationService
import com.android.CodilityTestProjectGPS.R
import com.android.CodilityTestProjectGPS.databinding.ActivityMainBinding
import com.android.CodilityTestProjectGPS.library.data.LocationRepository
import com.android.CodilityTestProjectGPS.library.ui.LocationViewModel
import com.android.CodilityTestProjectGPS.library.util.*
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtil.minDistance
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtil.minTimeMillis
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtil.runInBackground
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtils.isTrackingStarted
import com.example.codilitytestprojectgps.ui.NotificationUpdateListener
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var useDarkTheme = false

    /**
     * Currently selected navigation drawer position (so we don't unnecessarily swap fragments
     * if the same item is selected).  Initialized to -1 so the initial callback from
     * NavigationDrawerFragment always instantiates the fragments
     */
    private var currentNavDrawerPosition = -1


    // Main signal view model
    @OptIn(ExperimentalCoroutinesApi::class)
    private val signalInfoViewModel: LocationViewModel by viewModels()

    private var switch: SwitchMaterial? = null
    private var lastLocation: Location? = null
    var lastSavedInstanceState: Bundle? = null
    private var userDeniedPermission = false

    private var initialLanguage: String? = null
    private var initialMinTimeMillis: Long? = null
    private var initialMinDistance: Float? = null

    private var shareDialogOpen = false
    private var progressBar: ProgressBar? = null
    private var isServiceBound = false
    private var service: ForegroundOnlyLocationService? = null
    private lateinit var  notificationUpdateListener : NotificationUpdateListener


    private var foregroundOnlyServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder = iBinder as ForegroundOnlyLocationService.LocalBinder
            Log.d(TAG, "Service Bound ")
            service = binder.service
            notificationUpdateListener = binder.service
            isServiceBound = true
            if (locationFlow?.isActive == true) {
                // Activity started location updates but service wasn't bound yet - tell service to start now
                service?.subscribeToLocationUpdates()

            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            service = null
            isServiceBound = false
        }
    }

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via service notification
    private val stopTrackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        PreferenceUtil.newStopTrackingListener ({ gpsStop() }, prefs)

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme
//        if (darkTheme(app, prefs)) {
//            setTheme(R.style.AppTheme_Dark_NoActionBar)
//            useDarkTheme = true
//        }
        super.onCreate(savedInstanceState)
        // Reset the activity title to make sure dynamic locale changes are shown
//        LibUIUtils.resetActivityTitle(this)
        saveInstanceState(savedInstanceState)

        // Observe stopping location updates from the service
        prefs.registerOnSharedPreferenceChangeListener(stopTrackingListener)

        // Set the default values from the XML file if this is the first execution of the app
//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
//        initialLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language), prefs)
        initialMinTimeMillis = minTimeMillis(app, prefs)
        initialMinDistance = minDistance(app, prefs)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //setSupportActionBar(binding.toolbar)
        progressBar = findViewById(R.id.progress_horizontal)
        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, BIND_AUTO_CREATE)
    }



    /**
     * Save instance state locally so we can use it after the permission callback
     * @param savedInstanceState instance state to save
     */
    private fun saveInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            lastSavedInstanceState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                savedInstanceState.deepCopy()
            } else {
                savedInstanceState
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }



    override fun onResume() {
        super.onResume()
        shareDialogOpen = false
        if (!userDeniedPermission) {
            requestPermissionAndInit(this)
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            LibUIUtils.showLocationPermissionDialog(this)
        }
    }


    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }



    private fun requestPermissionAndInit(activity: Activity) {
        if (PermissionUtils.hasGrantedPermissions(activity, PermissionUtils.REQUIRED_PERMISSIONS)) {
            init()
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(
                activity,
                PermissionUtils.REQUIRED_PERMISSIONS,
                PermissionUtils.LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userDeniedPermission = false
                init()
            } else {
                userDeniedPermission = true
            }
        }
    }

    private fun init() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
        if (provider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER")
            Toast.makeText(
                this, "Unable to get GPS_PROVIDER",
                Toast.LENGTH_SHORT
            ).show()
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LibUIUtils.promptEnableGps(app,this)
        }
        setupStartState(lastSavedInstanceState)

        if (this.useDarkTheme != useDarkTheme) {
            this.useDarkTheme = useDarkTheme
            recreate()
        }
        val settings = prefs
        checkKeepScreenOn(settings)
    }

    override fun onPause() {
        // Stop GNSS if this isn't a configuration change and the user hasn't opted to run in background
        if (!isChangingConfigurations && !runInBackground(app, prefs)) {
           // service?.unsubscribeToLocationUpdates()
        }
        super.onPause()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupStartState(savedInstanceState: Bundle?) {
        // Use "Auto-start GNSS" setting, or existing tracking state (e.g., if service is running)
        if (prefs.getBoolean(
                getString(R.string.pref_key_auto_start_gps),
                true
            ) || isTrackingStarted(prefs)
        ) {
            gpsStart()
        }
    }


    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun gpsStart() {
        PreferenceUtils.saveTrackingStarted(true, prefs)
//        service?.subscribeToLocationUpdates()
        showProgressBar()

        // Observe flows
        observeLocationFlow()

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        // This should be a Flow and not LiveData to ensure that the Flow is active before the Service is bound
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        signalInfoViewModel.geoCodingResponse.observe(this){
            notificationUpdateListener.onDataUpdated(it.display_name)
            it?.let {  binding.geocodingValue.setText(it.display_name)}
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                lastLocation = it
                val latLngText = it.latitude.toString() + ":" +it.longitude.toString()
                binding.latLngValue.setText(latLngText)
                Log.d(TAG, "Activity location: ${lastLocation}")
                
                hideProgressBar()
                signalInfoViewModel.getReverseGeocoding(it.latitude , it.longitude)

                // Reset the options menu to trigger updates to action bar menu items
                invalidateOptionsMenu()

            }
            .launchIn(lifecycleScope)
    }


    @Synchronized
    private fun gpsStop() {
        PreferenceUtils.saveTrackingStarted(false, prefs)
        locationFlow?.cancel()

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
        progressBar?.visibility = View.GONE
    }

    private fun hideProgressBar() {
        val p = progressBar
        if (p != null) {
            LibUIUtils.hideViewWithAnimation(p, LibUIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun showProgressBar() {
        val p = progressBar
        if (p != null) {
            LibUIUtils.showViewWithAnimation(p, LibUIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun checkKeepScreenOn(settings: SharedPreferences) {
        binding.toolbar.keepScreenOn =
            settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        initGpsSwitch(menu)
        return true
    }

    @ExperimentalCoroutinesApi
    private fun initGpsSwitch(menu: Menu) {
        val item = menu.findItem(R.id.gps_switch_item)
        if (item != null) {
            switch = MenuItemCompat.getActionView(item).findViewById(R.id.gps_switch)
            if (switch != null) {
                // Initialize state of GPS switch before we set the listener, so we don't double-trigger start or stop
                switch!!.isChecked = isTrackingStarted(prefs)

                // Set up listener for GPS on/off switch
                switch!!.setOnClickListener {
                    // Turn GPS on or off
                    if (!switch!!.isChecked && isTrackingStarted(prefs)) {
                        gpsStop()
                        //service?.unsubscribeToLocationUpdates()
                    } else {
                        if (switch!!.isChecked && !isTrackingStarted(prefs)) {
                            gpsStart()
                        }
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item selection
        when (item.itemId) {
            R.id.gps_switch -> {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }





    companion object {
        private const val TAG = "GpsTestActivity"
        private const val SECONDS_TO_MILLISECONDS = 1000
        private const val GPS_RESUME = "gps_resume"
    }
}

