package com.aniket.connect4blood.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aniket.connect4blood.R
import com.aniket.connect4blood.activities.DonorDashboard
import com.aniket.connect4blood.models.Complaint
import com.aniket.connect4blood.models.ComplaintStatus
import com.aniket.connect4blood.models.User
import com.aniket.connect4blood.utils.AppVals
import com.aniket.connect4blood.utils.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DonorSettingsRaiseComplaint: Fragment() {

    private lateinit var etComplaintMobile: TextInputEditText
    private lateinit var etComplaintEmail: TextInputEditText
    private lateinit var etComplaintSubject: TextInputEditText
    private lateinit var etComplaintMsg: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private var progressDialog: AlertDialog? = null
    private var currentUser: User? = null

    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_donor_settings_raise_complaint, container, false)
        currentUser = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER, User::class.java)
        } else {
            arguments?.getSerializable(AppVals.ARG_KEY_CURRENT_USER) as User?
        }

        activity?.let {
            Utils
                .getInstance()
                .enableBackButton((it as DonorDashboard).supportActionBar, "Raise Complaint")
            progressDialog = Utils
                .getInstance()
                .initProgressDialog(
                    it as Context,
                    AppVals.CREATE_SUPPORT_TICKET
                )
        }

        etComplaintMobile = view.findViewById(R.id.etComplaintMobile)
        etComplaintEmail = view.findViewById(R.id.etComplaintEmail)
        etComplaintSubject = view.findViewById(R.id.etComplaintSubject)
        etComplaintMsg = view.findViewById(R.id.etComplaintMsg)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        mAuth = FirebaseAuth.getInstance()

        if(currentUser != null && mAuth.currentUser != null) {
            etComplaintMobile.setText(currentUser!!.phoneNo)
            etComplaintEmail.setText(currentUser!!.email)
            btnSubmit.setOnClickListener(onGenerateSupportTicket())
        }

        return view
    }

    private fun onGenerateSupportTicket(): View.OnClickListener {
        return View.OnClickListener {
            if(!validInputs()) return@OnClickListener
            progressDialog?.show()
            val currentDateTime = LocalDateTime.now()
            val complaintId = Utils.getInstance().generateComplaintId(currentUser!!.phoneNo!!, currentDateTime)
            val complaint = Complaint(
                currentUser!!.email!!,
                currentUser!!.phoneNo!!,
                etComplaintSubject.text.toString(),
                etComplaintMsg.text.toString(),
                ComplaintStatus.PENDING,
                currentDateTime.format(DateTimeFormatter.ofPattern(AppVals.APP_DEFAULT_DATETIME_FORMAT))
            )
            Firebase
                .database
                .getReference("/complaints/${mAuth.currentUser!!.uid}/$complaintId")
                .setValue(complaint)
                .addOnSuccessListener {
                    progressDialog?.dismiss()
                    etComplaintSubject.text?.clear()
                    etComplaintMsg.text?.clear()
                    activity?.let { it1 ->
                        Utils
                            .getInstance()
                            .showToast(
                                it1 as Context,
                                AppVals.SUCCESS_SUPPORT_TICKET_GENERATED
                            )
                        (it1 as DonorDashboard).closeFragment()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog?.dismiss()
                    activity?.let {
                        Utils
                            .getInstance()
                            .showToast(
                                it as Context,
                                e.message.toString()
                            )
                    }
                }
        }
    }

    private fun validInputs(): Boolean {
        if(etComplaintSubject.text.isNullOrEmpty() || etComplaintMsg.text.isNullOrEmpty()) {
            Utils
                .getInstance()
                .showToast(
                    activity as Context,
                    AppVals.ERROR_FIELDS_EMPTY
                )
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils
            .getInstance()
            .disableBackButton((activity as DonorDashboard).supportActionBar)
    }

    companion object {
        fun newInstance(currentUser: User?): DonorSettingsRaiseComplaint {
            val fragment = DonorSettingsRaiseComplaint()
            val args = Bundle()
            args.putSerializable(AppVals.ARG_KEY_CURRENT_USER, currentUser)
            fragment.arguments = args
            return fragment
        }
    }
}