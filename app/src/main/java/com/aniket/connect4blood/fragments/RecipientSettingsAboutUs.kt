package com.aniket.connect4blood.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.RecipientDashboard
import com.aniket.connect4blood.utils.Utils

class RecipientSettingsAboutUs: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as RecipientDashboard).supportActionBar, "About Us")
        }
        return inflater.inflate(R.layout.layout_about_us, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as RecipientDashboard).supportActionBar)
    }

    companion object {
        fun newInstance(): RecipientSettingsAboutUs {
            val fragment = RecipientSettingsAboutUs()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}