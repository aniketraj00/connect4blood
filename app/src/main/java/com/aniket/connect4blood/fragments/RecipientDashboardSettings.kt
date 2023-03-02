package com.aniket.connect4blood.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.auth.FirebaseAuth

class RecipientDashboardSettings : Fragment() {

    private lateinit var txtSettingsLogout: TextView
    private lateinit var txtSettingsFAQs: TextView
    private lateinit var txtSettingsRefer: TextView
    private lateinit var txtSettingsRaiseComplaint: TextView
    private lateinit var txtSettingsAbout: TextView
    private lateinit var mAuth: FirebaseAuth
    private var appSharedPreferences: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_recipient_dashboard_settings, container, false)

        txtSettingsFAQs = view.findViewById(R.id.txtSettingsFAQs)
        txtSettingsLogout = view.findViewById(R.id.txtSettingsLogout)
        txtSettingsRefer = view.findViewById(R.id.txtSettingsRefer)
        txtSettingsRaiseComplaint = view.findViewById(R.id.txtSettingsRaiseComplaint)
        txtSettingsAbout = view.findViewById(R.id.txtSettingsAbout)
        appSharedPreferences = activity?.getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()

        txtSettingsFAQs.setOnClickListener {
            openFragment(
                "RECIPIENT_SETTINGS_FAQS",
                RecipientSettingsFAQs.newInstance()
            )
        }
        txtSettingsRaiseComplaint.setOnClickListener {
            openFragment(
                "RECIPIENT_SETTINGS_RAISE_COMPLAINT",
                RecipientSettingsRaiseComplaint.newInstance()
            )
        }
        txtSettingsAbout.setOnClickListener {
            openFragment(
                "RECIPIENT_SETTINGS_ABOUT_US",
                RecipientSettingsAboutUs.newInstance()
            )
        }
        txtSettingsLogout.setOnClickListener(onLogout())
        txtSettingsRefer.setOnClickListener(onRefer())

        return view
    }

    private fun onRefer(): View.OnClickListener {
        return View.OnClickListener {
            Utils
                .getInstance()
                .shareApp(requireContext())
        }
    }

    private fun openFragment(title: String, fragment: Fragment) {
        activity
            ?.supportFragmentManager
            ?.beginTransaction()
            ?.replace(R.id.dashboardContent, fragment)
            ?.addToBackStack(title)
            ?.commit()
    }

    private fun onLogout(): View.OnClickListener {
        return View.OnClickListener {
            if(appSharedPreferences == null) return@OnClickListener
            Utils
                .getInstance()
                .logoutAndClearSharedPrefs(
                    requireActivity(),
                    mAuth,
                    appSharedPreferences!!
                )
        }
    }
}