package com.aniket.connect4blood.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.auth.FirebaseAuth


class DonorDashboardSettings : Fragment() {

    private lateinit var txtBDHistory: TextView
    private lateinit var txtBDUpdateMobile: TextView
    private lateinit var txtBDUpdateEmail: TextView
    private lateinit var txtBDUpdatePassword: TextView
    private lateinit var txtBDCloseAccount: TextView
    private lateinit var txtBDComplaint: TextView
    private lateinit var txtBDShare: TextView
    private lateinit var txtBDFaqs: TextView
    private lateinit var txtBDLogout: TextView
    private lateinit var mAuth: FirebaseAuth
    private var appSharedPreferences: SharedPreferences? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_dashboard_settings, container, false)
        val currentUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        txtBDHistory = view.findViewById(R.id.txtBDHistory)
        txtBDUpdateMobile = view.findViewById(R.id.txtBDUpdateMobile)
        txtBDUpdateEmail = view.findViewById(R.id.txtBDUpdateEmail)
        txtBDUpdatePassword = view.findViewById(R.id.txtBDUpdatePassword)
        txtBDCloseAccount = view.findViewById(R.id.txtBDCloseAccount)
        txtBDComplaint = view.findViewById(R.id.txtBDComplaint)
        txtBDShare = view.findViewById(R.id.txtBDShare)
        txtBDFaqs = view.findViewById(R.id.txtBDFaqs)
        txtBDLogout = view.findViewById(R.id.txtBDLogout)
        mAuth = FirebaseAuth.getInstance()
        appSharedPreferences =
            activity?.getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, Context.MODE_PRIVATE)


        txtBDHistory.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_HISTORY",
                DonorSettingsHistory.newInstance(currentUser)
            )
        }
        txtBDUpdateMobile.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_UPDATE_MOBILE",
                DonorSettingsUpdateMobile.newInstance(currentUser)
            )
        }
        txtBDUpdateEmail.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_UPDATE_EMAIL",
                DonorSettingsUpdateEmail.newInstance(currentUser)
            )
        }
        txtBDUpdatePassword.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_UPDATE_PASSWORD",
                DonorSettingsChangePassword.newInstance(currentUser)
            )
        }
        txtBDCloseAccount.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_CLOSE_ACCOUNT",
                DonorSettingsCloseAccount.newInstance(currentUser)
            )
        }
        txtBDComplaint.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_RAISE_COMPLAINT",
                DonorSettingsRaiseComplaint.newInstance(currentUser)
            )
        }
        txtBDFaqs.setOnClickListener {
            openFragment(
                "DONOR_SETTINGS_FAQs",
                DonorSettingsFAQs.newInstance()
            )
        }

        txtBDShare.setOnClickListener(onShare())
        txtBDLogout.setOnClickListener(onLogout())


        return view
    }

    private fun onLogout(): View.OnClickListener {
        return View.OnClickListener {
            if (appSharedPreferences == null) return@OnClickListener
            Utils
                .getInstance()
                .logoutAndClearSharedPrefs(
                    requireActivity(),
                    mAuth,
                    appSharedPreferences!!
                )
        }
    }

    private fun onShare(): View.OnClickListener {
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

    companion object {
        fun newInstance(currentUser: User?): DonorDashboardSettings {
            val fragment = DonorDashboardSettings()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }

}