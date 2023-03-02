package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class BloodRequestDetails: Fragment() {

    private lateinit var llBloodRequestDescContent: LinearLayout
    private lateinit var shimmerLoader: ShimmerFrameLayout
    private lateinit var txtBloodRequestId: TextView
    private lateinit var txtBloodRequestDate: TextView
    private lateinit var txtBloodRequestRecipientName: TextView
    private lateinit var txtBloodRequestBloodGroup: TextView
    private lateinit var txtBloodRequestReqUnits: TextView
    private lateinit var txtBloodRequestStatus: TextView
    private lateinit var btnBloodRequestFinish: Button
    private lateinit var btnBloodRequestCancel: Button
    private lateinit var mBloodRequestDBRef: DatabaseReference
    private var progressDialog: AlertDialog? = null
    private var requestId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_blood_request_details, container, false)
        requestId = arguments?.getString(AppVals.ARG_KEY_REQUEST_ID)

        shimmerLoader = view.findViewById(R.id.shimmerLoader)
        llBloodRequestDescContent = view.findViewById(R.id.llBloodRequestDescContent)
        txtBloodRequestId = view.findViewById(R.id.txtBloodRequestId)
        txtBloodRequestDate = view.findViewById(R.id.txtBloodRequestDate)
        txtBloodRequestRecipientName = view.findViewById(R.id.txtBloodRequestRecipientName)
        txtBloodRequestBloodGroup = view.findViewById(R.id.txtBloodRequestBloodGroup)
        txtBloodRequestReqUnits = view.findViewById(R.id.txtBloodRequestReqUnits)
        txtBloodRequestStatus = view.findViewById(R.id.txtBloodRequestStatus)
        btnBloodRequestFinish = view.findViewById(R.id.btnBloodRequestFinish)
        btnBloodRequestCancel = view.findViewById(R.id.btnBloodRequestCancel)

        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    activity as Context,
                    "Please wait..."
                )
        }

        mBloodRequestDBRef = Firebase
            .database
            .getReference("/bloodRequests")

        requestId?.let { id ->
            mBloodRequestDBRef
                .child(id)
                .get()
                .addOnSuccessListener {
                    //Insert the data into the target views
                    val request: BloodRequest? = it.getValue<BloodRequest>()
                    txtBloodRequestId.text = requestId
                    txtBloodRequestDate.text = request?.requestDateTime
                    txtBloodRequestRecipientName.text = request?.recipientName
                    txtBloodRequestBloodGroup.text = request?.bloodGroup
                    txtBloodRequestReqUnits.text = request?.unitsRequired.toString()
                    txtBloodRequestStatus.text =
                        request?.status.toString().replaceFirstChar(Char::titlecase)

                    //Update UI accordingly
                    activity?.let { activity ->
                        when (request?.status) {
                            BloodRequestStatus.PENDING -> {
                                txtBloodRequestStatus.setTextColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.yellow_500
                                    )
                                )
                                btnBloodRequestFinish.visibility = View.GONE
                            }
                            BloodRequestStatus.CONFIRMED -> {
                                txtBloodRequestStatus.setTextColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.blue_500
                                    )
                                )
                            }
                            BloodRequestStatus.COMPLETED -> {
                                txtBloodRequestStatus.setTextColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.green_500
                                    )
                                )
                                btnBloodRequestFinish.visibility = View.GONE
                                btnBloodRequestCancel.visibility = View.GONE
                            }
                            BloodRequestStatus.CANCELLED -> {
                                txtBloodRequestStatus.setTextColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.red_500
                                    )
                                )
                                btnBloodRequestFinish.visibility = View.GONE
                                btnBloodRequestCancel.visibility = View.GONE
                            }
                            else -> {
                                txtBloodRequestStatus.setTextColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.black
                                    )
                                )
                            }
                        }
                    }
                    //Update the donors list size variable
                    setFragmentResult(
                        AppVals.ARG_KEY_DONOR_LIST_SIZE_GET,
                        bundleOf(AppVals.ARG_KEY_DONOR_LIST_SIZE to hashMapOf<String, Any?>(
                            "value" to request?.unitsRequired
                        ))
                    )

                    //Add the cancel and finish button listener
                    btnBloodRequestFinish.setOnClickListener(onRequestFinish())
                    btnBloodRequestCancel.setOnClickListener(onRequestCancel())

                    //Display the layout and hide the loader
                    shimmerLoader.visibility = ViewGroup.GONE
                    llBloodRequestDescContent.visibility = ViewGroup.VISIBLE
                }
                .addOnFailureListener {
                    Log.d("BloodRequestStatusError: ", it.toString())
                    activity?.let { activity ->
                        Utils
                            .getInstance()
                            .showToast(
                                activity as Context,
                                "Error! Something went wrong."
                            )
                    }
                    setFragmentResult(
                        AppVals.ARG_KEY_DONOR_LIST_SIZE_GET,
                        bundleOf(AppVals.ARG_KEY_DONOR_LIST_SIZE to hashMapOf<String, Any?>(
                            "error" to it.message
                        ))
                    )
                }
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

    private fun initRequestStatusChangeDialog(
        dialogMsg: String,
        requestId: String?,
        requestStatus: BloodRequestStatus
    ): AlertDialog? {
        activity?.let { activity ->
            return MaterialAlertDialogBuilder(activity)
                .setTitle("Confirm")
                .setMessage(dialogMsg)
                .setPositiveButton("YES") { dialog, _ ->
                    requestId?.let { id ->
                        progressDialog?.show()
                        Firebase
                            .database
                            .getReference("/bloodRequests/$id/status")
                            .setValue(requestStatus)
                            .addOnCompleteListener {
                                progressDialog?.dismiss()
                                dialog.dismiss()
                            }
                    }
                }
                .setNegativeButton("NO") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
        }
        return null
    }

    private fun onRequestCancel(): View.OnClickListener {
        return View.OnClickListener {
            requestId?.let { id ->
                initRequestStatusChangeDialog(
                    "Are you sure you want to cancel this request?",
                    id,
                    BloodRequestStatus.CANCELLED
                )?.show()
            }
        }
    }

    private fun onRequestFinish(): View.OnClickListener {
        return View.OnClickListener {
            requestId?.let { id ->
                initRequestStatusChangeDialog(
                    "Are you sure you want to mark this request as completed?",
                    id,
                    BloodRequestStatus.COMPLETED
                )?.show()
            }
        }
    }

    companion object {
        fun newInstance(projectId: String): BloodRequestDetails {
            val fragment = BloodRequestDetails()
            val args = Bundle()
            args.putString(AppVals.ARG_KEY_REQUEST_ID, projectId)
            fragment.arguments = args
            return fragment
        }
    }

}