package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aniket.connect4blood.R
import com.aniket.connect4blood.adapters.RequestHistoryListAdapter
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class RecipientDashboardHistory : Fragment() {

    private lateinit var recyclerRecipientHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyRes: ViewGroup
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_recipient_dashboard_history, container, false)

        layoutEmptyRes = view.findViewById(R.id.layoutEmptyRes)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerRecipientHistory = view.findViewById(R.id.recyclerRecipientHistory)

        mAuth = FirebaseAuth.getInstance()
        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(activity as Context, "Please wait...")
        }

        progressDialog?.show()

        //Load all the blood request history of the current user
        loadRequestHistory(object : BloodRequestHistoryProvider {
            override fun onSuccess(history: List<Map<String, String>>) {
                progressDialog?.dismiss()
                if (history.isEmpty()) {
                    layoutEmptyRes.visibility = ViewGroup.VISIBLE
                } else {
                    recyclerRecipientHistory.layoutManager = LinearLayoutManager(activity)
                    recyclerRecipientHistory.adapter =
                        RequestHistoryListAdapter(activity, history)
                }
            }

            override fun onFailure(ex: Exception?) {
                progressDialog?.dismiss()
                Log.d("FirebaseDatabaseError: ", ex.toString())
                activity?.let { activity ->
                    Utils
                        .getInstance()
                        .showToast(
                            activity as Context,
                            "Error! Something went wrong."
                        )
                }
            }
        })

        //Implement swipe refresh functionality to refresh the history data upon user request
        swipeRefresh.setOnRefreshListener {
            layoutEmptyRes.visibility = ViewGroup.GONE
            swipeRefresh.isRefreshing = true
            loadRequestHistory(object : BloodRequestHistoryProvider {
                override fun onSuccess(history: List<Map<String, String>>) {
                    swipeRefresh.isRefreshing = false
                    if (history.isEmpty()) {
                        layoutEmptyRes.visibility = ViewGroup.VISIBLE
                    } else {
                        recyclerRecipientHistory.layoutManager = LinearLayoutManager(activity)
                        recyclerRecipientHistory.adapter =
                            RequestHistoryListAdapter(activity, history)
                    }
                }

                override fun onFailure(ex: Exception?) {
                    swipeRefresh.isRefreshing = false
                    Log.d("FirebaseDatabaseError: ", ex.toString())
                    activity?.let { activity ->
                        Utils
                            .getInstance()
                            .showToast(
                                activity as Context,
                                ex?.message ?: "Error! Something went wrong."
                            )
                    }
                }

            })
        }

        return view
    }

    private fun loadRequestHistory(mCallback: BloodRequestHistoryProvider) {
        Firebase
            .database
            .getReference("/users/${mAuth.currentUser?.uid}/bloodRequestHistory")
            .get()
            .addOnSuccessListener {
                val mRequestList = ArrayList<Map<String, String>>()
                if (it.hasChildren()) {
                    it.children.forEach { request ->
                        val id = request.key.toString()
                        Firebase
                            .database
                            .getReference("/bloodRequests/$id")
                            .get()
                            .addOnSuccessListener { requestData ->
                                val bloodRequest = requestData.getValue<BloodRequest>()
                                mRequestList.add(
                                    hashMapOf(
                                        "id" to requestData.key.toString(),
                                        "blood_group" to bloodRequest?.bloodGroup.toString(),
                                        "req_units" to bloodRequest?.unitsRequired.toString(),
                                        "status" to bloodRequest?.status.toString()
                                    )
                                )
                                if (it.childrenCount == mRequestList.size.toLong()) {
                                    mCallback.onSuccess(mRequestList)
                                }
                            }
                            .addOnFailureListener { ex ->
                                mCallback.onFailure(ex)
                            }
                    }
                } else {
                    mCallback.onSuccess(mRequestList)
                }
            }
            .addOnFailureListener {
                mCallback.onFailure(it)
            }
    }

    interface BloodRequestHistoryProvider {
        fun onSuccess(history: List<Map<String, String>>)
        fun onFailure(ex: Exception?)
    }
}