package com.aniket.connect4blood.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.fragments.DonorSearchStatusCancelled
import com.aniket.connect4blood.fragments.DonorSearchStatusCompleted
import com.aniket.connect4blood.fragments.DonorSearchStatusConfirmed
import com.aniket.connect4blood.fragments.DonorSearchStatusPending
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.utils.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class DonorSearchStatus : AppCompatActivity() {

    private lateinit var progressDialog: AlertDialog
    private lateinit var mRequestStatusEventListener: ValueEventListener
    private lateinit var mBloodRequestStatusDBRef: DatabaseReference
    private lateinit var requestId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_search_status)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(!intent.hasExtra("requestId")) {
            Utils
                .getInstance()
                .showToast(this, "Error! Can't locate the request id.")
            finish()
        }

        progressDialog = Utils
            .getInstance()
            .initProgressDialog(
                this@DonorSearchStatus,
                "Please wait..."
            )
        requestId = intent.getStringExtra("requestId").toString()

        mBloodRequestStatusDBRef = Firebase
            .database
            .getReference("/bloodRequests/$requestId/status")
        mRequestStatusEventListener = initRequestStatusEventListener()


        progressDialog.show()
        mBloodRequestStatusDBRef.addValueEventListener(mRequestStatusEventListener)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mBloodRequestStatusDBRef.removeEventListener(mRequestStatusEventListener)
    }

    private fun initRequestStatusEventListener(): ValueEventListener {
        return object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressDialog.dismiss()
                when(snapshot.getValue<BloodRequestStatus>()) {
                    BloodRequestStatus.PENDING -> {
                        createFragment(
                            R.id.donorSearchStatusContainer,
                            DonorSearchStatusPending.newInstance(requestId)
                        )
                    }
                    BloodRequestStatus.CONFIRMED -> {
                        createFragment(
                            R.id.donorSearchStatusContainer,
                            DonorSearchStatusConfirmed.newInstance(requestId)
                        )
                    }
                    BloodRequestStatus.COMPLETED -> {
                        createFragment(
                            R.id.donorSearchStatusContainer,
                            DonorSearchStatusCompleted.newInstance(requestId)
                        )
                    }
                    BloodRequestStatus.CANCELLED -> {
                        createFragment(
                            R.id.donorSearchStatusContainer,
                            DonorSearchStatusCancelled.newInstance(requestId)
                        )
                    }
                    else -> {

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                Utils
                    .getInstance()
                    .showToast(
                        this@DonorSearchStatus,
                        "Error! Something went wrong, please try again."
                    )
            }

        }
    }

    private fun createFragment(frameId: Int, fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(frameId, fragment)
            .commit()
    }
}