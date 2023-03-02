package com.aniket.connect4blood.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

class PasswordReset : AppCompatActivity() {

    private lateinit var etDonorPassResetNew: TextInputEditText
    private lateinit var etDonorPassResetConfirm: TextInputEditText
    private lateinit var btnDonorPassReset: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset)

        etDonorPassResetNew = findViewById(R.id.etDonorPassResetNew)
        etDonorPassResetConfirm = findViewById(R.id.etDonorPassResetConfirm)
        btnDonorPassReset = findViewById(R.id.btnDonorPassReset)
        mAuth = FirebaseAuth.getInstance()
        progressDialog = Utils
            .getInstance()
            .initProgressDialog(
                this@PasswordReset,
                AppVals.PROGRESS_GENERIC
            )
        btnDonorPassReset.setOnClickListener(onResetPassword())
    }

    private fun onResetPassword(): View.OnClickListener {
        return View.OnClickListener {
            if(!validInputs()) return@OnClickListener
            progressDialog.show()
            mAuth
                .currentUser
                ?.updatePassword(etDonorPassResetNew.text.toString())
                ?.addOnSuccessListener {
                    progressDialog.dismiss()
                    mAuth.signOut()
                    Utils
                        .getInstance()
                        .showToast(
                            this@PasswordReset,
                            AppVals.SUCCESS_PASS_UPDATE
                        )
                    finish()
                }
                ?.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    mAuth.signOut()
                    when(e) {
                        is FirebaseAuthRecentLoginRequiredException -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    this@PasswordReset,
                                    AppVals.ERROR_RECENT_LOGIN_REQUIRED
                                )
                        }
                        else -> {
                            Utils
                                .getInstance()
                                .showToast(
                                    this@PasswordReset,
                                    AppVals.ERROR_GENERIC
                                )
                        }
                    }
                    startActivity(Intent(
                        this@PasswordReset,
                        PasswordResetInit::class.java
                    ))
                    finish()
                }
        }
    }

    private fun validInputs(): Boolean {
        if(etDonorPassResetNew.text.isNullOrEmpty() || etDonorPassResetConfirm.text.isNullOrEmpty()) {
            Utils
                .getInstance()
                .showToast(
                    this@PasswordReset,
                    AppVals.ERROR_FIELDS_EMPTY
                )
            return false
        }
        if(etDonorPassResetNew.text.toString() != etDonorPassResetConfirm.text.toString()) {
            Utils
                .getInstance()
                .showToast(
                    this@PasswordReset,
                    AppVals.ERROR_PASS_FIELDS_NOT_EQUAL
                )
            return false
        }
        return true
    }
}