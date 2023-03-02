package com.aniket.connect4blood.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals

class DonorSearchStatusCompleted : Fragment() {

    private lateinit var btnRequestFinishFeedback: Button
    private lateinit var btnRequestFinishBackToHome: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_search_completed, container, false)
        val requestId = arguments?.getString(AppVals.ARG_KEY_REQUEST_ID)

        btnRequestFinishFeedback = view.findViewById(R.id.btnRequestFinishFeedback)
        btnRequestFinishBackToHome = view.findViewById(R.id.btnRequestFinishBackToHome)

        if(requestId != null) {
            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.requestDesc,
                    BloodRequestDetails.newInstance(requestId)
                )
                .commit()
        }
        return view
    }

    companion object {
        fun newInstance(requestId: String): DonorSearchStatusCompleted {
            val fragment = DonorSearchStatusCompleted()
            val args = Bundle()
            args.putString(AppVals.ARG_KEY_REQUEST_ID, requestId)
            fragment.arguments = args
            return fragment
        }
    }
}