package com.aniket.connect4blood.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aniket.connect4blood.BuildConfig
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.Main
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.util.*

class Utils private constructor() {
    companion object {
        fun getInstance() = Utils()
    }

    fun showToast(context: Context, msg: String) {
        Toast
            .makeText(context, msg, Toast.LENGTH_LONG)
            .show()
    }

    fun calcAge(birthDate: String): Int {
        val sdf = android.icu.text.SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val a = Calendar.getInstance()
        val b = Calendar.getInstance()
        b.time = Date.from(sdf.parse(birthDate).toInstant())
        var diff = a.get(Calendar.YEAR) - b.get(Calendar.YEAR)
        if (b.get(Calendar.MONTH) > a.get(Calendar.MONTH) ||
            (b.get(Calendar.MONTH) == a.get(Calendar.MONTH) &&
                    b.get(Calendar.DAY_OF_MONTH) > a.get(Calendar.DAY_OF_MONTH))
        ) {
            diff--
        }
        return diff
    }

    fun initProgressDialog(context: Context, msg: String): AlertDialog {
        val view = View.inflate(context, R.layout.dialog_progress, null)
        val msgTextView: TextView = view.findViewById(R.id.txtProgressDialogMsg)
        msgTextView.text = msg
        val dialog = AlertDialog
            .Builder(context, R.style.WrapContentDialog)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    fun initOTPVerifyDialog(
        context: Context,
        callback: OTPVerifyDialogCallback
    ): BottomSheetDialog {
        val otpDialogBox = BottomSheetDialog(context)
        otpDialogBox.setContentView(R.layout.layout_otp_verify)

        val etOtp = otpDialogBox.findViewById<TextInputEditText>(R.id.etOtp)
        val otpSubmitBtn = otpDialogBox.findViewById<Button>(R.id.btnOtpSend)
        val resendOtpText = otpDialogBox.findViewById<TextView>(R.id.btnOtpResend)

        otpSubmitBtn?.setOnClickListener { callback.onOTPSubmit(etOtp) }
        resendOtpText?.setOnClickListener { callback.onResendOTP() }

        otpDialogBox.setOnShowListener {
            etOtp?.requestFocus()
        }
        otpDialogBox.setOnDismissListener {
            etOtp?.text?.clear()
        }

        return otpDialogBox
    }

    @SuppressLint("MissingPermission")
    fun checkConnectivity(context: FragmentActivity?): Boolean {
        val isConnected: Boolean?
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        isConnected = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> true
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> true
            else -> false
        }
        if (!isConnected) {
            AlertDialog
                .Builder(context as Context)
                .setTitle("Error!")
                .setMessage("No internet connection. Please check your settings")
                .setPositiveButton(
                    "NETWORK SETTINGS"
                ) { _, _ -> context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                .setNegativeButton(
                    "QUIT APP"
                ) { _, _ -> ActivityCompat.finishAffinity(context as Activity) }
                .create()
                .show()
        }
        return isConnected
    }

    fun hasLocationPermissions(context: FragmentActivity?): Boolean {
        if (context != null) {
            if (
                (ContextCompat.checkSelfPermission(
                    context as Context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(
                    context as Context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
            ) return true
        }
        return false
    }

    fun logoutAndClearSharedPrefs(
        context: FragmentActivity,
        auth: FirebaseAuth,
        appSharedPreferences: SharedPreferences
    ) {
        auth.signOut()
        appSharedPreferences
            .edit()
            .remove(AppVals.APP_SHARED_PREFS_INIT_AUTH_KEY)
            .apply()
        val homeIntent = Intent(context, Main::class.java)
        val intentFlags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        homeIntent.flags = intentFlags
        context.startActivity(homeIntent)
    }

    fun generateComplaintId(userPhoneNo: String, dateTime: LocalDateTime): String {
        val pl4d = userPhoneNo.subSequence(5, userPhoneNo.length)
        val day = dateTime.dayOfMonth
        val month = dateTime.monthValue
        val year = dateTime.year
        val hour = dateTime.hour
        val min = dateTime.minute
        val sec = dateTime.second
        return "C4B$pl4d$day$month$year$hour$min$sec"
    }

    fun shareApp(context: Context) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Check out this app called Connect4Blood which is helping blood donors and recipients connect with each other: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
        )
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    fun enableBackButton(actionBar:ActionBar?, title: String = "Settings") {
        actionBar?.title = title
        actionBar?.setHomeButtonEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun disableBackButton(actionBar:ActionBar?, title: String = "Connect4Blood") {
        actionBar?.title = title
        actionBar?.setHomeButtonEnabled(false)
        actionBar?.setDisplayHomeAsUpEnabled(false)
    }

    interface OTPVerifyDialogCallback {
        fun onOTPSubmit(otpInputField: TextInputEditText?)
        fun onResendOTP()
    }
}


