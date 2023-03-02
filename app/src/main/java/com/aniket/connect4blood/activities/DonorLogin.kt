package com.aniket.connect4blood.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class DonorLogin : AppCompatActivity() {

    private lateinit var rlDonorLogin: CoordinatorLayout
    private lateinit var txtDonorRegisterLink: TextView
    private lateinit var txtDonorPassResetLink: TextView
    private lateinit var etDonorLoginEmail: TextInputEditText
    private lateinit var etDonorLoginPassword: TextInputEditText
    private lateinit var btnDonorLogin: Button
    private lateinit var progressDialog: AlertDialog
    private lateinit var appSharedPreferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_login)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rlDonorLogin = findViewById(R.id.rlDonorLogin)
        txtDonorRegisterLink = findViewById(R.id.txtDonorRegisterLink)
        txtDonorPassResetLink = findViewById(R.id.txtDonorPassResetLink)
        etDonorLoginEmail = findViewById(R.id.etDonorLoginEmail)
        etDonorLoginPassword = findViewById(R.id.etDonorLoginPassword)
        btnDonorLogin = findViewById(R.id.btnDonorLogin)
        progressDialog = Utils.getInstance().initProgressDialog(this@DonorLogin, "Please wait...")
        appSharedPreferences = getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, MODE_PRIVATE)

        mAuth = FirebaseAuth.getInstance()


        txtDonorRegisterLink.setOnClickListener {
            startActivity(Intent(this@DonorLogin, DonorSignup::class.java))
        }

        txtDonorPassResetLink.setOnClickListener {
            startActivity(Intent(this@DonorLogin, PasswordResetInit::class.java))
        }

        btnDonorLogin.setOnClickListener {
            if (!Utils.getInstance().checkConnectivity(this@DonorLogin)) return@setOnClickListener
            if (etDonorLoginEmail.text.isNullOrEmpty() || etDonorLoginPassword.text.isNullOrEmpty()) {
                Utils.getInstance().showToast(
                    this@DonorLogin,
                    "Error! Please enter your email and password!",
                )
                return@setOnClickListener
            }
            progressDialog.findViewById<TextView>(R.id.txtProgressDialogMsg)?.text =
                getString(R.string.wait_txt)
            progressDialog.show()
            mAuth.signInWithEmailAndPassword(
                etDonorLoginEmail.text.toString(),
                etDonorLoginPassword.text.toString()
            )
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    if (mAuth.currentUser?.isEmailVerified == false) {
                        Snackbar
                            .make(
                                rlDonorLogin,
                                "Email Not Verified! Please check your mailbox for verification email.",
                                Snackbar.LENGTH_INDEFINITE
                            )
                            .setAction("RESEND", View.OnClickListener {
                                progressDialog
                                    .findViewById<TextView>(R.id.txtProgressDialogMsg)
                                    ?.text = getString(R.string.resending_verification_email)
                                progressDialog.show()
                                if (mAuth.currentUser != null) {
                                    mAuth
                                        .currentUser
                                        ?.sendEmailVerification()
                                        ?.addOnCompleteListener(onVerificationEmailSent())
                                    return@OnClickListener
                                }
                                mAuth.signInWithEmailAndPassword(
                                    etDonorLoginEmail.text.toString(),
                                    etDonorLoginPassword.text.toString()
                                )
                                    .addOnSuccessListener {
                                        it
                                            .user
                                            ?.sendEmailVerification()
                                            ?.addOnCompleteListener(onVerificationEmailSent())
                                    }
                                    .addOnFailureListener {
                                        progressDialog.dismiss()
                                        mAuth.signOut()
                                        Utils
                                            .getInstance()
                                            .showToast(
                                                this@DonorLogin,
                                                "Error! Something went wrong, please try again."
                                            )
                                    }
                            })
                            .show()

                        mAuth.signOut()
                        return@addOnSuccessListener
                    }
                    appSharedPreferences
                        .edit()
                        .putString(AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY, "DONOR")
                        .apply()
                    val dashboardIntent = Intent(
                        this@DonorLogin,
                        DonorDashboard::class.java
                    )
                    val intentFlags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    dashboardIntent.flags = intentFlags
                    startActivity(dashboardIntent)
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    when (it) {
                        is FirebaseAuthInvalidCredentialsException -> Utils.getInstance().showToast(
                            this@DonorLogin,
                            "Error! Incorrect email or password."
                        )
                        is FirebaseAuthInvalidUserException -> Utils.getInstance().showToast(
                            this@DonorLogin,
                            "Error! User has been deleted or disabled."
                        )
                        is FirebaseNetworkException -> Utils.getInstance().showToast(
                            this@DonorLogin,
                            "Error! Something wrong with the network."
                        )
                        else -> Utils.getInstance().showToast(
                            this@DonorLogin,
                            "Error! Something went wrong."
                        )
                    }
                    Log.e("Firebase Error: ", it.toString())
                }
        }
    }

    private fun onVerificationEmailSent(): OnCompleteListener<Void> {
        return OnCompleteListener {
            progressDialog.dismiss()
            mAuth.signOut()
            Utils
                .getInstance()
                .showToast(
                    this@DonorLogin,
                    "Verification email sent! Please check your mailbox."
                )
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