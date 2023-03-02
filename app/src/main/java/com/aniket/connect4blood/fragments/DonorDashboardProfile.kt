package com.aniket.connect4blood.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.Utils
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DonorDashboardProfile : Fragment() {

    private lateinit var imgDonorProfilePic: ImageView
    private lateinit var progressDonorProfilePic: ProgressBar
    private lateinit var txtDonorProfilePicChange: TextView
    private lateinit var etDonorProfileMobileNo: TextInputEditText
    private lateinit var etDonorProfileEmail: TextInputEditText
    private lateinit var etDonorProfileUserName: TextInputEditText
    private lateinit var acDonorProfileGender: MaterialAutoCompleteTextView
    private lateinit var etDonorProfileDOB: TextInputEditText
    private lateinit var etDonorProfileBloodGroup: TextInputEditText
    private lateinit var btnDonorProfileEdit: MaterialButton
    private lateinit var mProfileDataEventListener: ValueEventListener
    private var progressDialog: AlertDialog? = null
    private var mBirthDate: LocalDate? = null
    private var mUser: User? = null
    private val mCurrentUserId = Firebase.auth.currentUser?.uid
    private val mCurrentUserDBRef = Firebase.database.getReference("/users/$mCurrentUserId")
    private val mCurrentUserStorageRef = Firebase.storage.getReference("/users/$mCurrentUserId")
    private val dateFormat = "dd/MM/yyyy"
    private val mSelectProfilePicResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { filePath: Uri? ->
            onProfilePicSelected(filePath)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.layout_donor_dashboard_profile, container, false)

        imgDonorProfilePic = view.findViewById(R.id.imgDonorProfilePic)
        progressDonorProfilePic = view.findViewById(R.id.progressDonorProfilePic)
        txtDonorProfilePicChange = view.findViewById(R.id.txtDonorProfilePicChange)
        etDonorProfileMobileNo = view.findViewById(R.id.etDonorProfileMobileNo)
        etDonorProfileEmail = view.findViewById(R.id.etDonorProfileEmail)
        etDonorProfileUserName = view.findViewById(R.id.etDonorProfileUserName)
        acDonorProfileGender = view.findViewById(R.id.acDonorProfileGender)
        etDonorProfileDOB = view.findViewById(R.id.etDonorProfileDOB)
        etDonorProfileBloodGroup = view.findViewById(R.id.etDonorProfileBloodGroup)
        btnDonorProfileEdit = view.findViewById(R.id.btnDonorProfileEdit)
        mProfileDataEventListener = initProfileDataListener()

        activity?.let { activity ->
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    activity as Context,
                    "Please wait..."
                )
        }

        mCurrentUserDBRef.addValueEventListener(mProfileDataEventListener)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCurrentUserDBRef.removeEventListener(mProfileDataEventListener)
    }

    private fun initProfileDataListener(): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mUser = snapshot.getValue<User>()
                if (initProfileView()) {
                    txtDonorProfilePicChange.setOnClickListener(openImageSelector())
                    btnDonorProfileEdit.setOnClickListener(editProfile())
                } else {
                    //Todo: Display user profile error page
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //TODO("Not yet implemented")
            }

        }
    }

    private fun openImageSelector(): View.OnClickListener {
        return View.OnClickListener {
            mSelectProfilePicResult.launch("image/*")
        }
    }

    private fun onProfilePicSelected(filePath: Uri?) {
        filePath?.let {
            progressDonorProfilePic.visibility = View.VISIBLE
            mCurrentUserStorageRef
                .child("/profile-pic.jpg")
                .putFile(it)
                .addOnSuccessListener { task ->
                    task
                        ?.metadata
                        ?.reference
                        ?.downloadUrl
                        ?.addOnCompleteListener { uriTask ->
                            if (uriTask.isSuccessful) {
                                val downloadUri = uriTask.result.toString()
                                mCurrentUserDBRef
                                    .child("image")
                                    .setValue(downloadUri)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            updateProfileInRequestsDB()
                                        } else {
                                            progressDonorProfilePic.visibility = View.GONE
                                            activity?.let { activity ->
                                                Utils
                                                    .getInstance()
                                                    .showToast(
                                                        activity as Context,
                                                        "Error! Failed to upload the user profile picture."
                                                    )
                                            }
                                        }
                                    }
                            } else {
                                progressDonorProfilePic.visibility = View.GONE
                                activity?.let { activity ->
                                    Utils
                                        .getInstance()
                                        .showToast(
                                            activity as Context,
                                            "Error! Failed to upload the user profile picture."
                                        )
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    progressDonorProfilePic.visibility = View.GONE
                    activity?.let { activity ->
                        Utils
                            .getInstance()
                            .showToast(
                                activity as Context,
                                "Error! Failed to upload the user profile picture."
                            )
                    }
                }
        }
    }

    private fun editProfile(): View.OnClickListener {
        return View.OnClickListener { view ->
            //Enable the fields
            onEditChangeViewProps(true)

            //Set the field props and event listeners
            if (acDonorProfileGender.adapter == null || acDonorProfileGender.adapter.count < 2) {
                genderDropdownViewInit()
            }
            if (!etDonorProfileDOB.hasOnClickListeners()) {
                etDonorProfileDOB.setOnClickListener(datePickerListener())
            }

            //Change click listener of the button to enable the profile update functionality
            view.setOnClickListener(updateProfile())
        }
    }

    private fun updateProfile(): View.OnClickListener {
        return View.OnClickListener {
            var userUpdated = false
            if (!etDonorProfileUserName.text.isNullOrEmpty() && etDonorProfileUserName.text.toString() != mUser?.name) {
                mUser?.name = etDonorProfileUserName.text.toString()
                userUpdated = true
            }
            if (!acDonorProfileGender.text.isNullOrEmpty() && acDonorProfileGender.text.toString() != mUser?.gender) {
                mUser?.gender = acDonorProfileGender.text.toString()
                userUpdated = true
            }
            if (!etDonorProfileDOB.text.isNullOrEmpty() && etDonorProfileDOB.text.toString() != mUser?.dateOfBirth && Utils.getInstance()
                    .calcAge(etDonorProfileDOB.text.toString()) >= 18
            ) {
                mUser?.dateOfBirth = etDonorProfileDOB.text.toString()
                userUpdated = true
            }

            if (userUpdated) {
                if (progressDialog?.isShowing == false) progressDialog?.show()
                mCurrentUserDBRef
                    .setValue(mUser)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updateProfileInRequestsDB()
                        } else {
                            if (progressDialog?.isShowing == true) progressDialog?.dismiss()
                            activity?.let {
                                Utils
                                    .getInstance()
                                    .showToast(
                                        it as Context,
                                        "Error! Something went wrong while updating your profile."
                                    )
                            }
                        }
                        onEditChangeViewProps(false)
                    }
            } else {
                onEditChangeViewProps(false)
            }

        }
    }

    private fun updateProgressViewState() {
        if (progressDonorProfilePic.isVisible) progressDonorProfilePic.visibility = View.GONE
        if (progressDialog?.isShowing == true) progressDialog?.dismiss()
    }

    private fun updateProfileInRequestsDB() {
        var isUpdateCompleted = false
        var count = 0
        mCurrentUserDBRef
            .child("/bloodDonationHistory")
            .get()
            .addOnSuccessListener {
                if (it.hasChildren()) {
                    it.children.forEach(action = { child ->
                        val requestId = child.key
                        Firebase
                            .database
                            .getReference("/bloodRequests/$requestId/donors/$mCurrentUserId")
                            .setValue(mUser?.toMap())
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    count++
                                    if (count.toLong() == it.childrenCount) {
                                        isUpdateCompleted = true
                                    }

                                    if (isUpdateCompleted) {
                                        updateProgressViewState()
                                    }
                                } else {
                                    updateProgressViewState()
                                    //Show retry snackbar
                                }
                            }

                    })
                } else {
                    updateProgressViewState()
                }
            }
            .addOnFailureListener {
                updateProgressViewState()
                //Show retry snackbar
            }
    }

    private fun onEditChangeViewProps(isProfileUpdateInProgress: Boolean) {
        if (isProfileUpdateInProgress) {
            btnDonorProfileEdit.text = getText(R.string.submit)
            activity?.let {
                btnDonorProfileEdit.setBackgroundColor(
                    ContextCompat.getColor(
                        it as Context,
                        R.color.green_500
                    )
                )
            }
        } else {
            btnDonorProfileEdit.text = getString(R.string.edit_profile)
            activity?.let {
                btnDonorProfileEdit.setBackgroundColor(
                    ContextCompat.getColor(it as Context, R.color.blue_700)
                )
            }
            btnDonorProfileEdit.setOnClickListener(editProfile())
        }
        changeViewInputState(
            etDonorProfileUserName,
            isProfileUpdateInProgress,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        )
        changeViewInputState(acDonorProfileGender, isProfileUpdateInProgress)
        etDonorProfileDOB.isEnabled = isProfileUpdateInProgress

    }

    private fun datePickerListener(): View.OnClickListener {
        return View.OnClickListener {
            val dob = if (mBirthDate != null) {
                mBirthDate
            } else {
                LocalDate.parse(mUser?.dateOfBirth, DateTimeFormatter.ofPattern(dateFormat))
            }
            DatePickerDialog(
                activity as Context,
                { _: DatePicker?, year: Int, month: Int, day: Int ->
                    setOnDateSelection(year, month, day)
                },
                dob!!.year,
                dob.monthValue - 1,
                dob.dayOfMonth
            ).show()
        }
    }

    private fun genderDropdownViewInit() {
        acDonorProfileGender.setAdapter(
            ArrayAdapter(
                activity as Context,
                R.layout.dropdown_menu_popup_item,
                arrayListOf(
                    "Male", "Female"
                )
            )
        )
    }

    private fun initProfileView(): Boolean {
        if (mUser != null) {
            etDonorProfileMobileNo.setText(mUser!!.phoneNo)
            etDonorProfileEmail.setText(mUser!!.email)
            etDonorProfileUserName.setText(mUser!!.name)
            acDonorProfileGender.setText(mUser!!.gender)
            etDonorProfileDOB.setText(mUser!!.dateOfBirth)
            etDonorProfileBloodGroup.setText(mUser!!.bloodGroup)
            if (mUser!!.image != null && activity != null) {
                Glide
                    .with(activity as Context)
                    .load(Uri.parse(mUser?.image))
                    .centerCrop()
                    .placeholder(R.drawable.user_icon)
                    .into(imgDonorProfilePic)
            }
            return true
        }
        return false
    }

    private fun changeViewInputState(view: View, isEnabled: Boolean, inputType: Int? = null) {
        if (isEnabled) {
            inputType?.let { it -> (view as EditText).inputType = it }
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.isEnabled = true

        } else {
            (view as EditText).inputType = InputType.TYPE_NULL
            view.isFocusable = false
            view.isFocusableInTouchMode = false
            view.isEnabled = false
        }
    }

    private fun setOnDateSelection(year: Int, month: Int, day: Int) {
        mBirthDate = LocalDate.of(year, month + 1, day)
        etDonorProfileDOB.setText(mBirthDate!!.format(DateTimeFormatter.ofPattern(dateFormat)))
    }

    companion object {
        fun newInstance(): DonorDashboardProfile {
            val fragment = DonorDashboardProfile()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}