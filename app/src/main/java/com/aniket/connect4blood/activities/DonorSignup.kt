package com.aniket.connect4blood.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.BloodGroupType
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.*
import java.util.concurrent.TimeUnit

class DonorSignup : AppCompatActivity() {
//TODO: Refactor DonorSignup Activity

    private lateinit var etDonorRegFullName: TextInputEditText
    private lateinit var etDonorRegEmail: TextInputEditText
    private lateinit var etDonorRegPassword: TextInputEditText
    private lateinit var etDonorRegPasswordConfirm: TextInputEditText
    private lateinit var etDonorRegMobile: TextInputEditText
    private lateinit var etDonorRegDOBPicker: TextInputEditText
    private lateinit var acDonorRegGender: AutoCompleteTextView
    private lateinit var acDonorRegBloodGroup: AutoCompleteTextView
    private lateinit var cbTerms: CheckBox
    private lateinit var btnDonorRegSendOTP: Button
    private lateinit var txtViewTermsLink: TextView
    private var progressDialog: AlertDialog? = null
    private var otpDialogBox: BottomSheetDialog? = null
    private var birthDate: Date? = null
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mResendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mVerificationId: String
    private var mVerificationInProgress: Boolean = false
    private val dateFormat = "dd/MM/yyyy"


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("KEY_VERIFY_IN_PROGRESS", mVerificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mVerificationInProgress = savedInstanceState.getBoolean("KEY_VERIFY_IN_PROGRESS")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_signup)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Init references
        etDonorRegFullName = findViewById(R.id.etDonorRegFullName)
        etDonorRegEmail = findViewById(R.id.etDonorRegEmail)
        etDonorRegPassword = findViewById(R.id.etDonorRegPassword)
        etDonorRegPasswordConfirm = findViewById(R.id.etDonorRegPasswordConfirm)
        etDonorRegMobile = findViewById(R.id.etDonorRegMobile)
        etDonorRegDOBPicker = findViewById(R.id.etDonorRegDOBPicker)
        acDonorRegGender = findViewById(R.id.acDonorRegGender)
        acDonorRegBloodGroup = findViewById(R.id.acDonorRegBloodGroup)
        cbTerms = findViewById(R.id.cbTerms)
        btnDonorRegSendOTP = findViewById(R.id.btnDonorRegSendOTP)
        txtViewTermsLink = findViewById(R.id.txtViewTermsLink)

        //Init widgets(if any)
        acDonorRegGender.setAdapter(
            ArrayAdapter(
                this@DonorSignup,
                R.layout.dropdown_menu_popup_item,
                arrayListOf(
                    "Male", "Female"
                )
            )
        )

        acDonorRegBloodGroup.setAdapter(
            ArrayAdapter(
                this@DonorSignup,
                R.layout.dropdown_menu_popup_item,
                arrayListOf(
                    BloodGroupType.A_POS,
                    BloodGroupType.A_NEG,
                    BloodGroupType.B_POS,
                    BloodGroupType.B_NEG,
                    BloodGroupType.O_POS,
                    BloodGroupType.O_NEG,
                    BloodGroupType.AB_POS,
                    BloodGroupType.AB_NEG
                )
            )
        )

