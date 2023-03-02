package com.aniket.connect4blood.fragments

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.aniket.connect4blood.utils.Utils.OTPVerifyDialogCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit


class DonorSettingsUpdateMobile : Fragment() {

    private lateinit var etMobileNo: TextInputEditText
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var btnSendOTP: Button
    private lateinit var mVerificationCallback: OnVerificationStateChangedCallbacks
    private lateinit var mVerificationResendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var mVerificationId: String
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null
    private var otpVerifyDialog: BottomSheetDialog? = null
    private var currentUser: User? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_donor_settings_update_mobile, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        etMobileNo = view.findViewById(R.id.etMobileNo)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        btnSendOTP = view.findViewById(R.id.btnSendOTP)

        mAuth = FirebaseAuth.getInstance()

        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "Update Mobile")

            progressDialog = Utils
                .getInstance()
                .initProgressDialog(it as Context, AppVals.PROGRESS_GENERIC)

            otpVerifyDialog = Utils
                .getInstance()
                .initOTPVerifyDialog(it as Context, object : OTPVerifyDialogCallback {
                    override fun onOTPSubmit(otpInputField: TextInputEditText?) {
                        if (!isValidOTP(otpInputField?.text)) return
                        changePhoneNumber(
                            PhoneAuthProvider
                                .getCredential(
                                    mVerificationId,
                                    otpInputField?.text.toString()
                                )
                        )
                    }

                    override fun onResendOTP() {
                        activity?.let {
                            resendOTP(
                                "+91${etMobileNo.text.toString()}",
                                mVerificationResendToken
                            )
                        }
                    }
                })
        }

        mVerificationCallback = initVerificationCallback()
        btnSendOTP.setOnClickListener(onSendOTP())

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    private fun initVerificationCallback(): OnVerificationStateChangedCallbacks {
        return object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                changePhoneNumber(credential)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                progressDialog?.dismiss()
                otpVerifyDialog?.dismiss()
                activity.let {
                    when (p0) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_INVALID_PHONE_NO
                                )
                        }
                        is FirebaseTooManyRequestsException -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_TOO_MANY_REQUEST
                                )
                        }
                        else -> {
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

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                mVerificationId = verificationId
                progressDialog?.dismiss()
                otpVerifyDialog?.show()
            }

            override fun onCodeSent(
                verificationId: String,
                resendToken: PhoneAuthProvider.ForceResendingToken
            ) {
                mVerificationResendToken = resendToken
                mVerificationId = verificationId
                progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text =
                    AppVals.OTP_SENT
            }
        }
    }

    private fun onSendOTP(): View.OnClickListener {
        return View.OnClickListener {
            activity?.let {
                if (!isPhoneNoValid(etMobileNo.text)) return@OnClickListener
                if (etCurrentPassword.text.isNullOrEmpty()) {
                    Utils
                        .getInstance()
                        .showToast(
                            activity as Context,
                            AppVals.ERROR_EMPTY_CUR_PASS
                        )
                    return@OnClickListener
                }
                if (currentUser == null || mAuth.currentUser == null) return@OnClickListener
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
                        sendOTP("+91${etMobileNo.text.toString()}")
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
    }

    private fun isPhoneNoValid(phoneNo: Editable?): Boolean {
        if (phoneNo.isNullOrEmpty()) {
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    AppVals.ERROR_EMPTY_PHONE_NO
                )
            return false
        }
        if (phoneNo.toString().length != 10) {
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    AppVals.ERROR_INVALID_PHONE_NO
                )
            return false
        }
        return true
    }

    private fun isValidOTP(otp: Editable?): Boolean {
        if (otp.isNullOrEmpty()) {
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    AppVals.ERROR_EMPTY_OTP
                )
            return false
        }
        if (otp.toString().length != 6) {
            Utils
                .getInstance()
                .showToast(
                    activity as Context, AppVals.ERROR_INVALID_OTP
                )
            return false
        }
        return true
    }

    private fun resendOTP(phoneNo: String, resendToken: PhoneAuthProvider.ForceResendingToken) {
        progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = AppVals.OTP_RESEND
        if (progressDialog?.isShowing == false) progressDialog?.show()
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder()
                .setActivity(activity as Activity)
                .setPhoneNumber(phoneNo)
                .setTimeout(15, TimeUnit.SECONDS)
                .setCallbacks(mVerificationCallback)
                .setForceResendingToken(resendToken)
                .build()
        )
    }

    private fun sendOTP(phoneNo: String) {
        progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = AppVals.SEND_OTP
        if (progressDialog?.isShowing == false) progressDialog?.show()
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder()
                .setActivity(activity as Activity)
                .setPhoneNumber(phoneNo)
                .setTimeout(15, TimeUnit.SECONDS)
                .setCallbacks(mVerificationCallback)
                .build()
        )
    }

    private fun changePhoneNumber(credential: PhoneAuthCredential) {
        progressDialog?.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text =
            AppVals.OTP_VERIFYING
        if (progressDialog?.isShowing == false) progressDialog?.show()
        mAuth
            .currentUser
            ?.updatePhoneNumber(credential)
            ?.addOnSuccessListener { _ ->
                updateUsersDB()
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
                                    AppVals.ERROR_INVALID_OTP
                                )
                        }
                    }
                    is FirebaseAuthUserCollisionException -> {
                        otpVerifyDialog?.dismiss()
                        etMobileNo.text?.clear()
                        etCurrentPassword.text?.clear()
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_USER_COLLISION_MOBILE
                                )
                        }
                    }
                    is FirebaseAuthInvalidUserException -> {
                        otpVerifyDialog?.dismiss()
                        etMobileNo.text?.clear()
                        etCurrentPassword.text?.clear()
                        activity?.let {
                            Utils
                                .getInstance()
                                .showToast(
                                    it as Context,
                                    AppVals.ERROR_USER_DISABLED_MOBILE
                                )
                        }
                    }
                    else -> {
                        otpVerifyDialog?.dismiss()
                        etMobileNo.text?.clear()
                        etCurrentPassword.text?.clear()
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
    }

    private fun updateUsersDB() {
        Firebase
            .database
            .getReference("/users/${mAuth.currentUser?.uid}/phoneNo")
            .setValue(etMobileNo.text.toString())
            .addOnCompleteListener { task ->
                var count = 0
                if (task.isSuccessful) {
                    currentUser?.bloodDonationHistory?.forEach(action = { request ->
                        count++
                        Firebase
                            .database
                            .getReference("/bloodRequests/${request.key}/donors/${mAuth.currentUser?.uid}/phoneNo")
                            .setValue(etMobileNo.text.toString())
                            .addOnCompleteListener {
                                if (count == currentUser?.bloodDonationHistory?.size) {
                                    etMobileNo.text?.clear()
                                    etCurrentPassword.text?.clear()
                                    progressDialog?.dismiss()
                                    otpVerifyDialog?.dismiss()
                                    activity?.let {
                                        Utils
                                            .getInstance()
                                            .showToast(
                                                it as Context,
                                                AppVals.SUCCESS_PHONE_UPDATE
                                            )
                                        (it as DonorDashboard).closeFragment()
                                    }
                                }
                            }
                    })
                } else {
                    progressDialog?.dismiss()
                    otpVerifyDialog?.dismiss()
                    etMobileNo.text?.clear()
                    etCurrentPassword.text?.clear()
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

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsUpdateMobile {
            val fragment = DonorSettingsUpdateMobile()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }

}