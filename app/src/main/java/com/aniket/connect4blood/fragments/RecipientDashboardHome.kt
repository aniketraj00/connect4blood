package com.aniket.connect4blood.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorSearch
import com.aniket.connect4blood.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class RecipientDashboardHome : Fragment(), OnMapReadyCallback {

    private lateinit var btnDonorSearch: Button
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mGoogleMap: GoogleMap? = null
    private var mLastKnownLocation: Location? = null
    private var progressDialog: AlertDialog? = null
    private val defaultZoomLevel = 16.0f
    private val mPermLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perm ->
        val granted = perm.entries.all {
            it.value
        }
        if (granted) {
            onPermissionGranted()
        } else {
            onPermissionDeclined()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_recipient_dashboard_home, container, false)
        val mSupportMapFragment: SupportMapFragment? =
            childFragmentManager.findFragmentById(R.id.fragGoogleMap) as SupportMapFragment?

        btnDonorSearch = view.findViewById(R.id.btnDonorSearch)
        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(activity as Context, "Please wait...")
            mFusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(activity as Context)
        }

        btnDonorSearch.setOnClickListener {
            if(!Utils.getInstance().checkConnectivity(activity)) return@setOnClickListener
            if(!Utils.getInstance().hasLocationPermissions(activity)) return@setOnClickListener
            //Get Current Location
            progressDialog?.show()
            mFusedLocationProviderClient
                ?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener {
                    progressDialog?.dismiss()
                    //Pass the location to donor search activity
                    val donorSearchIntent = Intent(activity, DonorSearch::class.java)
                    donorSearchIntent.putExtra("myCurrentLocation", it)
                    startActivity(donorSearchIntent)
                }
                ?.addOnFailureListener {
                    progressDialog?.dismiss()
                    activity.let { activity ->
                        Utils
                            .getInstance()
                            .showToast(
                                activity as Context,
                                "Error! Something went wrong, please try again later."
                            )
                    }
                }
        }
        mSupportMapFragment?.getMapAsync(this)
        return view
    }

    override fun onMapReady(p0: GoogleMap) {
        mGoogleMap = p0
        val locationPermissionGranted =
            Utils
                .getInstance()
                .hasLocationPermissions(activity)
        updateLocationUI(locationPermissionGranted)
        getDeviceLocation(locationPermissionGranted)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI(locationPermissionGranted: Boolean) {
        if (mGoogleMap == null) return
        try {
            if (locationPermissionGranted) {
                mGoogleMap?.isMyLocationEnabled = true
                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mGoogleMap?.isMyLocationEnabled = false
                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                mPermLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(locationPermissionGranted: Boolean) {
        try {
            if (locationPermissionGranted) {
                mFusedLocationProviderClient
                    ?.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    ?.addOnSuccessListener {
                        mLastKnownLocation = it
                        if (mLastKnownLocation != null) {
                            val locationCoordinates = LatLng(
                                mLastKnownLocation!!.latitude,
                                mLastKnownLocation!!.longitude
                            )
                            mGoogleMap?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    locationCoordinates,
                                    defaultZoomLevel
                                )
                            )
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun onPermissionGranted() {
        updateLocationUI(true)
        getDeviceLocation(true)
    }

    private fun onPermissionDeclined() {
        activity?.let { activity ->
            Utils.getInstance().showToast(
                activity as Context,
                "This app needs to access your location to search for the donors in your locality!"
            )
        }
    }


}