package com.aniket.connect4blood.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PasswordResetInit : AppCompatActivity() {

    private lateinit var etDonorPassResetMobile: TextInputEditText
    private lateinit var btnDonorPassResetSendOTP: Button
    private lateinit var progressDialog: AlertDialog
    private lateinit var otpDialogBox: BottomSheetDialog
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mVerificationCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mVerificationId: String
    private lateinit var mResendToken: PhoneAuthProvider.ForceResendingToken
    private var mVerificationInProgress = false

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("KEY_VERIFY_IN_PROGRESS", mVerificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mVerificationInProgress = savedInstanceState.getBoolean("KEY_VERIFY_IN_PROGRESS")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset_init)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etDonorPassResetMobile = findViewById(R.id.etDonorPassResetMobile)
        btnDonorPassResetSendOTP = findViewById(R.id.btnDonorPassResetSendOTP)
        mAuth = FirebaseAuth.getInstance()
        progressDialog = Utils
            .getInstance()
            .initProgressDialog(
                this@PasswordResetInit,
                AppVals.SEND_OTP
            )
        otpDialogBox = Utils
            .getInstance()
            .initOTPVerifyDialog(
                this@PasswordResetInit,
                object: Utils.OTPVerifyDialogCallback {
                    override fun onOTPSubmit(otpInputField: TextInputEditText?) {
                        if(otpInputField?.text.isNullOrEmpty()) {
                            Utils
                                .getInstance()
                                .showToast(
                                    this@PasswordResetInit,
                                    AppVals.ERROR_EMPTY_OTP
                                )
                            return
                        }
                        if(otpInputField?.text.toString().length != 6) {
                            Utils
                                .getInstance()
                                .showToast(
                                    this@PasswordResetInit,
                                    AppVals.ERROR_INVALID_OTP
                                )
                            return
                        }
                        val credential = PhoneAuthProvider.getCredential(
                            mVerificationId,
                            otpInputField?.text.toString()
                        )
                        mVerificationInProgress = false
                        verifyOTP(credential)
                    }

                    override fun onResendOTP() {
                        progressDialog.show()
                        progressDialog.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = AppVals.OTP_RESEND
                        resendOTP("+91${etDonorPassResetMobile.text.toString()}")
                    }

                }
            )
        mVerificationCallback = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                mVerificationInProgress = false
                verifyOTP(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                otpDialogBox.dismiss()
                mVerificationInProgress = false
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Utils.getInstance()
                            .showToast(this@PasswordResetInit, AppVals.ERROR_INVALID_PHONE_NO)
                    }
                    is FirebaseTooManyRequestsException -> {
                        Utils.getInstance().showToast(
                            this@PasswordResetInit,
                            AppVals.ERROR_TOO_MANY_BACKEND_REQUESTS
                        )
                    }
                    else -> {
                        Utils.getInstance()
                            .showToast(this@PasswordResetInit, AppVals.ERROR_GENERIC)
                    }
                }
            }

            override fun onCodeSent(verificationId: String, resendToken: PhoneAuthProvider.ForceResendingToken) {
                progressDialog.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = AppVals.OTP_SENT
                mVerificationId = verificationId
                mResendToken = resendToken
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                mVerificationId = verificationId
                progressDialog.dismiss()
                otpDialogBox.show()
            }
        }

        btnDonorPassResetSendOTP.setOnClickListener(onOTPSend())
        
    }

    private fun onOTPSend(): View.OnClickListener{
        return View.OnClickListener {
            if(etDonorPassResetMobile.text.isNullOrEmpty()) {
                Utils
                    .getInstance()
                    .showToast(
                        this@PasswordResetInit,
                        AppVals.ERROR_EMPTY_PHONE_NO
                    )
                return@OnClickListener
            }
            if(etDonorPassResetMobile.text.toString().length != 10) {
                Utils
                    .getInstance()
                    .showToast(
                        this@PasswordResetInit,
                        AppVals.ERROR_INVALID_PHONE_NO
                    )
                return@OnClickListener
            }
            progressDialog.show()
            sendOTP("+91${etDonorPassResetMobile.text.toString()}")
        }
    }

    private fun sendOTP(phoneNo: String) {
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder()
                .setActivity(this@PasswordResetInit)
                .setPhoneNumber(phoneNo)
                .setTimeout(15, TimeUnit.SECONDS)
                .setCallbacks(mVerificationCallback)
                .build()
        )
        mVerificationInProgress = true
    }

    private fun resendOTP(phoneNo: String) {
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions
                .newBuilder()
                .setActivity(this@PasswordResetInit)
                .setPhoneNumber(phoneNo)
                .setTimeout(15, TimeUnit.SECONDS)
                .setCallbacks(mVerificationCallback)
                .setForceResendingToken(mResendToken)
                .build()
        )
        mVerificationInProgress = true
    }

    private fun verifyOTP(credential: PhoneAuthCredential) {
        if(!progressDialog.isShowing) progressDialog.show()
        progressDialog.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = AppVals.OTP_VERIFYING
        mAuth
            .signInWithCredential(credential)
            .addOnSuccessListener {
                if(it.additionalUserInfo?.isNewUser == true) {
                    it.user?.delete()?.addOnCompleteListener {
                        progressDialog.dismiss()
                        otpDialogBox.dismiss()
                        Utils
                            .getInstance()
                            .showToast(
                                this@PasswordResetInit,
                                AppVals.ERROR_USER_NOT_REGISTERED
                            )
                        finish()
                    }
                } else {
                    progressDialog.dismiss()
                    otpDialogBox.dismiss()
                    startActivity(
                        Intent(
                            this@PasswordResetInit,
                            PasswordReset::class.java
                        )
                    )
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                when(e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Utils
                            .getInstance()
                            .showToast(
                                this@PasswordResetInit,
                                AppVals.ERROR_INVALID_OTP
                            )
                    }
                    is FirebaseAuthInvalidUserException -> {
                        otpDialogBox.dismiss()
                        etDonorPassResetMobile.text?.clear()
                        Utils
                            .getInstance()
                            .showToast(
                                this@PasswordResetInit,
                                AppVals.ERROR_USER_DISABLED_MOBILE
                            )
                    }
                    else -> {
                        otpDialogBox.dismiss()
                        etDonorPassResetMobile.text?.clear()
                        Utils
                            .getInstance()
                            .showToast(
                                this@PasswordResetInit,
                                AppVals.ERROR_GENERIC
                            )
                    }
                }
            }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}