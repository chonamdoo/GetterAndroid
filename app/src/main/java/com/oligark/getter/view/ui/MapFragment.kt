package com.oligark.getter.view.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.oligark.getter.R
import android.widget.RelativeLayout
import com.google.android.gms.maps.model.*
import com.oligark.getter.service.model.Store
import com.oligark.getter.viewmodel.OfferViewModel
import com.oligark.getter.viewmodel.StoresViewModel
import com.oligark.getter.viewmodel.resources.DataResource
import java.lang.Exception

/**
 * Created by pmvb on 17-09-23.
 */
class MapFragment :
        SupportMapFragment(),
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMarkerClickListener {

    companion object {
        @JvmField val TAG = MapFragment::class.java.simpleName
        @JvmStatic private val DEFAULT_ZOOM = 16.toFloat()
        @JvmField val PERMISSION_REQUEST_FINE_LOCATION = 51412
    }

    interface OnStoreSelectCallback {
        fun onStoreSelected(store: Store)
    }

    private var mMap: GoogleMap? = null
    private var mLocationPermissionGranted = false

    private lateinit var locationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val mDefaultLocation = LatLng(-12.069444, -77.079444) // PUCP
    private var mLocationCircle: Circle? = null

    private lateinit var storesViewModel: StoresViewModel
    private lateinit var offerViewModel: OfferViewModel
    private var mOnStoreSelectCallback: OnStoreSelectCallback? = null

    override fun onActivityCreated(data: Bundle?) {
        super.onActivityCreated(data)

        if (requestLocationPermission()) {
            locationClient = LocationServices.getFusedLocationProviderClient(activity)
        }
        storesViewModel = ViewModelProviders.of(activity).get(StoresViewModel::class.java)
        offerViewModel = ViewModelProviders.of(activity).get(OfferViewModel::class.java)

        try {
            mOnStoreSelectCallback = activity as OnStoreSelectCallback
        } catch (e: ClassCastException) {
            Log.e(TAG, "Activity does not implement ${OnStoreSelectCallback::class.java.simpleName}")
        }

        getMapAsync(this)
    }

    private fun updateStoreMarkers(stores: List<Store>) {
        if (mMap == null) {
            return
        }
        stores.forEach { store ->
            val marker = mMap?.addMarker(MarkerOptions()
                    .position(LatLng(store.latitude, store.longitude))
                    .title(store.businessName)
            )
            marker?.tag = store
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                    mapSetup()
                } else {
                    Toast.makeText(
                            activity,
                            getString(R.string.location_permission_rejected),
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * Requests location permission, if already granted, returns true
     * If not granted, requests permission and returns false
     */
    private fun requestLocationPermission(): Boolean {
        val locationPermissionStatus = ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        mLocationPermissionGranted = locationPermissionStatus == PackageManager.PERMISSION_GRANTED
        if (!mLocationPermissionGranted) {
            ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_FINE_LOCATION
            )
        }
        return mLocationPermissionGranted
    }

    private fun mapSetup() {
        if (mLocationPermissionGranted) {
            mMap?.isMyLocationEnabled = true

            setupLocation()

            // Move MyLocation button to bottom right
            val locationButton = (activity.findViewById<View>(1).parent as View)
                    .findViewById<View>(2)
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 20, 108)
        }
        storesViewModel.stores.observe(this, Observer { storesResource ->
            when (storesResource?.loadState) {
                DataResource.LoadState.SUCCESS -> {
                    updateStoreMarkers(storesResource.items)
                }
                else -> {}
            }
        })
        mMap?.setOnMarkerClickListener(this)
    }

    private fun setupLocation() {
        locationClient.lastLocation.addOnSuccessListener { location ->
            onLocationUpdate(location)
        }.addOnFailureListener { exception ->
            onLocationError(exception)
        }
    }

    private fun onLocationError(exception: Exception) {
        Log.d(TAG, "Current location is null")
        Log.e(TAG, "LocationServices error: $exception")
        mMap?.addMarker(MarkerOptions().position(mDefaultLocation).title("PUCP"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(mDefaultLocation))
    }

    private fun onLocationUpdate(location: Location?) {
        lastKnownLocation = location
        Log.d(TAG, "$lastKnownLocation")
        if (location != null) {
            val currentPos = LatLng(
                    location.latitude,
                    location.longitude
            )
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, DEFAULT_ZOOM))

            // Set circle around current location
            // Radius in meters
            // TODO("update radius to match user level")
            val circle = mLocationCircle
            if (circle == null) {
                mLocationCircle = mMap?.addCircle(CircleOptions()
                        .center(currentPos)
                        .radius(500.0)
                        .strokeWidth(0f)
                        .fillColor(Color.argb(96, 128, 128, 255))
                )
            } else {
                circle.center = currentPos
            }
        } else {
            Log.d(TAG, "Task successful. Current location is null")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnMyLocationButtonClickListener(this)

        if (requestLocationPermission()) {
            mapSetup()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
//        Toast.makeText(activity, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val store = marker.tag as Store
        Log.e(TAG, "StoreSelectCallback is null: ${mOnStoreSelectCallback == null}")
        mOnStoreSelectCallback?.onStoreSelected(store)
        return true
    }
}