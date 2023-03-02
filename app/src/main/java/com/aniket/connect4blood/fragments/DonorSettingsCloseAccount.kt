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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DonorSettingsCloseAccount : Fragment() {

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var btnCloseAccount: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null
    private var appSharedPreferences: SharedPreferences? = null
    private var currentUser: User? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_donor_settings_close_account, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "Deactivate Account")
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

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        btnCloseAccount = view.findViewById(R.id.btnCloseAccount)
        mAuth = FirebaseAuth.getInstance()

        btnCloseAccount.setOnClickListener(onCloseAccount())

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    private fun onCloseAccount(): View.OnClickListener {
        return View.OnClickListener { _ ->
            if (appSharedPreferences == null || currentUser == null || mAuth.currentUser == null) return@OnClickListener
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
                    closeAccount()
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

    private fun closeAccount() {
        progressDialog?.show()
        activity?.let {
            (it as DonorDashboard).checkUserPendingRequest(
                currentUser,
                object : DonorDashboard.UserPendingRequestProvider {
                    override fun onCheck(
                        hasPendingRequest: Boolean,
                        pendingRequestId: String?
                    ) {
                        if (hasPendingRequest && pendingRequestId != null) {
                            activity?.let { it1 ->
                                (it1 as DonorDashboard).cancelUserPendingRequest(
                                    mAuth.currentUser!!.uid,
                                    pendingRequestId,
                                    object : DonorDashboard.UserPendingRequestCancellationProvider {
                                        override fun onCancelSuccess() {
                                            val childUpdates: Map<String, Any?> = hashMapOf(
                                                "/users/${mAuth.currentUser!!.uid}" to null,
                                                "/archive/users/${mAuth.currentUser!!.uid}" to currentUser
                                            )
                                            Firebase
                                                .database
                                                .reference
                                                .updateChildren(childUpdates)
                                                .addOnSuccessListener {
                                                    mAuth
                                                        .currentUser!!
                                                        .delete()
                                                        .addOnSuccessListener {
                                                            progressDialog?.dismiss()
                                                            activity?.let { it2 ->
                                                                Utils
                                                                    .getInstance()
                                                                    .showToast(
                                                                        it2 as Context,
                                                                        AppVals.SUCCESS_ACCOUNT_CLOSURE
                                                                    )
                                                                Utils
                                                                    .getInstance()
                                                                    .logoutAndClearSharedPrefs(
                                                                        it2,
                                                                        mAuth,
                                                                        appSharedPreferences!!
                                                                    )
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            progressDialog?.dismiss()
                                                            activity?.let { it3 ->
                                                                Utils
                                                                    .getInstance()
                                                                    .showToast(
                                                                        it3 as Context,
                                                                        e.message.toString()
                                                                    )
                                                            }
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    progressDialog?.dismiss()
                                                    activity?.let { it4 ->
                                                        Utils
                                                            .getInstance()
                                                            .showToast(
                                                                it4 as Context,
                                                                e.message.toString()
                                                            )
                                                    }
                                                }
                                        }

                                        override fun onCancelFailure(exception: Exception) {
                                            progressDialog?.dismiss()
                                            activity?.let { activity ->
                                                Utils
                                                    .getInstance()
                                                    .showToast(
                                                        activity as Context,
                                                        AppVals.ERROR_GENERIC
                                                    )
                                            }
                                        }

                                    }
                                )
                            }
                        }
                    }

                }
            )
        }
    }

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsCloseAccount {
            val fragment = DonorSettingsCloseAccount()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }
}