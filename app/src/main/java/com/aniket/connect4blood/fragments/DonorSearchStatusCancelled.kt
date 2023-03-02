package com.aniket.connect4blood.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.RecipientDashboard
import com.aniket.connect4blood.utils.AppVals

class DonorSearchStatusCancelled : Fragment() {

    private lateinit var btnRequestCancelBackToHome: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_search_cancelled, container, false)
        val requestId = arguments?.getString(AppVals.ARG_KEY_REQUEST_ID)

        btnRequestCancelBackToHome = view.findViewById(R.id.btnRequestCancelBackToHome)

        requestId?.let { id ->
            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.requestDesc,
                    BloodRequestDetails.newInstance(id)
                )
                .commit()
        }

        btnRequestCancelBackToHome.setOnClickListener {
            val homeIntent = Intent(activity, RecipientDashboard::class.java)
            val homeIntentFlags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            homeIntent.flags = homeIntentFlags
            startActivity(homeIntent)
        }

        return view
    }

    companion object {
        fun newInstance(requestId: String): DonorSearchStatusCancelled {
            val fragment = DonorSearchStatusCancelled()
            val args = Bundle()
            args.putString(AppVals.ARG_KEY_REQUEST_ID, requestId)
            fragment.arguments = args
            return fragment
        }
    }
}