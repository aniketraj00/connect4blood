package com.aniket.connect4blood.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class DonorDashboardRequests : Fragment() {

    private lateinit var shimmerLoader: ShimmerFrameLayout
    private lateinit var emptyViewLayout: RelativeLayout
    private lateinit var txtEmptyLayoutLabel: TextView
    private lateinit var llActiveRequestContainer: LinearLayout
    private lateinit var txtActiveRequestId: TextView
    private lateinit var txtActiveRequestRecipientName: TextView
    private lateinit var txtActiveRequestBloodGroup: TextView
    private lateinit var txtActiveRequestUnits: TextView
    private lateinit var txtActiveRequestHospitalNameAndDistance: TextView
    private lateinit var txtActiveRequestHospitalAddress: TextView
    private lateinit var txtActiveRequestPoolStatus: TextView
    private lateinit var txtActiveRequestStatus: TextView
    private lateinit var btnDonorActiveRequestDirections: MaterialButton
    private lateinit var btnDonorActiveRequestCancel: MaterialButton
    private lateinit var mActiveRequestListener: ValueEventListener
    private var mActiveRequestDBRef: DatabaseReference? = null
    private var mFusedLocationApi: FusedLocationProviderClient? = null
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_dashboard_requests, container, false)
        val currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        shimmerLoader = view.findViewById(R.id.shimmerLoader)
        llActiveRequestContainer = view.findViewById(R.id.llActiveRequestContainer)
        emptyViewLayout = view.findViewById(R.id.emptyViewLayout)
        txtEmptyLayoutLabel = view.findViewById(R.id.txtEmptyLayoutLabel)
        txtActiveRequestId = view.findViewById(R.id.txtActiveRequestId)
        txtActiveRequestRecipientName = view.findViewById(R.id.txtActiveRequestRecipientName)
        txtActiveRequestBloodGroup = view.findViewById(R.id.txtActiveRequestBloodGroup)
        txtActiveRequestUnits = view.findViewById(R.id.txtActiveRequestUnits)
        txtActiveRequestHospitalNameAndDistance =
            view.findViewById(R.id.txtActiveRequestHospitalNameAndDistance)
        txtActiveRequestHospitalAddress = view.findViewById(R.id.txtActiveRequestHospitalAddress)
        txtActiveRequestPoolStatus = view.findViewById(R.id.txtActiveRequestPoolStatus)
        txtActiveRequestStatus = view.findViewById(R.id.txtActiveRequestStatus)
        btnDonorActiveRequestDirections = view.findViewById(R.id.btnDonorActiveRequestDirections)
        btnDonorActiveRequestCancel = view.findViewById(R.id.btnDonorActiveRequestCancel)

        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    activity as Context,
                    "Please wait..."
                )
        }

        activity?.let { activity ->
            mFusedLocationApi = LocationServices.getFusedLocationProviderClient(activity as Context)
            mActiveRequestListener = initActiveRequestValueListener()
            shimmerLoader.startShimmerAnimation()
            (activity as DonorDashboard).checkUserPendingRequest(
                currentUser,
                object : DonorDashboard.UserPendingRequestProvider {
                    override fun onCheck(hasPendingRequest: Boolean, pendingRequestId: String?) {
                        if (hasPendingRequest) {
                            mActiveRequestDBRef = Firebase
                                .database
                                .getReference("/bloodRequests/$pendingRequestId")

                            mActiveRequestDBRef?.addValueEventListener(mActiveRequestListener)

                        } else {
                            shimmerLoader.visibility = ViewGroup.GONE
                            llActiveRequestContainer.visibility = ViewGroup.GONE
                            emptyViewLayout.visibility = ViewGroup.VISIBLE
                        }
                    }

                }
            )
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        shimmerLoader.stopShimmerAnimation()

    }

    override fun onResume() {
        super.onResume()
        shimmerLoader.startShimmerAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActiveRequestDBRef?.removeEventListener(mActiveRequestListener)
    }

    @SuppressLint("MissingPermission")
    private fun initActiveRequestValueListener(): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llActiveRequestContainer.visibility = ViewGroup.GONE
                emptyViewLayout.visibility = ViewGroup.GONE
                shimmerLoader.visibility = ViewGroup.VISIBLE
                val activeRequest = snapshot.getValue<BloodRequest>()
                if (
                    activeRequest?.status == BloodRequestStatus.PENDING ||
                    activeRequest?.status == BloodRequestStatus.CONFIRMED
                ) {
                    //Init views with the request data
                    txtActiveRequestId.text = String.format(
                        activity?.getString(R.string.request_history_id_format).toString(),
                        snapshot.key
                    )
                    txtActiveRequestRecipientName.text = activeRequest.recipientName
                    txtActiveRequestBloodGroup.text = activeRequest.bloodGroup
                    txtActiveRequestUnits.text = String.format(
                        activity?.getString(R.string.blood_units_format).toString(),
                        activeRequest.unitsRequired
                    )
                    txtActiveRequestHospitalNameAndDistance.text = String.format(
                        activity?.getString(R.string.hospital_name_distance_view_format).toString(),
                        activeRequest.hospitalName,
                        "Calculating..."
                    )
                    txtActiveRequestHospitalAddress.text = activeRequest.hospitalAddress
                    txtActiveRequestPoolStatus.text = String.format(
                        activity?.getString(R.string.donors_pool_status_format).toString(),
                        activeRequest.donors?.size,
                        activeRequest.unitsRequired
                    )
                    txtActiveRequestStatus.text = activeRequest.status.toString()
                    activity?.let {
                        when (activeRequest.status) {
                            BloodRequestStatus.PENDING -> txtActiveRequestStatus.setTextColor(
                                ContextCompat.getColor(
                                    it as Context,
                                    R.color.yellow_500
                                )
                            )
                            BloodRequestStatus.CONFIRMED -> txtActiveRequestStatus.setTextColor(
                                ContextCompat.getColor(
                                    it as Context,
                                    R.color.blue_500
                                )
                            )
                            else -> txtActiveRequestStatus.setTextColor(
                                ContextCompat.getColor(
                                    it as Context,
                                    R.color.black
                                )
                            )
                        }
                    }

                    //Set cancel button listener
                    btnDonorActiveRequestCancel.setOnClickListener(
                        onCancel(
                            snapshot.key,
                            Firebase.auth.currentUser?.uid
                        )
                    )
                    if (activeRequest.location != null) {
                        //Set navigation button listener
                        btnDonorActiveRequestDirections.setOnClickListener(
                            onNavigation(
                                activeRequest.location["lat"],
                                activeRequest.location["lng"]
                            )
                        )
                        //Fetch user current location
                        mFusedLocationApi
                            ?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            ?.addOnSuccessListener {
                                val recipientLocation = Location("RecipientLocation")
                                recipientLocation.latitude = activeRequest.location["lat"]!!
                                recipientLocation.longitude = activeRequest.location["lng"]!!
                                val distance =
                                    String.format("%.2f", (it.distanceTo(recipientLocation) / 1000))
                                txtActiveRequestHospitalNameAndDistance.text = String.format(
                                    activity?.getString(R.string.hospital_name_distance_view_format)
                                        .toString(),
                                    activeRequest.hospitalName,
                                    distance
                                )
                            }
                            ?.addOnFailureListener {
                                activity?.let { activity ->
                                    Utils
                                        .getInstance()
                                        .showToast(
                                            activity as Context,
                                            "Error! Unable to fetch your current location."
                                        )
                                }
                                txtActiveRequestHospitalNameAndDistance.text =
                                    String.format(
                                        activity?.getString(R.string.hospital_name_distance_view_format)
                                            .toString(),
                                        activeRequest.hospitalName,
                                        "... Km"
                                    )
                            }
                    } else {
                        activity?.let { activity ->
                            Utils
                                .getInstance()
                                .showToast(
                                    activity as Context,
                                    "Error! Recipient location is unknown."
                                )
                        }
                        txtActiveRequestHospitalNameAndDistance.text = String.format(
                            activity?.getString(R.string.hospital_name_distance_view_format)
                                .toString(),
                            activeRequest.hospitalName,
                            "... Km"
                        )
                    }
                    llActiveRequestContainer.visibility = ViewGroup.VISIBLE
                } else {
                    emptyViewLayout.visibility = ViewGroup.VISIBLE
                    txtEmptyLayoutLabel.text = getText(R.string.txt_no_active_request)
                }
                shimmerLoader.visibility = ViewGroup.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                //Setup database error view
                activity?.let { activity ->
                    Utils
                        .getInstance()
                        .showToast(
                            activity as Context,
                            "Server Error! Please try again later."
                        )
                }
            }

        }
    }

    private fun onCancel(requestId: String?, userId: String?): View.OnClickListener {
        return View.OnClickListener {
            //Setup request cancellation feature
            if(userId == null || requestId == null) return@OnClickListener
            activity?.let { activity ->
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to cancel this request?")
                    .setPositiveButton("Yes")
                    { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        progressDialog?.show()
                        (activity as DonorDashboard).cancelUserPendingRequest(
                            userId,
                            requestId,
                            object: DonorDashboard.UserPendingRequestCancellationProvider {
                                override fun onCancelSuccess() {
                                    progressDialog?.dismiss()
                                    llActiveRequestContainer.visibility = ViewGroup.GONE
                                    emptyViewLayout.visibility = ViewGroup.VISIBLE
                                }

                                override fun onCancelFailure(exception: Exception) {
                                    progressDialog?.dismiss()
                                    Utils
                                        .getInstance()
                                        .showToast(
                                            activity as Context,
                                            AppVals.ERROR_GENERIC
                                        )
                                }
                            }
                        )
                    }
                    .setNegativeButton("No")
                    { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun onNavigation(lat: Double?, lng: Double?): View.OnClickListener {
        return View.OnClickListener {
            if (lat == null || lng == null) return@OnClickListener
            val gMapIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gMapIntentUri)
            if (mapIntent.resolveActivity(activity?.packageManager!!) != null) {
                startActivity(mapIntent)
            }
        }
    }

    companion object {
        fun newInstance(currentUser: User?): DonorDashboardRequests {
            val fragment = DonorDashboardRequests()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }
}