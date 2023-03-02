package com.aniket.connect4blood.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class DonorSettingsUpdateEmail : Fragment() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var btnVerify: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mUserDBRef: DatabaseReference
    private var progressDialog: AlertDialog? = null
    private var appSharedPreferences: SharedPreferences? = null
    private var currentUser: User? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_donor_settings_update_email, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "Update Email")
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    it as Context,
                    AppVals.PROGRESS_GENERIC
                )
            appSharedPreferences = it.getSharedPreferences(
                AppVals.APP_SHARED_PREFS_NAME,
                Context.MODE_PRIVATE
            )
        }

        etEmail = view.findViewById(R.id.etEmail)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        btnVerify = view.findViewById(R.id.btnVerify)

        mAuth = FirebaseAuth.getInstance()
        mUserDBRef = FirebaseDatabase
            .getInstance()
            .getReference("/users/${mAuth.currentUser?.uid}")

        btnVerify.setOnClickListener(onUpdateEmail())

        return view
    }

    private fun onUpdateEmail(): View.OnClickListener {
        return View.OnClickListener {
            if (currentUser == null || mAuth.currentUser == null || appSharedPreferences == null) return@OnClickListener
            if (etEmail.text.isNullOrEmpty()) {
                Utils
                    .getInstance()
                    .showToast(
                        activity as Context,
                        AppVals.ERROR_EMAIL_EMPTY
                    )
                return@OnClickListener
            }
            if (etCurrentPassword.text.isNullOrEmpty()) {
                Utils
                    .getInstance()
                    .showToast(
                        activity as Context,
                        AppVals.ERROR_EMPTY_CUR_PASS
                    )
                return@OnClickListener
            }
            progressDialog?.show()
            val credential = EmailAuthProvider.getCredential(
                currentUser!!.email!!,
                etCurrentPassword.text.toString()
            )
            mAuth
                .currentUser!!
                .reauthenticate(credential)
                .addOnSuccessListener {
                    progressDialog?.dismiss()
                    updateEmail()
                }
                .addOnFailureListener { e ->
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

    }

    private fun updateEmail() {
        progressDialog?.show()
        progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text =
            AppVals.UPDATING_EMAIL
        mAuth
            .currentUser
            ?.updateEmail(etEmail.text.toString())
            ?.addOnSuccessListener { _ ->
                mUserDBRef
                    .child("/email")
                    .setValue(etEmail.text.toString())
                    .addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                            progressDialog
                                ?.findViewById<TextView>(R.id.txtProgressDialogMsg)
                                ?.text = AppVals.SEND_VERIFY_EMAIL
                            mAuth
                                .currentUser
                                ?.sendEmailVerification()
                                ?.addOnCompleteListener {
                                    progressDialog?.dismiss()
                                    if (it.isSuccessful) {
                                        activity?.let { activity ->
                                            Utils
                                                .getInstance()
                                                .showToast(
                                                    activity as Context,
                                                    AppVals.SENT_VERIFY_EMAIL
                                                )
                                            Utils
                                                .getInstance()
                                                .logoutAndClearSharedPrefs(
                                                    activity,
                                                    mAuth,
                                                    appSharedPreferences!!,
                                                )
                                        }
                                    }
                                }
                        } else {
                            progressDialog?.dismiss()
                            activity?.let {
                                Utils
                                    .getInstance()
                                    .showToast(
                                        it as Context,
                                        AppVals.ERROR_GENERIC
                                    )
                            }
                        }
                    }
            }
            ?.addOnFailureListener { e ->
                progressDialog?.dismiss()
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_INVALID_EMAIL
                                )
                        }
                    }
                    is FirebaseAuthUserCollisionException -> {
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_USER_COLLISION_EMAIL
                                )
                        }
                    }
                    is FirebaseAuthInvalidUserException -> {
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_USER_DISABLED_EMAIL
                                )
                        }
                    }
                    is FirebaseAuthRecentLoginRequiredException -> {
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_RECENT_LOGIN_REQUIRED
                                )
                        }
                    }
                    else -> {
                        activity?.let {
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

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsUpdateEmail {
            val fragment = DonorSettingsUpdateEmail()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }
}