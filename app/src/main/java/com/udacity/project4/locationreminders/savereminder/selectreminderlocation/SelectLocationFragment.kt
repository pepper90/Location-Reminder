package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.createTitle
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment() {

    companion object {
        const val TAG = "SelectLocationFragment"
        const val GEOFENCE_EVENT = "GEOFENCE_EVENT"
        internal const val GEOFENCE_RADIUS = 50f
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var snackbarOne: Snackbar
    private lateinit var snackbarTwo: Snackbar

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myMarker: Marker? = null
    private var myPoi: PointOfInterest? = null

    lateinit var geofencingClient: GeofencingClient

    /*
    * MAP CALLBACK
    * */

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        setMapStyle(map)
        enableLocation()
        setMapLongClick(map)
        setPoiClick(map)
    }

    /*
    * LOCATION PERMISSION FUNCTIONS
    * */

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        @Suppress("DEPRECATION")
        if (shouldProvideRationale) {
            snackbarOne.setAction(R.string.settings) {
                requestPermissions(
                    permissionsArray,
                    REQUEST_LOCATION_PERMISSION)
            }.show()
        } else {
            requestPermissions(
                permissionsArray,
                REQUEST_LOCATION_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult")
        if (grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED
        ) {
            // Create an action that opens the settings for the specific app
            snackbarTwo.setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            enableLocation()
        }
    }

    /*
    * FRAGMENT FUNCTIONS
    * */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //Sets map container inside fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        //initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //Initialize geofence client
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        //Send location to viewModel
        binding.saveBtn.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snackbarOne = Snackbar.make(
            view,
            R.string.location_required_error,
            Snackbar.LENGTH_INDEFINITE
        )
        snackbarTwo = Snackbar.make(
            view,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
    }

    private fun onLocationSelected() {
        _viewModel.selectedPOI.value = myPoi
        _viewModel.latitude.value = myPoi?.latLng?.latitude
        _viewModel.longitude.value = myPoi?.latLng?.longitude
        _viewModel.reminderSelectedLocationStr.value = myPoi?.name
        findNavController().popBackStack()
    }

    /*
    * MAP-RELATED FUNCTIONS
    * */

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val title = createTitle(latLng)
            myPoi = PointOfInterest(latLng, title, title)
            map.clear(); myMarker?.remove()
            myMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            myMarker?.showInfoWindow()
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear(); myMarker?.remove()
            myPoi = poi
            myMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            myMarker?.showInfoWindow()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (isPermissionGranted()) {
            Log.d(TAG, "Permissions granted, preparing user location")
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    run {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val lat = location.latitude
                            val long = location.longitude
                            val currentPosition = LatLng(lat, long)
                            val title = createTitle(currentPosition)

                            myPoi = PointOfInterest(currentPosition, title, title)

                            myMarker = map.addMarker(
                                MarkerOptions()
                                    .position(currentPosition)
                                    .title(getString(R.string.you_are_here))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            )
                            myMarker?.showInfoWindow()
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
                        }
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    // Sets custom map styling
    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    //Inflates the map style menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    //Sets map style menu options
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::snackbarOne.isInitialized && ::snackbarTwo.isInitialized) {
            snackbarOne.dismiss(); @Suppress("DEPRECATION")
            snackbarTwo.dismiss()
        }
    }
}
