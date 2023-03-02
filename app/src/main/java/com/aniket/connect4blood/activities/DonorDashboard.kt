package com.aniket.connect4blood.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.fragments.DonorDashboardHome
import com.aniket.connect4blood.fragments.DonorDashboardProfile
import com.aniket.connect4blood.fragments.DonorDashboardRequests
import com.aniket.connect4blood.fragments.DonorDashboardSettings
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class DonorDashboard : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressDialog: AlertDialog
    private lateinit var mCurrentUserListener: ValueEventListener
    private lateinit var mCurrentUserDBRef: DatabaseReference
    private var mCurrentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_dashboard)
        title = "Connect4Blood"

        //Initialize components
        bottomNavigationView = findViewById(R.id.dashboardBottomNav)
        progressDialog = Utils
            .getInstance()
            .initProgressDialog(
                this@DonorDashboard,
                "Please wait..."
            )
        mCurrentUserDBRef = Firebase
            .database
            .getReference("/users/${Firebase.auth.currentUser?.uid}")
        mCurrentUserListener = initCurrentUserListener()

        //Attach currentUser Listener
        progressDialog.show()
        mCurrentUserDBRef
            .get()
            .addOnSuccessListener { snapshot ->
                progressDialog.dismiss()
                //Init the current user profile
                mCurrentUser = snapshot.getValue<User>()
                //Set View Listeners
                bottomNavigationView.setOnItemSelectedListener {
                    supportFragmentManager.popBackStack()
                    when (it.itemId) {
                        R.id.bottomNavHome -> {
                            openFragment(DonorDashboardHome.newInstance(mCurrentUser))
                            true
                        }
                        R.id.bottomNavActiveRequest -> {
                            openFragment(DonorDashboardRequests.newInstance(mCurrentUser))
                            true
                        }
                        R.id.bottomNavProfile -> {
                            openFragment(DonorDashboardProfile.newInstance())
                            true
                        }
                        R.id.bottomNavSettings -> {
                            openFragment(DonorDashboardSettings.newInstance(mCurrentUser))
                            true
                        }
                        else -> false
                    }
                }

                //Open default fragment
                openFragment(DonorDashboardHome.newInstance(mCurrentUser))

            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Utils
                    .getInstance()
                    .showToast(
                        this@DonorDashboard,
                        "Server Error! Please try again later."
                    )
            }
        //Attach user value change listener
        mCurrentUserDBRef.ref.addValueEventListener(mCurrentUserListener)
    }

    private fun initCurrentUserListener(): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mCurrentUser = snapshot.getValue<User>()
            }

            override fun onCancelled(error: DatabaseError) {
                //TODO("Not yet implemented")
            }

        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.dashboardContent, fragment)
            .commit()
    }

    fun checkUserPendingRequest(currentUser: User?, onCheckCallback: UserPendingRequestProvider) {
        if (currentUser != null && !currentUser.bloodDonationHistory.isNullOrEmpty()) {
            var mHasPendingRequest = false
            var count = 0
            currentUser.bloodDonationHistory.forEach {
                Firebase
                    .database
                    .getReference("/bloodRequests/${it.key}/status")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        count++
                        val status = snapshot.getValue<BloodRequestStatus>()
                        if ((status == BloodRequestStatus.PENDING || status == BloodRequestStatus.CONFIRMED) && !mHasPendingRequest) {
                            mHasPendingRequest = true
                            onCheckCallback.onCheck(true, it.key)
                        }
                        if ((count == currentUser.bloodDonationHistory.size) && !mHasPendingRequest) {
                            onCheckCallback.onCheck(false, null)
                        }
                    }
            }
        } else {
            onCheckCallback.onCheck(false, null)
        }
    }

    fun cancelUserPendingRequest(
        userId: String,
        requestId: String,
        onRequestCancelCallback: UserPendingRequestCancellationProvider
    ) {
        val childUpdates = hashMapOf<String, Any?>(
            "/bloodRequests/$requestId/donors/$userId" to null,
            "/users/$userId/bloodDonationHistory/$requestId" to null
        )
        Firebase
            .database
            .getReference("/bloodRequests/$requestId/status")
            .get()
            .addOnSuccessListener { statusSnapshot ->
                if (statusSnapshot.getValue<BloodRequestStatus>() == BloodRequestStatus.CONFIRMED) {
                    childUpdates["/bloodRequests/$requestId/status"] =
                        BloodRequestStatus.PENDING
                }
                Firebase
                    .database
                    .reference
                    .updateChildren(childUpdates)
                    .addOnSuccessListener {
                        onRequestCancelCallback.onCancelSuccess()
                    }
                    .addOnFailureListener { e ->
                        onRequestCancelCallback.onCancelFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onRequestCancelCallback.onCancelFailure(e)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                closeFragment()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun closeFragment() {
        if(supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    interface UserPendingRequestProvider {
        fun onCheck(hasPendingRequest: Boolean, pendingRequestId: String?)
    }

    interface UserPendingRequestCancellationProvider {
        fun onCancelSuccess()
        fun onCancelFailure(exception: Exception)
    }
}