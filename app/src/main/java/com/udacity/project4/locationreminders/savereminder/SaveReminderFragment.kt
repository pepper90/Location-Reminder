package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 2
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 3
        private const val LOCATION_PERMISSION_INDEX = 4
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 5
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
    private lateinit var snackbar: Snackbar

    lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = SelectLocationFragment.GEOFENCE_EVENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkDeviceLocationSettingsAndStartGeofence(_viewModel.findReminderDataItem())
        }
    }
    /*
    * GEOFENCE PERMISSION FUNCTIONS
    * */

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundPermissionApproved = (PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundPermissionApproved && backgroundPermissionApproved
    }

    @Suppress("DEPRECATION")
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        requestPermissions(permissionsArray, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {//use snackbar to inform user of need for permission
            snackbar.setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence(_viewModel.findReminderDataItem())
        }
    }

    /*
    * GEOFENCE FUNCTIONS
    * */

    private fun checkPermissionsAndStartGeofencing(reminder: ReminderDataItem) {

        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence(reminder)
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(
        reminder: ReminderDataItem,
        resolve: Boolean = true
    ) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationSettingRequestsBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingRequestsBuilder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->

            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resultLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence(_viewModel.findReminderDataItem())
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Device location enabled")
                addGeofence(reminder)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {

        if (reminder.longitude != null && reminder.latitude != null) {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    reminder.latitude!!, reminder.longitude!!,
                    SelectLocationFragment.GEOFENCE_RADIUS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            // Build the geofence request
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "Geofence added: ${geofence.requestId}")
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireActivity(), R.string.geofences_not_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message.toString())
                    }
                }
            }
        }
        _viewModel.validateAndSaveReminder(reminder)
    }

    /*
    * FRAGMENT FUNCTIONS
    * */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectedLocation.text = _viewModel.selectedPOI.value?.name

        snackbar = Snackbar.make(
            view, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
        ).apply { anchorView = binding.saveReminder }

        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminder = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude)

            _viewModel.validateAndSaveReminder(reminder)
            checkPermissionsAndStartGeofencing(reminder)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        if (::snackbar.isInitialized && snackbar.isShown) {
            snackbar.dismiss()
        }
    }
}
