package com.aniket.connect4blood.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class DonorSettingsChangePassword : Fragment() {

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null
    private var currentUser: User? = null
    private var appSharedPreferences: SharedPreferences? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater
            .inflate(R.layout.layout_donor_settings_change_pass, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }
        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "Change Password")
            progressDialog = Utils.getInstance().initProgressDialog(
                it as Context, AppVals.PROGRESS_GENERIC
            )
            appSharedPreferences =
                it.getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        }

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        mAuth = FirebaseAuth.getInstance()

        btnSubmit.setOnClickListener(onChangePassword())

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    private fun onChangePassword(): View.OnClickListener {
        return View.OnClickListener {
            if (!verifyInputs()) return@OnClickListener
            changePassword()
        }
    }

    private fun changePassword() {
        if (currentUser == null || appSharedPreferences == null) return
        progressDialog?.show()
        val credential = EmailAuthProvider.getCredential(
            currentUser!!.email!!, etCurrentPassword.text.toString()
        )
        mAuth
            .currentUser
            ?.reauthenticate(credential)
            ?.addOnSuccessListener {
                mAuth
                    .currentUser
                    ?.updatePassword(etNewPassword.text.toString())
                    ?.addOnSuccessListener {
                        progressDialog?.dismiss()
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(it as Context, AppVals.SUCCESS_PASS_UPDATE)
                            Utils
                                .getInstance()
                                .logoutAndClearSharedPrefs(
                                    it,
                                    mAuth,
                                    appSharedPreferences!!
                                )
                        }
                    }
                    ?.addOnFailureListener { e ->
                        progressDialog?.dismiss()
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(it as Context, e.message.toString())
                        }
                    }
            }
            ?.addOnFailureListener { e ->
                progressDialog?.dismiss()
                activity?.let {
                    when (e) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_PASSWORD
                                )
                        }
                        else -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    e.message.toString()
                                )
                        }
                    }
                }
            }
    }

    private fun verifyInputs(): Boolean {
        if (etCurrentPassword.text.isNullOrEmpty() || etNewPassword.text.isNullOrEmpty() || etConfirmPassword.text.isNullOrEmpty()) {
            Utils.getInstance().showToast(activity as Context, AppVals.ERROR_FIELDS_EMPTY)
            return false
        }
        if (etNewPassword.text.toString() != etConfirmPassword.text.toString()) {
            Utils.getInstance().showToast(activity as Context, AppVals.ERROR_PASS_FIELDS_NOT_EQUAL)
            return false
        }
        return true
    }

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsChangePassword {
            val fragment = DonorSettingsChangePassword()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }
}