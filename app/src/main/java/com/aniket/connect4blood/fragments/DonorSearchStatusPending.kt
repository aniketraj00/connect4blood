package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout

class DonorSearchStatusPending : Fragment(){

    private lateinit var shimmerLoader: ShimmerFrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_search_pending, container, false)
        val requestId = arguments?.getString(AppVals.ARG_KEY_REQUEST_ID)

        shimmerLoader = view.findViewById(R.id.shimmerLoader)

        requestId?.let { id ->
            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.requestDesc,
                    BloodRequestDetails.newInstance(id)
                )
                .commit()
        }

        childFragmentManager
            .setFragmentResultListener(
                AppVals.ARG_KEY_DONOR_LIST_SIZE_GET,
                this,
                initFragmentResultListener()
            )

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

    @Suppress("DEPRECATION")
    private fun initFragmentResultListener(): FragmentResultListener {
        return FragmentResultListener { requestKey, result ->
            when(requestKey) {
                AppVals.ARG_KEY_DONOR_LIST_SIZE_GET -> {
                    val data = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.getSerializable(AppVals.ARG_KEY_DONOR_LIST_SIZE, HashMap::class.java)
                    } else {
                        result.getSerializable(AppVals.ARG_KEY_DONOR_LIST_SIZE) as HashMap<*, *>?
                    }
                    if(data != null && data.containsKey("value")) {
                        val size = data["value"] as Int?
                        onDonorsListSizeGetSuccess(size)
                    } else if(data != null && data.containsKey("error")) {
                        val errMsg = data["error"] as String?
                        onDonorsListSizeGetFailure(errMsg)
                    }
                }
            }
        }
    }

    private fun onDonorsListSizeGetSuccess(size: Int?) {
        val shimmerElementsContainer = LinearLayout(activity as Context)
        shimmerElementsContainer.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        shimmerElementsContainer.orientation = LinearLayout.VERTICAL
        size?.let { count ->
            for (i in 1..count) {
                val shimmerEl =
                    View.inflate(activity, R.layout.layout_donors_placeholder, null)
                shimmerElementsContainer.addView(shimmerEl)
            }
        }
        shimmerLoader.addView(shimmerElementsContainer)
    }

    private fun onDonorsListSizeGetFailure(errMsg: String?) {
        activity?.let { activity ->
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    errMsg ?: "Server Error! Please try again later."
                )
        }
    }

    companion object {
        fun newInstance(requestId: String): DonorSearchStatusPending {
            val fragment = DonorSearchStatusPending()
            val args = Bundle()
            args.putString(AppVals.ARG_KEY_REQUEST_ID, requestId)
            fragment.arguments = args
            return fragment
        }
    }
}
