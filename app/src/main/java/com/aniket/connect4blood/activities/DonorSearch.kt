package com.aniket.connect4blood.activities

import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.BloodGroupType
import com.aniket.connect4blood.models.BloodRequest
import com.aniket.connect4blood.models.BloodRequestStatus
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DonorSearch : AppCompatActivity() {

    private lateinit var etDonorSearchFullName: TextInputEditText
    private lateinit var acDonorSearchBloodGroup: AutoCompleteTextView
    private lateinit var etDonorSearchBloodUnits: TextInputEditText
    private lateinit var etDonorSearchRadius: TextInputEditText
    private lateinit var etDonorSearchHospitalName: TextInputEditText
    private lateinit var etDonorSearchHospitalAddress: TextInputEditText
    private lateinit var progressDialog: AlertDialog
    private lateinit var cbTerms: CheckBox
    private lateinit var btnDonorSearch: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDB: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etDonorSearchFullName = findViewById(R.id.etDonorSearchFullName)
        acDonorSearchBloodGroup = findViewById(R.id.acDonorSearchBloodGroup)
        etDonorSearchBloodUnits = findViewById(R.id.etDonorSearchBloodUnits)
        etDonorSearchRadius = findViewById(R.id.etDonorSearchRadius)
        etDonorSearchHospitalName = findViewById(R.id.etDonorSearchHospitalName)
        etDonorSearchHospitalAddress = findViewById(R.id.etDonorSearchHospitalAddress)
        cbTerms = findViewById(R.id.cbTerms)
        btnDonorSearch = findViewById(R.id.btnDonorSearch)
        progressDialog = Utils.getInstance().initProgressDialog(this, "Please wait...")
        mAuth = FirebaseAuth.getInstance()
        mDB = Firebase.database.reference

        acDonorSearchBloodGroup.setAdapter(
            ArrayAdapter(
                this, R.layout.dropdown_menu_popup_item, arrayListOf(
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

        btnDonorSearch.setOnClickListener(onDonorSearch())

        //Check if we have the user current location.
        if (!intent.hasExtra("myCurrentLocation")) {
            Utils
                .getInstance()
                .showToast(this@DonorSearch, "Error! Can't fetch your location")
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onDonorSearch(): View.OnClickListener {
        return View.OnClickListener {
            //Check if user has the network connectivity
            if (!Utils.getInstance().checkConnectivity(this)) return@OnClickListener
            //Basic input validation
            if (!cbTerms.isChecked) {
                Utils.getInstance()
                    .showToast(this, "Error! Please accept the terms before proceeding")
                return@OnClickListener
            }
            if (etDonorSearchFullName.text.isNullOrEmpty() || acDonorSearchBloodGroup.text.isNullOrEmpty() || etDonorSearchBloodUnits.text.isNullOrEmpty() || etDonorSearchHospitalName.text.isNullOrEmpty() || etDonorSearchHospitalAddress.text.isNullOrEmpty()) {
                Utils.getInstance()
                    .showToast(this, "Error! Please fill in all the required fields.")
                return@OnClickListener
            }
            if (etDonorSearchBloodUnits.text.toString().toInt() > 10) {
                Utils.getInstance()
                    .showToast(this, "Error! You can request for maximum 10 units of blood")
                return@OnClickListener
            }
            if (!etDonorSearchRadius.text.isNullOrEmpty() && etDonorSearchRadius.text.toString()
                    .toInt() > 15
            ) {
                Utils.getInstance()
                    .showToast(this, "Error! Search radius can be up to maximum 15km")
                return@OnClickListener
            }
            createBloodRequest()

        }
    }

    @Suppress("DEPRECATION")
    private fun createBloodRequest() {
        progressDialog.show()
        val location = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("myCurrentLocation", Location::class.java)
        } else {
            intent.getParcelableExtra("myCurrentLocation") as Location?
        }
        val locationMap = hashMapOf(
            "lat" to location?.latitude,
            "lng" to location?.longitude
        )
        val searchRadius = if(!etDonorSearchRadius.text.isNullOrEmpty()) {
            etDonorSearchRadius.text.toString().toInt()
        } else {
            AppVals.APP_DEFAULT_MAX_SEARCH_DISTANCE
        }
        val newRequest = BloodRequest(
            requesterId = mAuth.currentUser?.uid,
            requestDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(AppVals.APP_DEFAULT_DATETIME_FORMAT)),
            recipientName = etDonorSearchFullName.text.toString(),
            bloodGroup = acDonorSearchBloodGroup.text.toString(),
            unitsRequired = etDonorSearchBloodUnits.text.toString().toInt(),
            hospitalName = etDonorSearchHospitalName.text.toString(),
            hospitalAddress = etDonorSearchHospitalAddress.text.toString(),
            location = locationMap,
            searchRadius = searchRadius,
            status = BloodRequestStatus.PENDING
        )
        val key = mDB.child("bloodRequests").push().key
        if (key == null) {
            Utils.getInstance().showToast(
                this, "Error! Something went wrong, please try again later."
            )
            return
        }
        val requestId = "C4B$key"
        val childUpdates = hashMapOf(
            "/bloodRequests/$requestId" to newRequest.toMap(),
            "/users/${mAuth.currentUser?.uid}/bloodRequestHistory/$requestId" to newRequest.requestDateTime.toString()
        )
        mDB.updateChildren(childUpdates).addOnSuccessListener {
            progressDialog.dismiss()
            val requestStatusIntent = Intent(this@DonorSearch, DonorSearchStatus::class.java)
            requestStatusIntent.putExtra("requestId", requestId)
            startActivity(requestStatusIntent)
            finish()
        }.addOnFailureListener {
            progressDialog.dismiss()
            Utils.getInstance().showToast(
                this, "Error! Something went wrong, Please try again later"
            )
        }
    }
}