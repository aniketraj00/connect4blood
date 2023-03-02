package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.adapters.DonationHistoryListAdapter
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class DonorSettingsHistory: Fragment() {

    private lateinit var emptyView: RelativeLayout
    private lateinit var recyclerDonationHistory: RecyclerView
    private lateinit var recyclerDonationHistoryAdapter: DonationHistoryListAdapter
    private lateinit var recyclerDonationHistoryLM: LinearLayoutManager
    private lateinit var mList: ArrayList<HashMap<String, Any?>>
    private lateinit var mRequestDBRef: DatabaseReference
    private var progressDialog: AlertDialog? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = layoutInflater.inflate(R.layout.layout_donor_settings_history, container, false)
        val currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "History")
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    it as Context,
                    "Loading History"
                )
        }

        mList = arrayListOf()
        emptyView = view.findViewById(R.id.layoutEmptyRes)
        recyclerDonationHistory = view.findViewById(R.id.recyclerDonationHistory)
        recyclerDonationHistoryLM = LinearLayoutManager(activity)
        recyclerDonationHistoryAdapter = DonationHistoryListAdapter(activity, mList)
        mRequestDBRef = Firebase
            .database
            .getReference("/bloodRequests")

        recyclerDonationHistory.layoutManager = recyclerDonationHistoryLM
        recyclerDonationHistory.adapter = recyclerDonationHistoryAdapter

        currentUser?.let { user ->
            getDonationHistory(user)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    private fun getDonationHistory(user: User) {
        if(user.bloodDonationHistory == null || user.bloodDonationHistory.isEmpty()) {
            emptyView.visibility = ViewGroup.VISIBLE
            return
        }
        progressDialog?.show()
        var targetRequestCount = 0
        var totalRequestCount = 0
        user.bloodDonationHistory.forEach(action = { requestMap ->
            val requestId = requestMap.key
            mRequestDBRef
                .child("/$requestId")
                .get()
                .addOnSuccessListener { dataSnapshot ->
                    totalRequestCount++
                    val bloodRequest = dataSnapshot.getValue<BloodRequest>()
                    if(bloodRequest?.status == BloodRequestStatus.COMPLETED ||
                        bloodRequest?.status ==  BloodRequestStatus.CANCELLED
                    ) {
                        targetRequestCount++
                        val bloodRequestMap = hashMapOf<String, Any?>(
                            "id" to requestId,
                            "name" to bloodRequest.recipientName,
                            "date" to bloodRequest.requestDateTime,
                            "status" to bloodRequest.status
                        )
                        mList.add(bloodRequestMap)
                        recyclerDonationHistoryAdapter.notifyItemInserted(mList.size)
                    }

                    if(totalRequestCount == user.bloodDonationHistory.size) {
                        progressDialog?.dismiss()
                        if(targetRequestCount == 0) {
                            emptyView.visibility = ViewGroup.VISIBLE
                        }
                    }

                }
                .addOnFailureListener {
                    if(progressDialog?.isShowing == true) progressDialog?.dismiss()
                }
        })
    }

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsHistory {
            val fragment = DonorSettingsHistory()
            val args = Bundle()
            //PUT FRAGMENT CONSTRUCTOR ARGUMENTS HERE
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            //ATTACH THE ARGUMENTS BUNDLE TO THE FRAGMENT
            fragment.arguments = args
            //RETURN THE FRAGMENT
            return fragment
        }
    }



}