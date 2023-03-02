package com.aniket.connect4blood.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aniket.connect4blood.R
import com.aniket.connect4blood.utils.AppVals
import com.google.firebase.auth.FirebaseAuth

class Main : AppCompatActivity() {

    private lateinit var btnDonor: Button
    private lateinit var btnRecipient: Button
    private lateinit var appSharedPreferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        btnDonor = findViewById(R.id.btnDonor)
        btnRecipient = findViewById(R.id.btnRecipient)
        appSharedPreferences = getSharedPreferences(AppVals.APP_SHARED_PREFS_NAME, MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()

        //Set click listeners
        btnDonor.setOnClickListener {
            btnDonorListener()
        }
        btnRecipient.setOnClickListener {
            btnRecipientListener()
        }

        if (appSharedPreferences.getString(
                AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY,
                null
            ) == "DONOR"
        ) {
            if (mAuth.currentUser != null) {
                val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val donorDashboardIntent = Intent(
                    this@Main,
                    DonorDashboard::class.java
                )
                donorDashboardIntent.flags = intentFlags
                startActivity(donorDashboardIntent)
            } else {
                appSharedPreferences
                    .edit()
                    .remove(AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY)
                    .apply()
            }
        } else if (appSharedPreferences.getString(
                AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY,
                null
            ) == "RECIPIENT"
        ) {
            if (mAuth.currentUser != null) {
                val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val recipientDashboardIntent = Intent(
                    this@Main,
                    RecipientDashboard::class.java
                )
                recipientDashboardIntent.flags = intentFlags
                startActivity(recipientDashboardIntent)
            } else {
                appSharedPreferences
                    .edit()
                    .remove(AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY)
                    .apply()
            }
        } else {
            if (mAuth.currentUser != null) {
                mAuth.signOut()
            }
        }

    }

    private fun btnDonorListener() {
        startActivity(Intent(this@Main, DonorLogin::class.java))
    }

    private fun btnRecipientListener() {
        startActivity(Intent(this@Main, RecipientLogin::class.java))
    }
}