        mAuth = Firebase.auth
        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                progressDialog?.dismiss()
                mVerificationInProgress = false
                signInUserWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog?.dismiss()
                otpDialogBox?.dismiss()
                mVerificationInProgress = false
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Utils.getInstance()
                            .showToast(this@DonorSignup, "Invalid phone number entered!")
                    }
                    is FirebaseTooManyRequestsException -> {
                        Utils.getInstance().showToast(
                            this@DonorSignup,
                            "Error! Too many requests were initiated."
                        )
                    }
                    else -> {
                        Utils.getInstance()
                            .showToast(this@DonorSignup, "Something went wrong!")
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                resendToken: PhoneAuthProvider.ForceResendingToken
            ) {
                mVerificationId = verificationId
                mResendToken = resendToken
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                progressDialog?.dismiss()
                openOTPVerifyDialog(verificationId)
            }
        }


        //Set click listeners
        etDonorRegDOBPicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (birthDate != null) {
                calendar.time = birthDate
            }
            DatePickerDialog(
                this@DonorSignup,
                { _: DatePicker?, year: Int, month: Int, day: Int ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    setOnDateSelection(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        txtViewTermsLink.setOnClickListener {
            //TODO:Redirect to default browser with terms and conditions webpage url
        }

        btnDonorRegSendOTP.setOnClickListener {
            //Connectivity Check and Input Validation
            if(!Utils.getInstance().checkConnectivity(this@DonorSignup))
                return@setOnClickListener
            if (!validateAllInputs())
                return@setOnClickListener
            //start authentication
            startVerification("+91" + etDonorRegMobile.text.toString())
        }

    }

    override fun onStart() {
        super.onStart()
        if (mVerificationInProgress &&
            !etDonorRegMobile.text.isNullOrEmpty() &&
            etDonorRegMobile.text.toString().length == 10
        ) {
            if(otpDialogBox?.isShowing == true) otpDialogBox?.dismiss()
            if(progressDialog?.isShowing == true) progressDialog?.dismiss()
            startVerification("+91" + etDonorRegMobile.text.toString())
        }
    }

    private fun setOnDateSelection(date: Date) {
        val dateFormat = SimpleDateFormat(dateFormat, Locale.US)
        birthDate = date
        etDonorRegDOBPicker.setText(dateFormat.format(date))
    }

    private fun openOTPVerifyDialog(verificationId: String) {
        if (otpDialogBox != null && otpDialogBox?.isShowing == true) return
        otpDialogBox = BottomSheetDialog(this@DonorSignup)
        otpDialogBox?.setContentView(R.layout.layout_otp_verify)
        val etOtp = otpDialogBox?.findViewById<TextInputEditText>(R.id.etOtp)
        val otpSubmitBtn = otpDialogBox?.findViewById<Button>(R.id.btnOtpSend)
        val resendOtpText = otpDialogBox?.findViewById<TextView>(R.id.btnOtpResend)

        otpSubmitBtn?.setOnClickListener {
            if(!Utils.getInstance().checkConnectivity(this@DonorSignup))
                return@setOnClickListener
            if (etOtp?.text.isNullOrEmpty()) {
                Utils.getInstance().showToast(
                    this@DonorSignup,
                    "Error! Please enter the OTP sent on your phone no."
                )
                return@setOnClickListener
            }
            if(etOtp?.text?.length != 6) {
                Utils.getInstance().showToast(
                    this@DonorSignup,
                    "Error! Please enter valid 6-digit OTP."
                )
                return@setOnClickListener
            }
            mVerificationInProgress = false
            signInUserWithCredential(
                PhoneAuthProvider.getCredential(
                    verificationId,
                    etOtp.text.toString()
                )
            )
        }
        resendOtpText?.setOnClickListener {
            if(!Utils.getInstance().checkConnectivity(this@DonorSignup))
                return@setOnClickListener
            restartVerification(
                "+91" + etDonorRegMobile.text.toString(),
                mResendToken
            )
            Utils.getInstance()
                .showToast(this@DonorSignup, "OTP sent again! Please check your phone.")
        }
        otpDialogBox?.show()
        etOtp?.requestFocus()
    }

    private fun initAndShowProgressDialog(msg: String) {
        if (progressDialog != null && progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
            progressDialog = null
        }
        val view = View.inflate(this@DonorSignup, R.layout.dialog_progress, null)
        val msgTextView: TextView = view.findViewById(R.id.txtProgressDialogMsg)
        msgTextView.text = msg
        progressDialog = AlertDialog
            .Builder(this@DonorSignup, R.style.WrapContentDialog)
            .setView(view)
            .create()
        progressDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog?.show()
    }

    private fun checkAgeAbove18(): Boolean {
        if (Utils
                .getInstance()
                .calcAge(SimpleDateFormat(dateFormat, Locale.US).format(birthDate)) >= 18
        ) return true
        return false
    }

    private fun phoneAuthOptionsBuilder(phoneNo: String): PhoneAuthOptions.Builder {
        return PhoneAuthOptions
            .newBuilder(mAuth)
            .setActivity(this@DonorSignup)
            .setPhoneNumber(phoneNo)
            .setTimeout(15, TimeUnit.SECONDS)
            .setCallbacks(mCallback)
    }

    private fun startVerification(phoneNo: String) {
        initAndShowProgressDialog("Waiting for OTP")
        PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptionsBuilder(phoneNo).build())
        mVerificationInProgress = true
    }

    private fun restartVerification(phoneNo: String, token: PhoneAuthProvider.ForceResendingToken) {
        PhoneAuthProvider.verifyPhoneNumber(
            phoneAuthOptionsBuilder(phoneNo)
                .setForceResendingToken(token)
                .build()
        )
        mVerificationInProgress = true
    }

    private fun validateAllInputs(): Boolean {
        if (!cbTerms.isChecked) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "Please accept the terms before proceeding!"
            )
            return false
        }
        if (etDonorRegFullName.text.isNullOrEmpty() ||
            etDonorRegEmail.text.isNullOrEmpty() ||
            etDonorRegPassword.text.isNullOrEmpty() ||
            etDonorRegPasswordConfirm.text.isNullOrEmpty() ||
            etDonorRegMobile.text.isNullOrEmpty() ||
            etDonorRegDOBPicker.text.isNullOrEmpty() ||
            acDonorRegGender.text.isNullOrEmpty() ||
            acDonorRegBloodGroup.text.isNullOrEmpty()
        ) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "All fields are mandatory! Please make sure they are not empty."
            )
            return false
        }

        if (etDonorRegPassword.text.toString().length < 8) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "Password should be minimum 8 characters long!"
            )
            return false
        }

        if (etDonorRegPasswordConfirm.text.toString() != etDonorRegPassword.text.toString()) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "Password and Confirm Password fields don't match! Please verify."
            )
            return false
        }

        if (etDonorRegMobile.text.toString().length < 10) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "Please enter a valid phone number!"
            )
            return false
        }

        if (!checkAgeAbove18()) {
            Utils.getInstance().showToast(
                this@DonorSignup,
                "The minimum age of a blood donor should be 18 years!"
            )
            return false
        }
        return true
    }

    private fun signInUserWithCredential(credential: PhoneAuthCredential) {
        initAndShowProgressDialog("Signing Up...")
        mAuth
            .signInWithCredential(credential)
            .addOnFailureListener {
                progressDialog?.dismiss()
                when (it) {
                    is FirebaseAuthInvalidCredentialsException -> Utils.getInstance().showToast(
                        this@DonorSignup,
                        "Wrong OTP entered! Please try again."
                    )
                    else -> Utils.getInstance().showToast(
                        this@DonorSignup,
                        "Something went wrong! Please try again later."
                    )
                }
            }
            .addOnSuccessListener {
                val emailAuth = EmailAuthProvider.getCredential(
                    etDonorRegEmail.text.toString(),
                    etDonorRegPassword.text.toString()
                )
                mAuth
                    .currentUser
                    ?.linkWithCredential(emailAuth)
                    ?.addOnSuccessListener { result ->
                        val user = User(
                            name = etDonorRegFullName.text.toString(),
                            email = etDonorRegEmail.text.toString(),
                            phoneNo = etDonorRegMobile.text.toString(),
                            dateOfBirth = etDonorRegDOBPicker.text.toString(),
                            gender = acDonorRegGender.text.toString(),
                            bloodGroup = acDonorRegBloodGroup.text.toString(),
                            isEligible = false
                        )
                        result.user?.uid?.let { it1 ->
                            FirebaseDatabase
                                .getInstance()
                                .getReference("users")
                                .child(it1)
                                .setValue(user)
                                .addOnFailureListener {
                                    //Rollback changes
                                    mAuth.currentUser?.delete()?.addOnCompleteListener {
                                        mAuth.signOut()
                                        Utils.getInstance().showToast(
                                            this@DonorSignup,
                                            "Something went wrong! Please try again later."
                                        )
                                        val loginIntent = Intent(
                                            this@DonorSignup,
                                            DonorLogin::class.java
                                        )
                                        val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        loginIntent.flags = intentFlags
                                        startActivity(loginIntent)
                                    }
                                }
                                .addOnSuccessListener {
                                    otpDialogBox?.dismiss()
                                    progressDialog?.dismiss()
                                    Utils.getInstance().showToast(
                                        this@DonorSignup,
                                        "Registration successful! Sending verification email..."
                                    )
                                    mAuth
                                        .currentUser
                                        ?.sendEmailVerification()
                                        ?.addOnCompleteListener { task ->
                                            if(task.isSuccessful) {
                                                Utils.getInstance().showToast(
                                                    this@DonorSignup,
                                                    "Verification email sent! Please check your mailbox"
                                                )
                                            } else {
                                                Utils.getInstance().showToast(
                                                    this@DonorSignup,
                                                    "Error! Something went wrong, please try again."
                                                )
                                            }
                                            mAuth.signOut()
                                            val donorLoginIntent = Intent(
                                                this@DonorSignup,
                                                DonorLogin::class.java
                                            )
                                            val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            donorLoginIntent.flags = intentFlags
                                            startActivity(donorLoginIntent)
                                        }

                                }
                        }
                    }
                    ?.addOnFailureListener {
                        progressDialog?.dismiss()
                        otpDialogBox?.dismiss()
                        mAuth.signOut()
                        when (it) {
                            is FirebaseAuthWeakPasswordException -> Utils.getInstance().showToast(
                                this@DonorSignup,
                                "Password is weak! Please choose another one."
                            )
                            is FirebaseAuthUserCollisionException -> {
                                Utils.getInstance().showToast(
                                    this@DonorSignup,
                                    "This email address is already linked to another account!"
                                )
                            }
                            is FirebaseAuthInvalidUserException -> {
                                Utils.getInstance().showToast(
                                    this@DonorSignup,
                                    "Error! This user's account has been disabled or deleted!"
                                )
                            }
                            is FirebaseAuthException -> {
                                Utils.getInstance().showToast(
                                    this@DonorSignup,
                                    "Some unexpected error occurred!"
                                )
                            }
                        }
                    }
            }
    }


}