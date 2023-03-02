package com.aniket.connect4blood.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.adapters.IncomingRequestListAdapter
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class DonorDashboardHome : Fragment() {

    private lateinit var emptyViewLayout: RelativeLayout
    private lateinit var donorDashboardRecyclerView: RecyclerView
    private lateinit var donorDashboardRecyclerLM: LayoutManager
    private lateinit var donorDashboardRecyclerAdapter: IncomingRequestListAdapter
    private lateinit var mRequestList: ArrayList<Map<String, String>>
    private lateinit var mFusedLocationAPI: FusedLocationProviderClient
    private lateinit var mRequestDBRef: DatabaseReference
    private lateinit var mRequestDBChildEventListener: ChildEventListener
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private var currentUser: User? = null
    private var mCurrentLocation: Location? = null
    private val mPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perm ->
            val granted = perm.entries.all {
                it.value
            }
            if (granted) {
                onPermissionGranted()
            } else {
                onPermissionDeclined()
            }
        }

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_dashboard_home, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        mRequestList = arrayListOf()
        emptyViewLayout = view.findViewById(R.id.emptyViewLayout)
        donorDashboardRecyclerView = view.findViewById(R.id.donorDashboardRecyclerView)
        donorDashboardRecyclerLM = LinearLayoutManager(activity)
        donorDashboardRecyclerAdapter = IncomingRequestListAdapter(activity,
            mRequestList,
            object : IncomingRequestListAdapter.RequestFulfillmentProvider {
                override fun onAccept(requestData: Map<String, String>) {
                    acceptRequest(requestData)
                }
            })

        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    activity as Context,
                    "Please wait..."
                )
        }

        donorDashboardRecyclerView.layoutManager = donorDashboardRecyclerLM
        donorDashboardRecyclerView.adapter = donorDashboardRecyclerAdapter

        return view
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        activity?.let { activity ->
            mFusedLocationAPI = LocationServices.getFusedLocationProviderClient(activity as Context)
            mRequestDBRef = Firebase.database.getReference("/bloodRequests")
            mRequestDBChildEventListener = initBloodRequestListener()

            if (!Utils.getInstance().checkConnectivity(activity)) return
            if (!Utils.getInstance().hasLocationPermissions(activity)) {
                mPermLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }
            getCurrentLocation()
        }
    }

    override fun onStop() {
        super.onStop()
        mRequestDBRef.removeEventListener(mRequestDBChildEventListener)
    }

    private fun onPermissionGranted() {
        getCurrentLocation()
    }

    private fun onPermissionDeclined() {
        activity?.let { activity ->
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    "This app needs to access your location to display the blood donation requests in your locality!"
                )
        }
    }

    private fun updateEmptyResView() {
        if (mRequestList.isEmpty()) {
            emptyViewLayout.visibility = ViewGroup.VISIBLE
        } else {
            emptyViewLayout.visibility = ViewGroup.GONE
        }
    }

    private fun initBloodRequestListener(): ChildEventListener {
        return object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                activity?.let { activity ->
                    val requestId = snapshot.key
                    val request = snapshot.getValue<BloodRequest>()
                    (activity as DonorDashboard)
                        .checkUserPendingRequest(
                            currentUser,
                            object : DonorDashboard.UserPendingRequestProvider {
                                override fun onCheck(
                                    hasPendingRequest: Boolean,
                                    pendingRequestId: String?
                                ) {
                                    if (pendingRequestId == requestId || doesRequestExists(requestId)) return
                                    else {
                                        if (
                                            currentUser != null &&
                                            mCurrentLocation != null &&
                                            (request?.status == BloodRequestStatus.PENDING) &&
                                            (request.bloodGroup == currentUser?.bloodGroup) &&
                                            (request.location != null)
                                        ) {
                                            val requestLocation = Location("Request Location")
                                            requestLocation.latitude = request.location["lat"]!!
                                            requestLocation.longitude = request.location["lng"]!!
                                            val radialDistance =
                                                (mCurrentLocation!!.distanceTo(requestLocation) / 1000)
                                            val searchRadiusLimit =
                                                (request.searchRadius
                                                    ?: AppVals.APP_DEFAULT_MAX_SEARCH_DISTANCE)
                                            if (radialDistance <= searchRadiusLimit) {
                                                val incomingRequest = hashMapOf(
                                                    "id" to requestId.toString(),
                                                    "name" to request.recipientName.toString(),
                                                    "hospitalName" to request.hospitalName.toString(),
                                                    "hospitalAddress" to request.hospitalAddress.toString(),
                                                    "distance" to String.format(
                                                        "%.2f",
                                                        radialDistance
                                                    ),
                                                    "dateTime" to request.requestDateTime.toString(),
                                                    "status" to request.status.toString(),
                                                    "curDonorsPoolSize:" to request.donors?.size.toString(),
                                                    "unitsRequired" to request.unitsRequired.toString()
                                                )
                                                mRequestList.add(incomingRequest)
                                                donorDashboardRecyclerAdapter.notifyItemInserted(
                                                    mRequestList.size - 1
                                                )
                                                updateEmptyResView()
                                            }
                                        }
                                    }
                                }

                            }
                        )
                }

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedRequest = snapshot.getValue<BloodRequest>()
                val updatedRequestKey = snapshot.key
                val curRequest = mRequestList.find { request ->
                    updatedRequestKey == request["id"]
                }
                curRequest ?: return
                if (
                    curRequest["status"] == BloodRequestStatus.PENDING.toString() &&
                    (updatedRequest?.status == BloodRequestStatus.CONFIRMED || updatedRequest?.status == BloodRequestStatus.CANCELLED)
                ) {
                    val currentRequestIdx = mRequestList.indexOf(curRequest)
                    mRequestList.removeAt(currentRequestIdx)
                    donorDashboardRecyclerAdapter.notifyItemRemoved(currentRequestIdx)
                    updateEmptyResView()
                    return
                }
                onChildAdded(
                    snapshot,
                    previousChildName
                )

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                //Not Required

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                //Not Required
            }

            override fun onCancelled(error: DatabaseError) {
                activity?.let { activity ->
                    Utils.getInstance().showToast(
                        activity as Context, "Server Error! Please try again later."
                    )
                }
                Log.d("FirebaseDatabaseError(DonorDashboardHome): ", error.message)
            }

        }
    }

    private fun doesRequestExists(requestId: String?): Boolean {
        return mRequestList.find { map -> requestId == map["id"] } != null
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        mFusedLocationAPI.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener {
                mCurrentLocation = it
                mRequestDBRef.addChildEventListener(mRequestDBChildEventListener)
            }.addOnFailureListener {
                activity?.let { activity ->
                    Utils.getInstance().showToast(
                        activity as Context,
                        "Error! Unable to fetch your current location."
                    )
                }
            }
    }

    private fun acceptRequest(requestData: Map<String, String>) {
        //Check if user has active donation request
        progressDialog?.show()
        (activity as DonorDashboard).checkUserPendingRequest(
            currentUser,
            object : DonorDashboard.UserPendingRequestProvider {
                override fun onCheck(hasPendingRequest: Boolean, pendingRequestId: String?) {
                    progressDialog?.dismiss()
                    if (!hasPendingRequest) {
                        val currentUserId = Firebase.auth.currentUser?.uid.toString()
                        val currentRequestId = requestData["id"]
                        val currentRequestDateTime = requestData["dateTime"]
                        val childUpdates: HashMap<String, Any?> = hashMapOf(
                            "/users/$currentUserId/bloodDonationHistory/$currentRequestId" to currentRequestDateTime
                        )
                        progressDialog?.show()
                        Firebase
                            .database
                            .getReference("/users/$currentUserId")
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue<User>()
                                childUpdates["/bloodRequests/$currentRequestId/donors/$currentUserId"] =
                                    user?.toMap()
                                Firebase
                                    .database
                                    .getReference("/bloodRequests/$currentRequestId/donors")
                                    .get()
                                    .addOnSuccessListener { donorsSnapshot ->
                                        if (donorsSnapshot.childrenCount == (requestData["unitsRequired"]!!.toLong() - 1)) {
                                            childUpdates["/bloodRequests/$currentRequestId/status"] =
                                                BloodRequestStatus.CONFIRMED.toString()
                                        }
                                        Firebase.database.reference.updateChildren(childUpdates)
                                            .addOnSuccessListener {
                                                progressDialog?.dismiss()
                                                //Take to active request page
                                                activity?.findViewById<BottomNavigationView>(R.id.dashboardBottomNav)?.selectedItemId =
                                                    R.id.bottomNavActiveRequest
                                            }
                                            .addOnFailureListener {
                                                progressDialog?.dismiss()
                                                Log.d("FirebaseDatabaseError:", it.toString())
                                                activity?.let { activity ->
                                                    Utils
                                                        .getInstance()
                                                        .showToast(
                                                            activity as Context,
                                                            "Error! Something went wrong, please try again later."
                                                        )
                                                }
                                            }
                                    }
                                    .addOnFailureListener {
                                        progressDialog?.dismiss()
                                        activity?.let { activity ->
                                            Utils
                                                .getInstance()
                                                .showToast(
                                                    activity as Context,
                                                    "Error! Something went wrong, please try again later."
                                                )
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                progressDialog?.dismiss()
                                activity?.let { activity ->
                                    Utils
                                        .getInstance()
                                        .showToast(
                                            activity as Context,
                                            "Error! Something went wrong, please try again later."
                                        )
                                }
                            }

                    } else {
                        activity?.let { activity ->
                            MaterialAlertDialogBuilder(activity)
                                .setTitle("Error")
                                .setMessage("You already have an active request. You can only accept one request at a time.")
                                .setPositiveButton(
                                    "Got it"
                                ) { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                }.create().show()
                        }
                    }
                }
            })

    }

    companion object {
        fun newInstance(currentUser: User?): DonorDashboardHome {
            val fragment = DonorDashboardHome()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }

}




