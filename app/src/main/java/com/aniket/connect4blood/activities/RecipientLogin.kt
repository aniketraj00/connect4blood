package com.aniket.connect4blood.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils.OTPVerifyDialogCallback
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class RecipientLogin : AppCompatActivity() {

    private lateinit var etRecipientMobileNo: TextInputEditText
    private lateinit var btnRecipientSendOTP: Button
    private lateinit var progressDialog: AlertDialog
    private var otpDialogBox: BottomSheetDialog? = null
    private lateinit var appSharedPreferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mResendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var mVerificationId: String
    private var mVerificationInProgress = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("KEY_VERIFY_IN_PROGRESS", mVerificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mVerificationInProgress = savedInstanceState.getBoolean("KEY_VERIFY_IN_PROGRESS")
    }

    override fun onStart() {
        super.onStart()
        if (mVerificationInProgress &&
            !etRecipientMobileNo.text.isNullOrEmpty() &&
            etRecipientMobileNo.text.toString().length == 10
        ) {
            if(otpDialogBox?.isShowing == true) otpDialogBox?.dismiss()
            if(progressDialog.isShowing) progressDialog.dismiss()
            startVerification("+91" + etRecipientMobileNo.text.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipient_login)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etRecipientMobileNo = findViewById(R.id.etRecipientMobileNo)
        btnRecipientSendOTP = findViewById(R.id.btnRecipientSendOTP)
        appSharedPreferences = getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
        mCallback = initOnVerificationStateChangeCallback()
        progressDialog = Utils
            .getInstance()
            .initProgressDialog(
                this@RecipientLogin,
                "Please wait..."
            )
        otpDialogBox = Utils
            .getInstance()
            .initOTPVerifyDialog(this@RecipientLogin, object : OTPVerifyDialogCallback {
                override fun onOTPSubmit(otpInputField: TextInputEditText?) {
                    if(!Utils.getInstance().checkConnectivity(this@RecipientLogin)) return
                    if (otpInputField?.text.isNullOrEmpty()) {
                        Utils
                            .getInstance()
                            .showToast(
                                this@RecipientLogin,
                                "Error! Please enter the otp sent on your phone no."
                            )
                        return
                    }
                    if (otpInputField?.text?.length != 6) {
                        Utils
                            .getInstance()
                            .showToast(
                                this@RecipientLogin,
                                "Error! Please enter a valid 6-digit OTP."
                            )
                        return
                    }
                    mVerificationInProgress = false
                    signInWithCredentials(
                        PhoneAuthProvider.getCredential(
                            mVerificationId,
                            otpInputField.text.toString()
                        )
                    )
                }

                override fun onResendOTP() {
                    if(!Utils.getInstance().checkConnectivity(this@RecipientLogin)) return
                    startVerification("+91" + etRecipientMobileNo.text.toString(), mResendToken)
                }
            })

        btnRecipientSendOTP.setOnClickListener {
            if (!Utils.getInstance().checkConnectivity(this@RecipientLogin))
                return@setOnClickListener
            if (etRecipientMobileNo.text.isNullOrEmpty()) {
                Utils
                    .getInstance()
                    .showToast(this@RecipientLogin, "Error! Please enter your mobile no.")
                return@setOnClickListener
            }
            if(etRecipientMobileNo.text.toString().length != 10) {
                Utils
                    .getInstance()
                    .showToast(this@RecipientLogin, "Error! Please enter a valid mobile no.")
                return@setOnClickListener
            }
            startVerification("+91" + etRecipientMobileNo.text.toString())
        }
    }

    private fun startVerification(
        phoneNo: String,
        token: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        val options = PhoneAuthOptions
            .newBuilder()
            .setActivity(this@RecipientLogin)
            .setPhoneNumber(phoneNo)
            .setTimeout(15, TimeUnit.SECONDS)
            .setCallbacks(mCallback)
        if (token != null) {
            changeProgressDialogMsg("Resending OTP")
            PhoneAuthProvider.verifyPhoneNumber(
                options
                    .setForceResendingToken(token)
                    .build()
            )
        } else {
            changeProgressDialogMsg("Sending OTP")
            PhoneAuthProvider.verifyPhoneNumber(options.build())
        }
        mVerificationInProgress = true
        progressDialog.show()
    }

    private fun initOnVerificationStateChangeCallback(): PhoneAuthProvider.OnVerificationStateChangedCallbacks {
        return object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                mVerificationInProgress = false
                signInWithCredentials(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                otpDialogBox?.dismiss()
                mVerificationInProgress = false
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Utils.getInstance()
                            .showToast(this@RecipientLogin, "Invalid phone number entered!")
                    }
                    is FirebaseTooManyRequestsException -> {
                        Utils.getInstance().showToast(
                            this@RecipientLogin,
                            "Error! Too many requests were initiated."
                        )
                    }
                    else -> {
                        Utils.getInstance()
                            .showToast(this@RecipientLogin, "Something went wrong!")
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                changeProgressDialogMsg("Reading OTP")
                mResendToken = token
                mVerificationId = verificationId
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                progressDialog.dismiss()
                otpDialogBox?.show()
            }
        }
    }

    private fun signInWithCredentials(credential: PhoneAuthCredential) {
        changeProgressDialogMsg("Signing you in")
        if (!progressDialog.isShowing) progressDialog.show()
        mAuth
            .signInWithCredential(credential)
            .addOnSuccessListener {
                progressDialog.dismiss()
                otpDialogBox?.dismiss()
                appSharedPreferences
                    .edit()
                    .putString(AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY, "RECIPIENT")
                    .apply()
                val recipientDashboardIntent =
                    Intent(this@RecipientLogin, RecipientDashboard::class.java)
                val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                recipientDashboardIntent.flags = intentFlags
                startActivity(recipientDashboardIntent)
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                when (it) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Utils
                            .getInstance()
                            .showToast(
                                this@RecipientLogin,
                                "Error! Invalid OTP entered."
                            )
                    }
                    is FirebaseAuthInvalidUserException -> {
                        otpDialogBox?.dismiss()
                        Utils
                            .getInstance()
                            .showToast(
                                this@RecipientLogin,
                                "Error! Account deleted or is inactive."
                            )
                    }
                    else -> {
                        otpDialogBox?.dismiss()
                        Utils
                            .getInstance()
                            .showToast(
                                this@RecipientLogin,
                                "Error! Something went wrong, please try again later."
                            )
                    }
                }
            }

    }

    private fun changeProgressDialogMsg(msg: String) {
        progressDialog.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text = msg
    }

}