package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.adapters.DonorsListAdapter
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DonorSearchStatusConfirmed : Fragment() {

    private lateinit var waitingForDonorLabel: TextView
    private lateinit var donorListLabel: TextView
    private lateinit var recyclerDonorsList: RecyclerView
    private lateinit var mDonorsList: ArrayList<Map<String, String>>
    private var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_search_confirmed, container, false)
        val requestId = arguments?.getString(AppVals.ARG_KEY_REQUEST_ID)

        waitingForDonorLabel = view.findViewById(R.id.txtDonorListSubHead)
        recyclerDonorsList = view.findViewById(R.id.recyclerDonorsList)
        donorListLabel = view.findViewById(R.id.txtDonorListHead)
        mDonorsList = arrayListOf()
        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(activity as Context, "Please wait...")
        }

        requestId?.let { id ->
            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.requestDesc,
                    BloodRequestDetails.newInstance(id)
                )
                .commit()

            Firebase
                .database
                .getReference("/bloodRequests/$id/donors")
                .get()
                .addOnSuccessListener {
                    progressDialog?.dismiss()
                    if (it.hasChildren()) {
                        //Update UI
                        waitingForDonorLabel.visibility = View.GONE
                        val params = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.addRule(RelativeLayout.BELOW, R.id.requestDesc)
                        params.bottomMargin = 14
                        params.leftMargin = 10
                        donorListLabel.layoutParams = params
                        //Prepare donors list
                        it.children.forEach(action = { child ->
                            val donor = hashMapOf<String, String>()
                            donor["id"] = child.key.toString()
                            child.children.forEach(action = { donorInfo ->
                                donor[donorInfo.key.toString()] =
                                    donorInfo.value.toString()
                            })
                            mDonorsList.add(donor)
                        })
                        //Setup the recycler view
                        recyclerDonorsList.layoutManager = LinearLayoutManager(activity)
                        recyclerDonorsList.adapter = DonorsListAdapter(activity, mDonorsList)
                    } else {
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
                .addOnFailureListener {
                    progressDialog?.dismiss()
                }
        }


        return view
    }

    companion object {
        fun newInstance(requestId: String): DonorSearchStatusConfirmed {
            val fragment = DonorSearchStatusConfirmed()
            val args = Bundle()
            args.putString(AppVals.ARG_KEY_REQUEST_ID, requestId)
            fragment.arguments = args
            return fragment
        }
    }


}